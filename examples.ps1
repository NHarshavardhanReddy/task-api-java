# Usage:  .\examples.ps1 [-Base http://localhost:8081]
param([string]$Base = "http://localhost:8081")

function Show-Error($label, [scriptblock]$call) {
    try { & $call | Out-Null; Write-Host "$label -> unexpected success" }
    catch { Write-Host "$label -> HTTP $([int]$_.Exception.Response.StatusCode)" }
}

# 1. Log in
$login = Invoke-RestMethod -Method Post -Uri "$Base/login" -ContentType "application/json" `
    -Body '{"username":"admin","password":"admin123"}'
$token = if ($login.accessToken) { $login.accessToken } else { $login.access_token }
$h = @{ Authorization = "Bearer $token" }
Write-Host "Logged in, token received."

# 2. Create
$task = Invoke-RestMethod -Method Post -Uri "$Base/tasks" -Headers $h -ContentType "application/json" `
    -Body '{"title":"Write report","description":"Q4 numbers","status":"TODO","dueDate":"2026-12-31"}'
$task | ConvertTo-Json
$id = $task.id

# 3. List (filter + pagination), get one
Invoke-RestMethod -Uri "$Base/tasks?status=TODO&page=1&limit=10" -Headers $h | ConvertTo-Json -Depth 5
Invoke-RestMethod -Uri "$Base/tasks/$id" -Headers $h | ConvertTo-Json

# 4. Update
Invoke-RestMethod -Method Put -Uri "$Base/tasks/$id" -Headers $h -ContentType "application/json" `
    -Body '{"title":"Write report (final)","status":"IN_PROGRESS","dueDate":"2026-12-31"}' | ConvertTo-Json

# 5. Delete (204)
Invoke-RestMethod -Method Delete -Uri "$Base/tasks/$id" -Headers $h
Write-Host "Deleted task $id"

# 6. Error cases
Show-Error "GET /tasks without token (expect 401)" { Invoke-RestMethod -Uri "$Base/tasks" }
Show-Error "GET /tasks/999 (expect 404)" { Invoke-RestMethod -Uri "$Base/tasks/999" -Headers $h }
Show-Error "POST blank title (expect 400)" { Invoke-RestMethod -Method Post -Uri "$Base/tasks" -Headers $h -ContentType "application/json" -Body '{"title":"   "}' }
