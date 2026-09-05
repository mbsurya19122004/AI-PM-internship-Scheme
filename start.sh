#!/usr/bin/env sh

# ============================================================
# WeatherGPT - Universal Startup Script
#
# Compatible with:
#   bash
#   zsh
#   fish  -> fish start.sh
#   sh
#
# Usage:
#   ./start.sh
#   sh start.sh
#   bash start.sh
#   zsh start.sh
#   fish start.sh
#
# Commands:
#   ./start.sh          Start backend + frontend test console
#   ./start.sh backend  Start backend only
#   ./start.sh frontend Start frontend only
#   ./start.sh test     Run backend tests
#   ./start.sh build    Build backend
#   ./start.sh stop     Stop WeatherGPT processes started on ports
# ============================================================

set -eu

PROJECT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
BACKEND_DIR="$PROJECT_DIR/backend"
FRONTEND_DIR="$PROJECT_DIR/frontend"

BACKEND_PORT=8080
FRONTEND_PORT=3000

BACKEND_PID=""
FRONTEND_PID=""

# ------------------------------------------------------------
# Colors
# ------------------------------------------------------------

if [ -t 1 ]; then
    RED="$(printf '\033[0;31m')"
    GREEN="$(printf '\033[0;32m')"
    YELLOW="$(printf '\033[1;33m')"
    BLUE="$(printf '\033[0;34m')"
    NC="$(printf '\033[0m')"
else
    RED=""
    GREEN=""
    YELLOW=""
    BLUE=""
    NC=""
fi

info() {
    printf "%s[INFO]%s %s\n" "$BLUE" "$NC" "$1"
}

success() {
    printf "%s[SUCCESS]%s %s\n" "$GREEN" "$NC" "$1"
}

warn() {
    printf "%s[WARNING]%s %s\n" "$YELLOW" "$NC" "$1"
}

error() {
    printf "%s[ERROR]%s %s\n" "$RED" "$NC" "$1"
}

# ------------------------------------------------------------
# Check command availability
# ------------------------------------------------------------

require_command() {
    if ! command -v "$1" >/dev/null 2>&1; then
        error "Required command not found: $1"
        exit 1
    fi
}

# ------------------------------------------------------------
# Find process using a port
# ------------------------------------------------------------

port_in_use() {
    PORT="$1"

    if command -v lsof >/dev/null 2>&1; then
        lsof -i ":$PORT" >/dev/null 2>&1
        return $?
    fi

    if command -v ss >/dev/null 2>&1; then
        ss -ltn 2>/dev/null | grep -q ":$PORT "
        return $?
    fi

    return 1
}

# ------------------------------------------------------------
# Stop process running on a port
# ------------------------------------------------------------

stop_port() {
    PORT="$1"

    if command -v lsof >/dev/null 2>&1; then
        PIDS="$(lsof -ti ":$PORT" 2>/dev/null || true)"

        if [ -n "$PIDS" ]; then
            warn "Stopping process on port $PORT..."

            for PID in $PIDS; do
                kill "$PID" 2>/dev/null || true
            done

            sleep 1

            PIDS="$(lsof -ti ":$PORT" 2>/dev/null || true)"

            if [ -n "$PIDS" ]; then
                for PID in $PIDS; do
                    kill -9 "$PID" 2>/dev/null || true
                done
            fi

            success "Port $PORT is now free."
        fi
    else
        warn "lsof not available. Cannot automatically stop port $PORT."
    fi
}

# ------------------------------------------------------------
# Cleanup when Ctrl+C is pressed
# ------------------------------------------------------------

cleanup() {
    printf "\n"

    info "Stopping WeatherGPT..."

    if [ -n "$BACKEND_PID" ]; then
        kill "$BACKEND_PID" 2>/dev/null || true
    fi

    if [ -n "$FRONTEND_PID" ]; then
        kill "$FRONTEND_PID" 2>/dev/null || true
    fi

    success "WeatherGPT stopped."
    exit 0
}

trap cleanup INT TERM

# ------------------------------------------------------------
# Backend
# ------------------------------------------------------------

start_backend() {

    require_command java
    require_command mvn

    if [ ! -d "$BACKEND_DIR" ]; then
        error "Backend directory not found:"
        error "$BACKEND_DIR"
        exit 1
    fi

    if [ ! -f "$BACKEND_DIR/pom.xml" ]; then
        error "pom.xml not found in backend directory."
        exit 1
    fi

    if port_in_use "$BACKEND_PORT"; then
        warn "Port $BACKEND_PORT is already in use."
        warn "Assuming WeatherGPT backend may already be running."
        info "Backend URL: http://localhost:$BACKEND_PORT"
        return
    fi

    info "Starting WeatherGPT Backend..."

    (
        cd "$BACKEND_DIR"

        mvn spring-boot:run
    ) &

    BACKEND_PID=$!

    info "Waiting for backend on port $BACKEND_PORT..."

    COUNT=0

    while [ "$COUNT" -lt 60 ]; do

        if port_in_use "$BACKEND_PORT"; then
            success "WeatherGPT Backend started!"
            success "Backend: http://localhost:$BACKEND_PORT"
            return
        fi

        if ! kill -0 "$BACKEND_PID" 2>/dev/null; then
            error "Backend process stopped unexpectedly."
            exit 1
        fi

        sleep 1
        COUNT=$((COUNT + 1))
    done

    error "Backend did not start within 60 seconds."
    exit 1
}

# ------------------------------------------------------------
# Frontend
#
# Current frontend is a static HTML API console.
# No package.json / npm / React / Next.js required.
# ------------------------------------------------------------

start_frontend() {

    require_command python3

    if [ ! -d "$FRONTEND_DIR" ]; then
        error "Frontend directory not found:"
        error "$FRONTEND_DIR"
        exit 1
    fi

    if [ ! -f "$FRONTEND_DIR/test.html" ]; then
        error "frontend/test.html not found."
        exit 1
    fi

    if port_in_use "$FRONTEND_PORT"; then
        warn "Port $FRONTEND_PORT is already in use."
        warn "Frontend may already be running."
        info "Frontend: http://localhost:$FRONTEND_PORT/test.html"
        return
    fi

    info "Starting WeatherGPT API Test Console..."

    (
        cd "$FRONTEND_DIR"

        python3 -m http.server "$FRONTEND_PORT"
    ) &

    FRONTEND_PID=$!

    sleep 2

    if port_in_use "$FRONTEND_PORT"; then
        success "Frontend started!"
        success "Test Console: http://localhost:$FRONTEND_PORT/test.html"
    else
        error "Frontend failed to start."
        exit 1
    fi
}

# ------------------------------------------------------------
# Run tests
# ------------------------------------------------------------

run_tests() {

    require_command mvn

    info "Running WeatherGPT Backend Tests..."

    cd "$BACKEND_DIR"

    mvn clean test

    success "All backend tests completed."
}

# ------------------------------------------------------------
# Build backend
# ------------------------------------------------------------

build_backend() {

    require_command mvn

    info "Building WeatherGPT Backend..."

    cd "$BACKEND_DIR"

    mvn clean package -DskipTests

    success "Backend build completed."
}

# ------------------------------------------------------------
# Stop services
# ------------------------------------------------------------

stop_services() {

    info "Stopping services..."

    stop_port "$BACKEND_PORT"
    stop_port "$FRONTEND_PORT"

    success "WeatherGPT services stopped."
}

# ------------------------------------------------------------
# Main
# ------------------------------------------------------------

COMMAND="${1:-start}"

case "$COMMAND" in

    start)
        printf "\n"
        printf "============================================================\n"
        printf "                 WEATHERGPT STARTUP\n"
        printf "============================================================\n"
        printf "\n"

        start_backend
        start_frontend

        printf "\n"
        success "WeatherGPT is running!"
        printf "\n"

        printf "Backend:\n"
        printf "  http://localhost:%s\n\n" "$BACKEND_PORT"

        printf "API Test Console:\n"
        printf "  http://localhost:%s/test.html\n\n" "$FRONTEND_PORT"

        printf "Press Ctrl+C to stop services.\n"
        printf "\n"

        # Keep script alive when processes were started here
        while true; do
            sleep 60

            if [ -n "$BACKEND_PID" ] && ! kill -0 "$BACKEND_PID" 2>/dev/null; then
                error "Backend process stopped."
                cleanup
            fi

            if [ -n "$FRONTEND_PID" ] && ! kill -0 "$FRONTEND_PID" 2>/dev/null; then
                error "Frontend process stopped."
                cleanup
            fi
        done
        ;;

    backend)
        start_backend

        if [ -n "$BACKEND_PID" ]; then
            wait "$BACKEND_PID"
        fi
        ;;

    frontend)
        start_frontend

        if [ -n "$FRONTEND_PID" ]; then
            wait "$FRONTEND_PID"
        fi
        ;;

    test)
        run_tests
        ;;

    build)
        build_backend
        ;;

    stop)
        stop_services
        ;;

    *)
        error "Unknown command: $COMMAND"

        printf "\nUsage:\n"
        printf "  ./start.sh\n"
        printf "  ./start.sh backend\n"
        printf "  ./start.sh frontend\n"
        printf "  ./start.sh test\n"
        printf "  ./start.sh build\n"
        printf "  ./start.sh stop\n"

        exit 1
        ;;
esac
