#!/usr/bin/env bash
# Usage: ./examples.sh [BASE_URL]      (default http://localhost:8081)
BASE=${1:-http://localhost:8081}

# 1. Log in and capture the JWT
TOKEN=$(curl -s -X POST "$BASE/login" -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | python3 -c "import sys,json;print(json.load(sys.stdin)['accessToken'])")
AUTH="Authorization: Bearer $TOKEN"

# 2. Create
curl -s -X POST "$BASE/tasks" -H "$AUTH" -H "Content-Type: application/json" \
  -d '{"title":"Write report","description":"Q4 numbers","status":"TODO","dueDate":"2026-12-31"}'; echo

# 3. List (filter + pagination)
curl -s "$BASE/tasks" -H "$AUTH"; echo
curl -s "$BASE/tasks?status=TODO&page=1&limit=10" -H "$AUTH"; echo

# 4. Get one
curl -s "$BASE/tasks/1" -H "$AUTH"; echo

# 5. Update (full replacement)
curl -s -X PUT "$BASE/tasks/1" -H "$AUTH" -H "Content-Type: application/json" \
  -d '{"title":"Write report (final)","status":"IN_PROGRESS","dueDate":"2026-12-31"}'; echo

# 6. Delete -> 204 No Content
curl -s -o /dev/null -w "DELETE -> %{http_code}\n" -X DELETE "$BASE/tasks/1" -H "$AUTH"

# 7. Error cases
curl -s -w " [%{http_code}]\n" "$BASE/tasks"                                    # 401 no token
curl -s -w " [%{http_code}]\n" "$BASE/tasks/999" -H "$AUTH"                     # 404
curl -s -w " [%{http_code}]\n" -X POST "$BASE/tasks" -H "$AUTH" \
  -H "Content-Type: application/json" -d '{"title":"   "}'                      # 400
