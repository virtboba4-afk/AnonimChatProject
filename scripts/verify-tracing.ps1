param(
    [string]$GatewayUrl = 'http://localhost:8088',
    [string]$RestUrl = 'http://localhost:8089',
    [string]$TempoUrl = 'http://localhost:3200',
    [string]$LogDir = "$PSScriptRoot\..\runtime-logs"
)

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'

Add-Type -AssemblyName System.Net.Http

function Assert-Equal($Expected, $Actual, [string]$What) {
    if ("$Expected" -ne "$Actual") {
        throw "$What`: ожидалось '$Expected', получено '$Actual'"
    }
}

function Send-Request {
    param([string]$Uri, [string]$Method = 'GET', [hashtable]$Headers = @{}, [string]$Body, $Session)
    $handler = New-Object System.Net.Http.HttpClientHandler
    $handler.AllowAutoRedirect = $false
    $handler.UseProxy = $false
    if ($Session) { $handler.CookieContainer = $Session.Cookies }
    $client = New-Object System.Net.Http.HttpClient($handler)
    try {
        $request = New-Object System.Net.Http.HttpRequestMessage([System.Net.Http.HttpMethod]::new($Method), $Uri)
        foreach ($key in $Headers.Keys) {
            $null = $request.Headers.TryAddWithoutValidation($key, [string]$Headers[$key])
        }
        if ($Body) {
            $request.Content = New-Object System.Net.Http.StringContent($Body, [System.Text.Encoding]::UTF8, 'application/json')
        }
        $response = $client.SendAsync($request).GetAwaiter().GetResult()
        $requestIds = @()
        $values = $null
        if ($response.Headers.TryGetValues('X-Request-ID', [ref]$values)) { $requestIds = @($values) }
        return [pscustomobject]@{
            Status     = [int]$response.StatusCode
            RequestIds = $requestIds
            Content    = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
        }
    } finally {
        $client.Dispose()
    }
}

$session = & "$PSScriptRoot\bff-login.ps1" -GatewayUrl $GatewayUrl
$csrf = Invoke-RestMethod -Uri "$GatewayUrl/bff/csrf" -WebSession $session

$nickname = 'trace-' + [guid]::NewGuid().ToString('N').Substring(0, 12)
$profileBody = @{ nickname = $nickname; age = 30; preferredLanguage = 'en' } | ConvertTo-Json
$created = Send-Request -Uri "$GatewayUrl/api/profiles" -Method POST -Session $session `
    -Headers @{ $csrf.headerName = $csrf.token } -Body $profileBody
Assert-Equal 201 $created.Status 'Создание профиля через BFF'
$profileId = ($created.Content | ConvertFrom-Json).id

$requestId = [guid]::NewGuid().ToString()
$activity = Send-Request -Uri "$GatewayUrl/api/profiles/$profileId/activity" -Session $session `
    -Headers @{ 'X-Request-ID' = $requestId }
Assert-Equal 200 $activity.Status 'Активность профиля через BFF'
Assert-Equal 1 $activity.RequestIds.Count 'Количество заголовков X-Request-ID'
Assert-Equal $requestId $activity.RequestIds[0] 'Сохранённый X-Request-ID'

$replaced = Send-Request -Uri "$GatewayUrl/api/profiles/$profileId/activity" -Session $session `
    -Headers @{ 'X-Request-ID' = '1-1-1-1-1' }
Assert-Equal 200 $replaced.Status 'Запрос с невалидным X-Request-ID'
Assert-Equal 1 $replaced.RequestIds.Count 'Количество заголовков при замене'
if ($replaced.RequestIds[0] -eq '1-1-1-1-1') { throw 'Невалидный X-Request-ID не заменён' }

$denied = Send-Request -Uri "$GatewayUrl/api/profiles" -Method POST -Session $session `
    -Headers @{ 'X-Request-ID' = 'not-a-uuid' } -Body $profileBody
Assert-Equal 403 $denied.Status 'POST без CSRF'
Assert-Equal 1 $denied.RequestIds.Count 'X-Request-ID при отказе CSRF'

$direct = Send-Request -Uri "$RestUrl/api/profiles"
Assert-Equal 401 $direct.Status 'Прямой запрос без токена'
Assert-Equal 1 $direct.RequestIds.Count 'X-Request-ID при 401'

$missing = Send-Request -Uri "$GatewayUrl/api/profiles/$([guid]::NewGuid())/activity" -Session $session
Assert-Equal 404 $missing.Status 'Активность несуществующего профиля'

$lines = @()
$services = 'chat-gateway', 'chat-rest', 'chat-audit'
$deadline = (Get-Date).AddSeconds(15)
do {
    Start-Sleep -Seconds 1
    $lines = foreach ($service in $services) {
        Get-Content "$LogDir\$service.log" -Encoding UTF8 |
            Where-Object { $_ -like "*$requestId*" } |
            ForEach-Object { $_ | ConvertFrom-Json }
    }
    $seen = @($lines | ForEach-Object { $_.'service.name' } | Sort-Object -Unique)
} while ($seen.Count -lt 3 -and (Get-Date) -lt $deadline)
Assert-Equal 3 $seen.Count 'Сервисы с записями по request ID'

$traceIds = @($lines | ForEach-Object { $_.traceId } | Sort-Object -Unique)
Assert-Equal 1 $traceIds.Count 'Количество trace ID в логах'
$traceId = $traceIds[0]

$httpSpans = 0
$deadline = (Get-Date).AddSeconds(30)
do {
    Start-Sleep -Seconds 2
    $trace = $null
    try {
        $trace = Invoke-RestMethod -Uri "$TempoUrl/api/traces/$traceId"
    } catch {
        continue
    }
    $spans = @($trace.batches | ForEach-Object { $_.scopeSpans } | ForEach-Object { $_.spans })
    $http = @($spans | Where-Object {
        $_.kind -in 'SPAN_KIND_SERVER', 'SPAN_KIND_CLIENT' -and
        ($_.attributes | Where-Object { $_.key -match '^(http\.|method$|uri$)' })
    })
    $httpSpans = $http.Count
} while ($httpSpans -lt 5 -and (Get-Date) -lt $deadline)
Assert-Equal 5 $httpSpans 'HTTP-спаны в Tempo'

$ids = @($http | ForEach-Object { $_.spanId })
$roots = @($http | Where-Object { $ids -notcontains $_.parentSpanId })
Assert-Equal 1 $roots.Count 'Корневые HTTP-спаны'
Assert-Equal 5 (@($ids | Sort-Object -Unique).Count) 'Уникальные span ID'

[pscustomobject]@{
    Status          = 'OK'
    HttpSpans       = $httpSpans
    TraceId         = $traceId
    RequestId       = $requestId
    ReplacedInvalid = $replaced.RequestIds[0]
    CsrfDenied      = "$($denied.Status) $($denied.RequestIds[0])"
    DirectNoToken   = "$($direct.Status) $($direct.RequestIds[0])"
    ProfileId       = $profileId
}
