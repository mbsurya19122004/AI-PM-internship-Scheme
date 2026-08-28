#!/usr/bin/env python3
"""
FastAPI service for resume-job matching using Ollama LLM.

Endpoints:
  POST /analyze-resume    — extract candidate profile from resume text
  POST /match-jobs        — score and rank jobs against a resume
  GET  /health            — health check
"""

import json
import os
import tempfile
from pathlib import Path

import pandas as pd
from fastapi import FastAPI, File, HTTPException, UploadFile
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel

# Import from existing ML code
from main import (
    call_ollama,
    extract_resume_text,
    load_jobs,
    resolve_model,
    score_job,
)

app = FastAPI(title="ML Resume-Job Matching Service", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Config
OLLAMA_HOST = os.getenv("OLLAMA_HOST", "http://localhost:11434")
MODEL = os.getenv("MODEL", "qwen2.5:0.5b")
JOBS_CSV = os.getenv("JOBS_CSV", str(Path(__file__).parent / "jobs_sample.csv"))

# Auto-detect best available model on startup
try:
    import requests as _req
    _resp = _req.get(f"{OLLAMA_HOST}/api/tags", timeout=5)
    _models = [m["name"] for m in _resp.json().get("models", [])]
    if _models:
        # Prefer faster models first, then fallbacks
        for pref in ["qwen2.5:0.5b", "qwen2.5", "gemma2:2b", "gemma2", "gemma:2b"]:
            matches = [m for m in _models if m.startswith(pref)]
            if matches:
                MODEL = matches[0]
                break
        else:
            MODEL = _models[0]
        print(f"Auto-detected model: {MODEL}")
except Exception:
    pass


# ---------------------------------------------------------------------------
# Request / Response models
# ---------------------------------------------------------------------------

class AnalyzeResumeRequest(BaseModel):
    resume_text: str


class JobMatchRequest(BaseModel):
    resume_text: str
    top_n: int = 10


class CandidateProfile(BaseModel):
    name: str
    email: str
    phone: str
    education: str
    skills: list[str]
    experience_level: str
    domains: list[str]
    preferred_roles: list[str]
    summary: str


class JobMatch(BaseModel):
    id: int
    title: str
    score: int
    reason: str
    fields: dict


# ---------------------------------------------------------------------------
# Endpoints
# ---------------------------------------------------------------------------

@app.get("/health")
def health():
    return {"status": "ok", "model": MODEL, "ollama_host": OLLAMA_HOST}


@app.post("/analyze-resume")
def analyze_resume(req: AnalyzeResumeRequest):
    """Use LLM to extract a structured candidate profile from resume text."""
    if not req.resume_text.strip():
        raise HTTPException(status_code=400, detail="resume_text is empty")

    from main import _truncate
    prompt = f"""Extract profile from resume. Reply JSON only:
{{"name":"...","email":"","phone":"","education":"...","skills":["..."],"experience_level":"Student|Fresher|Junior|Mid|Senior","domains":["..."],"preferred_roles":["..."],"summary":"one sentence"}}

Resume:\n{_truncate(req.resume_text)}"""

    try:
        model = resolve_model(MODEL, OLLAMA_HOST)
        raw = call_ollama(prompt, model=model, host=OLLAMA_HOST)
    except Exception as e:
        raise HTTPException(status_code=503, detail=f"LLM error: {e}")

    # Parse the response
    import re
    text = raw.strip()
    text = re.sub(r"^```(?:json)?", "", text, flags=re.IGNORECASE).strip()
    text = re.sub(r"```$", "", text).strip()
    match = re.search(r"\{.*\}", text, flags=re.DOTALL)
    if match:
        text = match.group(0)

    try:
        profile = json.loads(text)
    except json.JSONDecodeError:
        raise HTTPException(status_code=500, detail=f"Could not parse LLM response: {raw[:500]}")

    return {"success": True, "data": profile}


@app.post("/match-jobs")
def match_jobs(req: JobMatchRequest):
    """Score all jobs in the CSV against the given resume text, return ranked list."""
    if not req.resume_text.strip():
        raise HTTPException(status_code=400, detail="resume_text is empty")

    if not Path(JOBS_CSV).exists():
        raise HTTPException(status_code=500, detail=f"Jobs CSV not found: {JOBS_CSV}")

    jobs = load_jobs(JOBS_CSV)
    if not jobs:
        raise HTTPException(status_code=500, detail="No jobs found in CSV")

    model = resolve_model(MODEL, OLLAMA_HOST)

    results = []
    for job in jobs:
        result = score_job(req.resume_text, job, model, OLLAMA_HOST)
        results.append(result)

    results.sort(key=lambda r: r["score"], reverse=True)
    top = results[: req.top_n]

    return {
        "success": True,
        "data": {
            "total_jobs": len(jobs),
            "matches": top,
        },
    }


@app.post("/match-jobs-file")
async def match_jobs_file(
    file: UploadFile = File(...),
    top_n: int = 10,
):
    """Upload a resume PDF, extract text, and match against jobs."""
    if not file.filename.lower().endswith(".pdf"):
        raise HTTPException(status_code=400, detail="Only PDF files are accepted")

    # Save uploaded file temporarily
    with tempfile.NamedTemporaryFile(suffix=".pdf", delete=False) as tmp:
        content = await file.read()
        tmp.write(content)
        tmp_path = tmp.name

    try:
        resume_text = extract_resume_text(tmp_path)
    finally:
        os.unlink(tmp_path)

    if not resume_text.strip():
        raise HTTPException(status_code=400, detail="Could not extract text from PDF")

    # Analyze profile
    from main import _truncate
    profile_prompt = f"""Extract profile from resume. Reply JSON only:
{{"name":"...","education":"...","skills":["..."],"experience_level":"Student|Fresher|Junior|Mid|Senior","domains":["..."],"preferred_roles":["..."],"summary":"one sentence"}}

Resume:\n{_truncate(resume_text)}"""

    profile = None
    try:
        model = resolve_model(MODEL, OLLAMA_HOST)
        raw_profile = call_ollama(profile_prompt, model=model, host=OLLAMA_HOST)
        import re
        text = raw_profile.strip()
        text = re.sub(r"^```(?:json)?", "", text, flags=re.IGNORECASE).strip()
        text = re.sub(r"```$", "", text).strip()
        m = re.search(r"\{.*\}", text, flags=re.DOTALL)
        if m:
            profile = json.loads(m.group(0))
    except Exception:
        pass  # profile is optional

    # Match jobs
    if not Path(JOBS_CSV).exists():
        raise HTTPException(status_code=500, detail=f"Jobs CSV not found: {JOBS_CSV}")

    jobs = load_jobs(JOBS_CSV)
    model = resolve_model(MODEL, OLLAMA_HOST)

    results = []
    for job in jobs:
        result = score_job(resume_text, job, model, OLLAMA_HOST)
        results.append(result)

    results.sort(key=lambda r: r["score"], reverse=True)

    return {
        "success": True,
        "data": {
            "profile": profile,
            "total_jobs": len(jobs),
            "matches": results[:top_n],
        },
    }


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
