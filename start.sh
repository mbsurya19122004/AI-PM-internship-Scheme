#!/usr/bin/env fish

set SCRIPT_DIR (cd (dirname (status --current-filename)); and pwd)
set BACKEND_DIR "$SCRIPT_DIR/backend"
set FRONTEND_DIR "$SCRIPT_DIR/frontend"
set ML_DIR "$SCRIPT_DIR/ML"

# ============================================================
# START EVERYTHING
# ============================================================

function start_all
    # Kill any lingering processes on our ports
    lsof -ti:8080 | xargs kill -9 2>/dev/null
    lsof -ti:8000 | xargs kill -9 2>/dev/null
    lsof -ti:3000 | xargs kill -9 2>/dev/null
    sleep 1

    echo "=========================================="
    echo "       INTERNSHIP PLATFORM"
    echo "=========================================="
    echo ""

    # Check Java
    if not command -q java
        echo "ERROR: Java not found. Please install Java 17+."
        exit 1
    end

    echo "Java:"
    java -version 2>&1 | head -1
    echo ""

    # Check Maven
    if not command -q mvn
        echo "ERROR: Maven not found."
        exit 1
    end

    echo "Maven: OK"
    echo ""

    # --------------------------------------------------------
    # ML SERVICE
    # --------------------------------------------------------

    if command -q python3
        echo "Starting ML service..."
        echo "URL: http://localhost:8000"

        cd "$ML_DIR"

        python3 -m pip install -q -r requirements.txt 2>/dev/null

        python3 -m uvicorn app:app \
            --host 0.0.0.0 \
            --port 8000 &

        set ML_PID $last_pid

        echo "ML Docs: http://localhost:8000/docs"
    else
        echo "WARNING: Python3 not found."
        echo "Skipping ML service."
    end

    echo ""

    # --------------------------------------------------------
    # BACKEND
    # --------------------------------------------------------

    cd "$BACKEND_DIR"

    echo "Building backend..."

    mvn -q compile -DskipTests

    echo ""
    echo "Starting backend..."
    echo "URL: http://localhost:8080"

    mvn spring-boot:run &

    set BACKEND_PID $last_pid

    echo ""

    # --------------------------------------------------------
    # FRONTEND
    # --------------------------------------------------------

    if command -q python3
        echo "Starting frontend..."
        echo "URL: http://localhost:3000"

        cd "$FRONTEND_DIR"

        python3 -m http.server 3000 &

        set FRONTEND_PID $last_pid

        echo "Test Console: http://localhost:3000/test.html"
    else
        echo "WARNING: Python3 not found."
        echo "Skipping frontend server."
    end

    echo ""
    echo "=========================================="
    echo "  BACKEND:  http://localhost:8080"
    echo "  ML API:   http://localhost:8000"
    echo "  FRONTEND: http://localhost:3000/test.html"
    echo "=========================================="
    echo ""
    echo "Press Ctrl+C to stop."

    # Wait for processes
    while true
        sleep 1

        if test -n "$BACKEND_PID"
            if not kill -0 $BACKEND_PID 2>/dev/null
                echo "Backend stopped."
                break
            end
        end
    end

    # Cleanup
    if test -n "$ML_PID"
        kill $ML_PID 2>/dev/null
    end

    if test -n "$BACKEND_PID"
        kill $BACKEND_PID 2>/dev/null
    end

    if test -n "$FRONTEND_PID"
        kill $FRONTEND_PID 2>/dev/null
    end
end


# ============================================================
# ML ONLY
# ============================================================

function start_ml
    echo "Starting ML service on http://localhost:8000"
    echo "Model auto-detected from Ollama"

    cd "$ML_DIR"

    python3 -m uvicorn app:app \
        --host 0.0.0.0 \
        --port 8000
end


# ============================================================
# API TEST SUITE
# ============================================================

function run_tests

    # Kill any lingering process on port 8080
    lsof -ti:8080 | xargs kill -9 2>/dev/null
    sleep 1

    cd "$BACKEND_DIR"

    echo "Building backend..."
    mvn -q package -DskipTests 2>/dev/null

    echo "Starting server..."

    java -jar target/internship-platform-backend-1.0.0.jar &

    set SPRING_PID $last_pid

    function cleanup_tests --on-event fish_exit
        kill $SPRING_PID 2>/dev/null
    end

    echo "Waiting for server..."

    set SERVER_READY false

    for i in (seq 1 30)

        set response (curl -s -o /dev/null -w '%{http_code}' \
            http://localhost:8080/api/auth/login \
            -X POST \
            -H "Content-Type: application/json" \
            -d '{"email":"x@x.com","password":"x"}' \
            2>/dev/null)

        if test -n "$response" -a "$response" != "000"
            echo "Server ready! (HTTP $response)"
            set SERVER_READY true
            break
        end

        if not kill -0 $SPRING_PID 2>/dev/null
            echo "ERROR: Server died during startup."
            return 1
        end

        sleep 2
    end

    if test "$SERVER_READY" = false
        echo "ERROR: Server did not start."
        return 1
    end

    set PASS 0
    set FAIL 0

    # check: prints PASS/FAIL and updates counters.
    # In fish, 'set' inside a function only modifies local scope,
    # so we print the result and use 'or'/'and' to update counters inline.
    function check
        set name $argv[1]
        set expected $argv[2]
        set actual $argv[3]

        if string match -q "*$expected*" "$actual"
            echo "  PASS: $name"
        else
            echo "  FAIL: $name"
            echo "       Expected: $expected"
            echo "       Got: "(string sub -l 200 "$actual")
        end
    end

    echo ""
    echo "=========================================="
    echo "       FULL API TEST SUITE"
    echo "=========================================="
    echo ""

    # 1. REGISTER
    echo "1. Register new user"

    set R1 (curl -s -X POST \
        http://localhost:8080/api/auth/register \
        -H "Content-Type: application/json" \
        -d '{"fullName":"Abhay Kumar","email":"abhay@example.com","phoneNumber":"9876543210","college":"IIT Delhi","department":"Computer Science","graduationYear":"2027","password":"Pass@1234","confirmPassword":"Pass@1234"}')

    check "Register returns success" "true" "$R1"
    and set PASS (math $PASS + 1)
    or set FAIL (math $FAIL + 1)

    # 2. DUPLICATE EMAIL
    echo "2. Duplicate email"

    set R2 (curl -s -X POST \
        http://localhost:8080/api/auth/register \
        -H "Content-Type: application/json" \
        -d '{"fullName":"Test","email":"abhay@example.com","phoneNumber":"1234567890","college":"IIT Bombay","password":"Pass@1234","confirmPassword":"Pass@1234"}')

    check "Duplicate email rejected" "already registered" "$R2"
    and set PASS (math $PASS + 1)
    or set FAIL (math $FAIL + 1)

    # 3. INVALID PASSWORD
    echo "3. Invalid password"

    set R3 (curl -s -X POST \
        http://localhost:8080/api/auth/register \
        -H "Content-Type: application/json" \
        -d '{"fullName":"Test","email":"t@t.com","phoneNumber":"1111111111","college":"IIT","password":"weak","confirmPassword":"weak"}')

    check "Weak password rejected" "Validation failed" "$R3"
    and set PASS (math $PASS + 1)
    or set FAIL (math $FAIL + 1)

    # 4. PASSWORD MISMATCH
    echo "4. Mismatched passwords"

    set R4 (curl -s -X POST \
        http://localhost:8080/api/auth/register \
        -H "Content-Type: application/json" \
        -d '{"fullName":"Test","email":"t2@t.com","phoneNumber":"2222222222","college":"IIT","password":"Pass@1234","confirmPassword":"Different@123"}')

    check "Mismatched passwords rejected" "Passwords do not match" "$R4"
    and set PASS (math $PASS + 1)
    or set FAIL (math $FAIL + 1)

    # 5. LOGIN
    echo "5. Login"

    set LOGIN (curl -s -X POST \
        http://localhost:8080/api/auth/login \
        -H "Content-Type: application/json" \
        -d '{"email":"abhay@example.com","password":"Pass@1234"}')

    check "Login returns success" "true" "$LOGIN"
    and set PASS (math $PASS + 1)
    or set FAIL (math $FAIL + 1)

    set ACCESS (echo "$LOGIN" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',{}).get('accessToken',''))")
    set REFRESH (echo "$LOGIN" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',{}).get('refreshToken',''))")

    if test -z "$ACCESS"
        echo "FATAL: Login failed."
        return 1
    end

    # 6. WRONG PASSWORD
    echo "6. Wrong password"

    set R6 (curl -s -X POST \
        http://localhost:8080/api/auth/login \
        -H "Content-Type: application/json" \
        -d '{"email":"abhay@example.com","password":"WrongPass@123"}')

    check "Wrong password rejected" "Invalid email or password" "$R6"
    and set PASS (math $PASS + 1)
    or set FAIL (math $FAIL + 1)

    # 7. GET ME
    echo "7. Get profile with token"

    set R7 (curl -s \
        http://localhost:8080/api/auth/me \
        -H "Authorization: Bearer $ACCESS")

    check "Get /me success" "Abhay Kumar" "$R7"
    and set PASS (math $PASS + 1)
    or set FAIL (math $FAIL + 1)

    # 8. NO TOKEN
    echo "8. Get profile without token"

    set R8 (curl -s http://localhost:8080/api/auth/me)

    check "No token rejected" "Authentication required" "$R8"
    and set PASS (math $PASS + 1)
    or set FAIL (math $FAIL + 1)

    # 9. REFRESH
    echo "9. Refresh token"

    set R9 (curl -s -X POST \
        http://localhost:8080/api/auth/refresh \
        -H "Content-Type: application/json" \
        -d "{\"refreshToken\":\"$REFRESH\"}")

    check "Refresh returns new tokens" "accessToken" "$R9"
    and set PASS (math $PASS + 1)
    or set FAIL (math $FAIL + 1)

    # 10. UPLOAD RESUME
    echo "10. Upload resume 1"

    echo "%PDF-1.4 resume content" > /tmp/test-resume.pdf

    set U1 (curl -s -X POST \
        http://localhost:8080/api/resumes/upload \
        -H "Authorization: Bearer $ACCESS" \
        -F "file=@/tmp/test-resume.pdf" \
        -F "description=Main resume")

    check "Upload resume 1 success" "Resume uploaded" "$U1"
    and set PASS (math $PASS + 1)
    or set FAIL (math $FAIL + 1)

    set R1_ID (echo "$U1" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['id'])")

    # 11. UPLOAD RESUME 2
    echo "11. Upload resume 2"

    echo "%PDF-1.4 second resume" > /tmp/test-resume2.pdf

    set U2 (curl -s -X POST \
        http://localhost:8080/api/resumes/upload \
        -H "Authorization: Bearer $ACCESS" \
        -F "file=@/tmp/test-resume2.pdf" \
        -F "description=Internship resume")

    check "Upload resume 2 success" "Resume uploaded" "$U2"
    and set PASS (math $PASS + 1)
    or set FAIL (math $FAIL + 1)

    set R2_ID (echo "$U2" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['id'])")

    # 12. LIST RESUMES
    echo "12. List resumes"

    set R12 (curl -s \
        http://localhost:8080/api/resumes \
        -H "Authorization: Bearer $ACCESS")

    check "List resumes" "originalFileName" "$R12"
    and set PASS (math $PASS + 1)
    or set FAIL (math $FAIL + 1)

    # 13. ACTIVE RESUME
    echo "13. Get active resume"

    set R13 (curl -s \
        http://localhost:8080/api/resumes/active \
        -H "Authorization: Bearer $ACCESS")

    check "Active resume exists" "active" "$R13"
    and set PASS (math $PASS + 1)
    or set FAIL (math $FAIL + 1)

    # 14. ACTIVATE RESUME
    echo "14. Activate resume 2"

    set R14 (curl -s -X PUT \
        "http://localhost:8080/api/resumes/$R2_ID/activate" \
        -H "Authorization: Bearer $ACCESS")

    check "Resume 2 activated" "Resume activated" "$R14"
    and set PASS (math $PASS + 1)
    or set FAIL (math $FAIL + 1)

    # 15. GET BY ID
    echo "15. Get resume by ID"

    set R15 (curl -s \
        "http://localhost:8080/api/resumes/$R1_ID" \
        -H "Authorization: Bearer $ACCESS")

    check "Get resume by ID success" "originalFileName" "$R15"
    and set PASS (math $PASS + 1)
    or set FAIL (math $FAIL + 1)

    # 16. DOWNLOAD
    echo "16. Download resume"

    curl -s \
        "http://localhost:8080/api/resumes/$R1_ID/download" \
        -H "Authorization: Bearer $ACCESS" \
        -o /tmp/downloaded.pdf

    if test -f /tmp/downloaded.pdf
        set CONTENT (cat /tmp/downloaded.pdf)

        if test "$CONTENT" = "%PDF-1.4 resume content"
            echo "  PASS: Download resume"
            set PASS (math $PASS + 1)
        else
            echo "  FAIL: Download resume"
            set FAIL (math $FAIL + 1)
        end
    else
        echo "  FAIL: Download resume"
        set FAIL (math $FAIL + 1)
    end

    # 17. DELETE
    echo "17. Delete resume 1"

    set R17 (curl -s -X DELETE \
        "http://localhost:8080/api/resumes/$R1_ID" \
        -H "Authorization: Bearer $ACCESS")

    check "Delete resume success" "Resume deleted" "$R17"
    and set PASS (math $PASS + 1)
    or set FAIL (math $FAIL + 1)

    # 18. VERIFY DELETE
    echo "18. Verify delete"

    set R18 (curl -s \
        http://localhost:8080/api/resumes \
        -H "Authorization: Bearer $ACCESS")

    set COUNT18 (echo "$R18" | python3 -c "import sys,json; print(len(json.load(sys.stdin)['data']))")

    if test "$COUNT18" = "1"
        echo "  PASS: 1 resume remaining"
        set PASS (math $PASS + 1)
    else
        echo "  FAIL: Expected 1 resume, got $COUNT18"
        set FAIL (math $FAIL + 1)
    end

    # 19. INVALID FILE
    echo "19. Invalid file type"

    echo "not a pdf" > /tmp/test.txt

    set R19 (curl -s -X POST \
        http://localhost:8080/api/resumes/upload \
        -H "Authorization: Bearer $ACCESS" \
        -F "file=@/tmp/test.txt;type=text/plain")

    check "Invalid file rejected" "Only PDF and Word" "$R19"
    and set PASS (math $PASS + 1)
    or set FAIL (math $FAIL + 1)

    # 20. UNAUTHORIZED
    echo "20. Unauthorized access"

    set R20 (curl -s http://localhost:8080/api/resumes)

    check "Unauthorized returns 401" "Authentication required" "$R20"
    and set PASS (math $PASS + 1)
    or set FAIL (math $FAIL + 1)

    # 21. GET ALL INTERNSHIPS
    echo "21. Get all internships"

    set R21 (curl -s \
        http://localhost:8080/api/internships \
        -H "Authorization: Bearer $ACCESS")

    check "Get internships returns data" "id" "$R21"
    and set PASS (math $PASS + 1)
    or set FAIL (math $FAIL + 1)

    # 22. CREATE INTERNSHIP
    echo "22. Create internship"

    set R22 (curl -s -X POST \
        http://localhost:8080/api/internships \
        -H "Authorization: Bearer $ACCESS" \
        -H "Content-Type: application/json" \
        -d '{"title":"Software Engineer Intern","company":"Google","description":"Work on search infrastructure","applicationLink":"https://careers.google.com/apply"}')

    check "Create internship returns success" "Software Engineer Intern" "$R22"
    and set PASS (math $PASS + 1)
    or set FAIL (math $FAIL + 1)

    set NEW_ID (echo "$R22" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['id'])")

    # 23. GET INTERNSHIP BY ID
    echo "23. Get internship by ID"

    set R23 (curl -s \
        "http://localhost:8080/api/internships/$NEW_ID" \
        -H "Authorization: Bearer $ACCESS")

    check "Get internship by ID" "Software Engineer Intern" "$R23"
    and set PASS (math $PASS + 1)
    or set FAIL (math $FAIL + 1)

    # 24. INTERNSHIP UNAUTHORIZED
    echo "24. Internship unauthorized"

    set R24 (curl -s http://localhost:8080/api/internships)

    check "Internship unauthorized returns 401" "Authentication required" "$R24"
    and set PASS (math $PASS + 1)
    or set FAIL (math $FAIL + 1)

    # 25. RESEND VERIFICATION
    echo "25. Resend verification email"

    set R25 (curl -s -X POST \
        http://localhost:8080/api/auth/resend-verification \
        -H "Authorization: Bearer $ACCESS")

    check "Resend verification returns success" "Verification email sent" "$R25"
    and set PASS (math $PASS + 1)
    or set FAIL (math $FAIL + 1)

    # 26. PROFILE PICTURE UPLOAD
    echo "26. Upload profile picture"

    echo -n "\xff\xd8\xff\xe0" > /tmp/test-pic.jpg

    set R26 (curl -s -X POST \
        http://localhost:8080/api/users/me/profile-picture \
        -H "Authorization: Bearer $ACCESS" \
        -F "file=@/tmp/test-pic.jpg;type=image/jpeg")

    check "Upload profile picture success" "Profile picture uploaded" "$R26"
    and set PASS (math $PASS + 1)
    or set FAIL (math $FAIL + 1)

    # 27. GET PROFILE PICTURE
    echo "27. Get profile picture"

    set R27_STATUS (curl -s -o /dev/null -w "%{http_code}" \
        http://localhost:8080/api/users/me/profile-picture \
        -H "Authorization: Bearer $ACCESS")

    if test "$R27_STATUS" = "200"
        echo "  PASS: Get profile picture returns 200"
        set PASS (math $PASS + 1)
    else
        echo "  FAIL: Get profile picture returned $R27_STATUS"
        set FAIL (math $FAIL + 1)
    end

    # 28. DELETE PROFILE PICTURE
    echo "28. Delete profile picture"

    set R28 (curl -s -X DELETE \
        http://localhost:8080/api/users/me/profile-picture \
        -H "Authorization: Bearer $ACCESS")

    check "Delete profile picture success" "Profile picture deleted" "$R28"
    and set PASS (math $PASS + 1)
    or set FAIL (math $FAIL + 1)

    # 29. ADMIN LIST USERS (should fail for ROLE_USER)
    echo "29. Admin list users (should 403 for ROLE_USER)"

    set R29 (curl -s http://localhost:8080/api/admin/users \
        -H "Authorization: Bearer $ACCESS")

    check "Admin access denied for ROLE_USER" "Access denied" "$R29"
    and set PASS (math $PASS + 1)
    or set FAIL (math $FAIL + 1)

    echo ""
    echo "=========================================="
    echo "RESULTS: $PASS passed, $FAIL failed (29 tests)"
    echo "=========================================="

    kill $SPRING_PID 2>/dev/null

    if test $FAIL -gt 0
        return 1
    end
end


# ============================================================
# MAIN
# ============================================================

switch "$argv[1]"
    case ""
        start_all

    case start
        start_all

    case ml
        start_ml

    case test
        run_tests

    case '*'
        echo "Usage:"
        echo ""
        echo "  ./start.sh"
        echo "      Start ML + Backend + Frontend"
        echo ""
        echo "  ./start.sh ml"
        echo "      Start ML service only"
        echo ""
        echo "  ./start.sh test"
        echo "      Run API test suite"
end