param(
    [string]$GatewayUrl = 'http://localhost:8088',
    [string]$PrometheusUrl = 'http://localhost:9090',
    [string]$AlertmanagerUrl = 'http://localhost:9093',
    [string]$LokiUrl = 'http://localhost:3100',
    [string]$GrafanaUrl = 'http://localhost:3000',
    [switch]$SkipFaultExperiment
)

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
$alertName = 'ChatActivityHighErrorRatio'

function Get-PromResult([string]$Query) {
    $encoded = [uri]::EscapeDataString($Query)
    return (Invoke-RestMethod -Uri "$PrometheusUrl/api/v1/query?query=$encoded").data.result
}

function Get-AlertState {
    $alerts = (Invoke-RestMethod -Uri "$PrometheusUrl/api/v1/alerts").data.alerts
    $alert = $alerts | Where-Object { $_.labels.alertname -eq $alertName } | Select-Object -First 1
    if ($alert) { return $alert.state }
    return 'inactive'
}

function Wait-Until([scriptblock]$Condition, [int]$Seconds, [string]$What) {
    $deadline = (Get-Date).AddSeconds($Seconds)
    while ((Get-Date) -lt $deadline) {
        if (& $Condition) { return }
        Start-Sleep -Seconds 3
    }
    throw "Не дождались: $What"
}

function Start-Traffic([int]$Count) {
    $script = "$PSScriptRoot\generate-traffic.ps1"
    return Start-Job -ScriptBlock { param($Path, $Total) & $Path -Count $Total -IntervalMs 300 } -ArgumentList $script, $Count
}

$targets = (Invoke-RestMethod -Uri "$PrometheusUrl/api/v1/targets").data.activeTargets
$down = @($targets | Where-Object { $_.health -ne 'up' })
if ($targets.Count -ne 3 -or $down.Count -gt 0) {
    throw "Targets: всего $($targets.Count), недоступно $($down.Count)"
}

$credential = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes('admin:admin-dev-only'))
$dashboard = Invoke-RestMethod -Uri "$GrafanaUrl/api/dashboards/uid/chat-golden-signals" `
    -Headers @{ Authorization = "Basic $credential" }

$tracing = & "$PSScriptRoot\verify-tracing.ps1" -GatewayUrl $GatewayUrl

$encoded = [uri]::EscapeDataString('{job="course-apps"} | json | requestId="' + $tracing.RequestId + '"')
$start = [DateTimeOffset]::UtcNow.AddMinutes(-10).ToUnixTimeSeconds() * 1000000000
$lokiServices = @()
Wait-Until {
    $streams = (Invoke-RestMethod -Uri "$LokiUrl/loki/api/v1/query_range?query=$encoded&start=$start&limit=100").data.result
    $script:lokiServices = @($streams | ForEach-Object { $_.stream.service } | Sort-Object -Unique)
    $script:lokiServices.Count -eq 3
} 60 'JSON-логи трёх сервисов в Loki'

& "$PSScriptRoot\set-diagnostic-mode.ps1" -Mode NORMAL | Out-Null
& "$PSScriptRoot\generate-traffic.ps1" -Count 30 -IntervalMs 200 | Out-Null

$now = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$exemplarTrace = $null
Wait-Until {
    $query = [uri]::EscapeDataString('http_server_requests_seconds_bucket{job="chat-rest",uri="/api/profiles/{id}/activity"}')
    $url = "$PrometheusUrl/api/v1/query_exemplars?query=$query&start=$($now - 300)&end=$([DateTimeOffset]::UtcNow.ToUnixTimeSeconds())"
    $series = (Invoke-RestMethod -Uri $url).data
    $script:exemplarTrace = @($series | ForEach-Object { $_.exemplars } | ForEach-Object { $_.labels.trace_id } | Where-Object { $_ }) | Select-Object -First 1
    [bool]$script:exemplarTrace
} 30 'exemplar с trace_id в Prometheus'

$experiment = 'skipped'
if (-not $SkipFaultExperiment) {
    try {
        & "$PSScriptRoot\set-diagnostic-mode.ps1" -Mode FAIL -Seconds 120 | Out-Null
        $job = Start-Traffic -Count 180
        Wait-Until { (Get-AlertState) -eq 'firing' } 100 'алерт firing в Prometheus'
        Wait-Until {
            $active = Invoke-RestMethod -Uri "$AlertmanagerUrl/api/v2/alerts?active=true"
            [bool]($active | Where-Object { $_.labels.alertname -eq $alertName })
        } 30 'алерт в Alertmanager'
        Stop-Job $job
        Remove-Job $job -Force
    } finally {
        & "$PSScriptRoot\set-diagnostic-mode.ps1" -Mode NORMAL | Out-Null
    }
    $recovery = Start-Traffic -Count 400
    try {
        Wait-Until { (Get-AlertState) -eq 'inactive' } 120 'снятие алерта'
    } finally {
        Stop-Job $recovery
        Remove-Job $recovery -Force
    }
    $experiment = '503 -> firing -> Alertmanager -> NORMAL -> resolved'
}

[pscustomobject]@{
    Status          = 'OK'
    Targets         = ($targets | ForEach-Object { $_.labels.job } | Sort-Object) -join ','
    Dashboard       = $dashboard.dashboard.title
    HttpSpans       = $tracing.HttpSpans
    LokiServices    = $lokiServices -join ','
    Exemplar        = "OK trace_id=$exemplarTrace"
    FaultExperiment = $experiment
}
