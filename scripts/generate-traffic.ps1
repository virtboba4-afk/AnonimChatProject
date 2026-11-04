param(
    [int]$Count = 30,
    [int]$IntervalMs = 200,
    [string]$RestUrl = 'http://localhost:8089'
)

$ProgressPreference = 'SilentlyContinue'
$token = & "$PSScriptRoot\get-service-token.ps1" -Client service
$headers = @{ Authorization = "Bearer $token" }

$nickname = 'traffic-' + [guid]::NewGuid().ToString('N').Substring(0, 12)
$profileBody = @{ nickname = $nickname; age = 30; preferredLanguage = 'en' } | ConvertTo-Json
$profile = Invoke-RestMethod -Method Post -Uri "$RestUrl/api/profiles" -Headers $headers `
    -ContentType 'application/json' -Body $profileBody

$statuses = @{}
for ($i = 0; $i -lt $Count; $i++) {
    try {
        $response = Invoke-WebRequest -UseBasicParsing -Uri "$RestUrl/api/profiles/$($profile.id)/activity" -Headers $headers
        $status = [int]$response.StatusCode
    } catch [System.Net.WebException] {
        if ($null -eq $_.Exception.Response) { throw }
        $status = [int]$_.Exception.Response.StatusCode
    } catch {
        if ($null -eq $_.Exception.Response) { throw }
        $status = [int]$_.Exception.Response.StatusCode
    }
    $statuses["$status"] = 1 + [int]$statuses["$status"]
    Start-Sleep -Milliseconds $IntervalMs
}
"Requests=$Count Statuses=$($statuses | ConvertTo-Json -Compress)"
