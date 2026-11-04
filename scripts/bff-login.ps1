param(
    [string]$GatewayUrl = 'http://localhost:8088',
    [string]$Username = 'user',
    [string]$Password = 'user'
)

$ProgressPreference = 'SilentlyContinue'
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$form = Invoke-WebRequest -UseBasicParsing -Uri "$GatewayUrl/oauth2/authorization/keycloak" -WebSession $session
if ($form.Content -notmatch 'action="([^"]+)"') {
    throw 'Форма входа Keycloak не найдена'
}
$action = [System.Net.WebUtility]::HtmlDecode($Matches[1])
$null = Invoke-WebRequest -UseBasicParsing -Method Post -Uri $action -WebSession $session `
    -ContentType 'application/x-www-form-urlencoded' `
    -Body @{ username = $Username; password = $Password; credentialId = '' }
$me = Invoke-RestMethod -Uri "$GatewayUrl/bff/me" -WebSession $session
if (-not $me.authenticated) {
    throw 'Вход через BFF не выполнен'
}
$session
