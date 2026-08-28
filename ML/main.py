#!/usr/bin/env python3
"""
resume_job_matcher.py

Parses a resume PDF, reads a jobs CSV (schema is flexible — any set of
columns is accepted), and uses a local Ollama LLM to score/rank how well
each job fits the resume.

This version is written to never hard-crash on bad input:
  - Messy/unquoted CSVs are auto-repaired instead of raising a parser error.
  - An unavailable/misspelled model name is auto-resolved to the closest
    available model on your Ollama install (with a warning), instead of
    exiting.
  - A single job failing to score (timeout, weird model output, etc.)
    is recorded with a 0 score and a reason, and the run continues —
    one bad row can't kill the whole ranking.
  - The only unrecoverable case left is "Ollama isn't running at all",
    because there is no LLM to fall back to; that prints one clear,
    actionable message.

Usage:
    python resume_job_matcher.py --resume resume.pdf --jobs jobs.csv

Optional:
    --model gemma2            # Ollama model tag to use (default: gemma2)
    --host http://localhost:11434
    --top-n 10                # how many results to print (default: all)
    --output ranked_jobs.csv  # save full ranked results to a CSV
    --title-col "job title"   # force which column is treated as the title
    --workers 1               # parallel requests to Ollama

Requirements:
    pip install pdfplumber pandas requests
    (optional, improves PDF coverage: pip install pypdf)
"""

import argparse
import csv
import json
import re
import sys
import difflib
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path

import pandas as pd
import requests

try:
    import pdfplumber
except ImportError:
    pdfplumber = None

try:
    from pypdf import PdfReader
except ImportError:
    try:
        from PyPDF2 import PdfReader
    except ImportError:
        PdfReader = None


# --------------------------------------------------------------------------- #
# Resume parsing
# --------------------------------------------------------------------------- #

def extract_resume_text(pdf_path: str) -> str:
    """Extract plain text from a resume PDF, trying multiple backends."""
    path = Path(pdf_path)
    if not path.exists():
        sys.exit(f"Resume file not found: {pdf_path}")

    text = ""

    if pdfplumber is not None:
        try:
            parts = []
            with pdfplumber.open(path) as pdf:
                for page in pdf.pages:
                    parts.append(page.extract_text() or "")
            text = "\n".join(parts).strip()
        except Exception as e:
            print(f"Warning: pdfplumber failed to read the PDF ({e}); trying a fallback reader...")

    if not text and PdfReader is not None:
        try:
            reader = PdfReader(str(path))
            parts = [(page.extract_text() or "") for page in reader.pages]
            text = "\n".join(parts).strip()
        except Exception as e:
            print(f"Warning: fallback PDF reader also failed ({e}).")

    if not text:
        if pdfplumber is None and PdfReader is None:
            sys.exit(
                "No PDF reading library is installed. Install one with:\n"
                "    pip install pdfplumber\n"
                "or:\n"
                "    pip install pypdf"
            )
        sys.exit(
            "Could not extract any text from the PDF. It's likely a scanned "
            "image rather than a text PDF — OCR it first (e.g. with "
            "`ocrmypdf`) and re-run."
        )
    return text


# --------------------------------------------------------------------------- #
# Jobs CSV loading — deliberately schema-agnostic and crash-resistant
# --------------------------------------------------------------------------- #

def robust_read_csv(csv_path: str) -> pd.DataFrame:
    """
    Read a CSV without ever raising a hard parser error.

    Real-world job CSVs often have free-text fields (descriptions) with
    unquoted commas. Pandas' C parser rejects those outright. Here we
    tokenize with Python's csv module (which never throws on ragged rows)
    and heuristically repair rows that have too many or too few fields,
    warning about each repair so you can fix the source file later if
    you want cleaner data.
    """
    path = Path(csv_path)
    with open(path, newline="", encoding="utf-8-sig") as f:
        rows = list(csv.reader(f))

    if not rows:
        sys.exit(f"Jobs CSV '{csv_path}' is empty.")

    header = [h.strip() for h in rows[0]]
    ncols = len(header)
    fixed_rows = []
    repairs = 0

    for line_num, row in enumerate(rows[1:], start=2):
        if not any(cell.strip() for cell in row):
            continue  # skip fully blank lines
        if len(row) == ncols:
            fixed_rows.append(row)
        elif len(row) > ncols:
            # Most likely cause: an unquoted comma inside one free-text field.
            # Heuristic: assume the overflow happened in the 2nd column
            # (commonly "description"), and merge the extra pieces back in.
            excess = len(row) - ncols
            merge_end = 1 + excess  # inclusive index of last field to merge
            if merge_end < len(row):
                merged = ",".join(row[1:merge_end + 1])
                new_row = [row[0], merged] + row[merge_end + 1:]
            else:
                new_row = row[:ncols - 1] + [",".join(row[ncols - 1:])]
            if len(new_row) != ncols:
                new_row = (new_row + [""] * ncols)[:ncols]
            fixed_rows.append(new_row)
            repairs += 1
            print(f"Warning: line {line_num} had an unquoted comma — auto-repaired.")
        else:
            # Fewer fields than the header — pad with blanks rather than fail.
            new_row = row + [""] * (ncols - len(row))
            fixed_rows.append(new_row)
            repairs += 1
            print(f"Warning: line {line_num} had missing fields — padded with blanks.")

    if repairs:
        print(
            f"({repairs} row(s) auto-repaired. For cleanest results, wrap any "
            f'field containing a comma in double quotes, e.g. "like, this".)'
        )

    df = pd.DataFrame(fixed_rows, columns=header).fillna("")
    return df


def load_jobs(csv_path: str, title_col: str | None = None) -> list[dict]:
    """
    Load the jobs CSV into a list of dicts, one per job.

    The CSV schema is NOT hardcoded. Whatever columns exist (job title,
    job description, qualifications, location, salary, seniority, remote,
    department, whatever gets added later) are all read in automatically
    and passed through to the LLM. This means you can add/remove/rename
    columns in the CSV later without touching this code — the only thing
    that matters is picking a reasonable "title" column for display
    purposes, which is auto-detected (or can be forced with --title-col).
    """
    path = Path(csv_path)
    if not path.exists():
        sys.exit(f"Jobs CSV not found: {csv_path}")

    df = robust_read_csv(csv_path)
    if df.empty:
        sys.exit("Jobs CSV has no data rows.")

    columns = list(df.columns)

    resolved_title_col = None
    if title_col and title_col in columns:
        resolved_title_col = title_col
    else:
        candidates = ["job title", "title", "job_title", "position", "role"]
        lower_map = {c.lower().strip(): c for c in columns}
        for cand in candidates:
            if cand in lower_map:
                resolved_title_col = lower_map[cand]
                break
        if resolved_title_col is None:
            resolved_title_col = columns[0]

    jobs = []
    for i, row in df.iterrows():
        record = {col: row[col] for col in columns}
        title = str(record.get(resolved_title_col, "")).strip() or f"Job #{i}"
        jobs.append({"id": i, "title": title, "fields": record})
    return jobs


def job_to_text_block(job: dict) -> str:
    """Turn a job's arbitrary columns into a labeled text block for the prompt."""
    lines = []
    for key, value in job["fields"].items():
        value = str(value).strip()
        if value:
            lines.append(f"{key.strip()}: {value}")
    return "\n".join(lines)


# --------------------------------------------------------------------------- #
# LLM scoring via Ollama
# --------------------------------------------------------------------------- #

PROMPT_TEMPLATE = """You are an expert technical recruiter. Compare the RESUME below \
against the JOB LISTING and evaluate how good a fit the candidate is for this job.

Respond with ONLY a JSON object, no other text, no markdown fences, in exactly \
this shape:
{{"score": <integer 0-100>, "reason": "<one or two sentence justification>"}}

Score meaning:
- 90-100: excellent fit, candidate meets/exceeds nearly all qualifications
- 70-89: strong fit, meets most core qualifications
- 50-69: partial fit, meets some qualifications but has notable gaps
- 20-49: weak fit, few relevant qualifications
- 0-19: not a fit at all

RESUME:
\"\"\"
{resume_text}
\"\"\"

JOB LISTING:
\"\"\"
{job_text}
\"\"\"

JSON response:"""


class OllamaError(Exception):
    """Raised for any problem talking to Ollama; always caught, never crashes the run."""


def get_available_models(host: str) -> list[str]:
    """Return the list of model tags Ollama has pulled, or [] if it can't be reached."""
    try:
        resp = requests.get(f"{host.rstrip('/')}/api/tags", timeout=10)
        resp.raise_for_status()
        data = resp.json()
        return [m.get("name", "") for m in data.get("models", []) if m.get("name")]
    except requests.exceptions.RequestException:
        return []


def resolve_model(requested: str, host: str) -> str:
    """
    Make sure we use a model that actually exists on this Ollama install.
    Falls back gracefully instead of failing later mid-run:
      1. exact match -> use it
      2. exact match ignoring ":latest" tag suffix -> use it
      3. closest name match (e.g. "gemma4" -> "gemma2:latest") -> use it, warn
      4. no models installed at all / can't reach Ollama -> keep requested,
         the very first API call will surface a clear one-time error.
    """
    available = get_available_models(host)
    if not available:
        return requested  # can't check right now; let call_ollama report if it fails

    if requested in available:
        return requested

    bare = {m.split(":")[0]: m for m in available}
    if requested in bare:
        return bare[requested]

    close = difflib.get_close_matches(requested, available, n=1, cutoff=0.3) or \
        difflib.get_close_matches(requested, list(bare.keys()), n=1, cutoff=0.3)
    if close:
        chosen = bare.get(close[0], close[0])
        print(
            f"Note: model '{requested}' is not installed. Using the closest "
            f"available model instead: '{chosen}'.\n"
            f"(Installed models: {', '.join(available)}. "
            f"Run `ollama pull {requested}` if you specifically want that one.)"
        )
        return chosen

    # No good match — just use whatever the first installed model is.
    fallback = available[0]
    print(
        f"Note: model '{requested}' is not installed and no close match was "
        f"found. Falling back to '{fallback}'.\n"
        f"(Installed models: {', '.join(available)}.)"
    )
    return fallback


def call_ollama(prompt: str, model: str, host: str, timeout: int = 120) -> str:
    url = f"{host.rstrip('/')}/api/generate"
    payload = {"model": model, "prompt": prompt, "stream": False}
    try:
        resp = requests.post(url, json=payload, timeout=timeout)
        resp.raise_for_status()
    except requests.exceptions.ConnectionError as e:
        raise OllamaError(
            f"Could not connect to Ollama at {host}. Is it running? (`ollama serve`)"
        ) from e
    except requests.exceptions.Timeout as e:
        raise OllamaError(f"Request to Ollama timed out after {timeout}s.") from e
    except requests.exceptions.HTTPError as e:
        raise OllamaError(f"Ollama returned an error for model '{model}': {e}") from e

    try:
        data = resp.json()
    except ValueError as e:
        raise OllamaError("Ollama returned a non-JSON response.") from e
    return data.get("response", "")


def parse_score_response(raw: str) -> dict:
    """
    Pull a {"score": int, "reason": str} object out of the model's raw
    text, tolerating stray markdown fences or extra prose around the JSON.
    Never raises — worst case it returns a 0 score with the raw text as reason.
    """
    text = (raw or "").strip()
    text = re.sub(r"^```(?:json)?", "", text, flags=re.IGNORECASE).strip()
    text = re.sub(r"```$", "", text).strip()

    match = re.search(r"\{.*\}", text, flags=re.DOTALL)
    if match:
        text = match.group(0)

    try:
        obj = json.loads(text)
        score = int(obj.get("score", 0))
        score = max(0, min(100, score))
        reason = str(obj.get("reason", "")).strip()
        return {"score": score, "reason": reason}
    except Exception:
        num_match = re.search(r"\b(\d{1,3})\b", raw or "")
        score = max(0, min(100, int(num_match.group(1)))) if num_match else 0
        return {"score": score, "reason": (raw or "").strip()[:200] or "(unparseable model response)"}


def score_job(resume_text: str, job: dict, model: str, host: str) -> dict:
    """Score one job. Never raises — any failure becomes a 0 score with an explanation."""
    try:
        job_text = job_to_text_block(job)
        prompt = PROMPT_TEMPLATE.format(resume_text=resume_text, job_text=job_text)
        raw = call_ollama(prompt, model=model, host=host)
        result = parse_score_response(raw)
    except OllamaError as e:
        print(f"  Warning: could not score '{job['title']}' ({e}). Recording as 0.")
        result = {"score": 0, "reason": f"Scoring failed: {e}"}
    except Exception as e:
        print(f"  Warning: unexpected error scoring '{job['title']}' ({e}). Recording as 0.")
        result = {"score": 0, "reason": f"Unexpected error: {e}"}

    return {
        "id": job["id"],
        "title": job["title"],
        "score": result["score"],
        "reason": result["reason"],
        "fields": job["fields"],
    }


# --------------------------------------------------------------------------- #
# Main
# --------------------------------------------------------------------------- #

def main():
    parser = argparse.ArgumentParser(
        description="Rank jobs in a CSV against a resume PDF using a local Ollama LLM."
    )
    parser.add_argument("--resume", required=True, help="Path to resume PDF")
    parser.add_argument("--jobs", required=True, help="Path to jobs CSV")
    parser.add_argument(
        "--model",
        default="gemma2",
        help="Ollama model tag to use (default: gemma2). If not installed, "
        "the closest installed model is used automatically.",
    )
    parser.add_argument("--host", default="http://localhost:11434", help="Ollama host URL")
    parser.add_argument("--top-n", type=int, default=None, help="Only show top N results")
    parser.add_argument("--output", default=None, help="Optional path to save full ranked CSV")
    parser.add_argument(
        "--title-col",
        default=None,
        help="Force which CSV column is used as the job title for display",
    )
    parser.add_argument(
        "--workers",
        type=int,
        default=1,
        help="Number of concurrent requests to Ollama (default: 1, sequential)",
    )
    args = parser.parse_args()

    print(f"Reading resume: {args.resume}")
    resume_text = extract_resume_text(args.resume)

    print(f"Reading jobs CSV: {args.jobs}")
    jobs = load_jobs(args.jobs, title_col=args.title_col)
    print(f"Loaded {len(jobs)} job(s). Columns detected: {list(jobs[0]['fields'].keys())}")

    model = resolve_model(args.model, args.host)

    print(f"Scoring jobs against resume using model '{model}' at {args.host} ...")
    results = []

    if args.workers <= 1:
        for i, job in enumerate(jobs, 1):
            print(f"  [{i}/{len(jobs)}] scoring: {job['title']}")
            results.append(score_job(resume_text, job, model, args.host))
    else:
        with ThreadPoolExecutor(max_workers=args.workers) as executor:
            futures = {
                executor.submit(score_job, resume_text, job, model, args.host): job
                for job in jobs
            }
            for i, future in enumerate(as_completed(futures), 1):
                job = futures[future]
                print(f"  [{i}/{len(jobs)}] scored: {job['title']}")
                results.append(future.result())

    if not results:
        sys.exit("No jobs were scored — nothing to rank.")

    results.sort(key=lambda r: r["score"], reverse=True)
    display_results = results[: args.top_n] if args.top_n else results

    print("\n=== Ranking (best fit first) ===")
    for rank, r in enumerate(display_results, 1):
        print(f"{rank}. {r['title']}  —  score: {r['score']}/100")
        if r["reason"]:
            print(f"   reason: {r['reason']}")

    if args.output:
        out_df = pd.DataFrame(
            [
                {**r["fields"], "match_score": r["score"], "match_reason": r["reason"]}
                for r in results
            ]
        )
        out_df.to_csv(args.output, index=False)
        print(f"\nFull ranked results saved to: {args.output}")


if __name__ == "__main__":
    main()