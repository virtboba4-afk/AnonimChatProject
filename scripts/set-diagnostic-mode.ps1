param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('NORMAL', 'SLOW', 'FAIL')]
    [string]$Mode,
    [ValidateRange(1, 120)]
    [int]$Seconds = 60,
    [string]$AuditUrl = 'http://localhost:8081'
)

$token = & "$PSScriptRoot\get-service-token.ps1" -Client ops
$uri = "$AuditUrl/internal/diagnostics/mode?mode=$Mode&seconds=$Seconds"
$result = Invoke-RestMethod -Method Put -Uri $uri -Headers @{ Authorization = "Bearer $token" }
"Mode=$($result.mode)"
