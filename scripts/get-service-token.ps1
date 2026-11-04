param(
    [ValidateSet('service', 'ops')]
    [string]$Client = 'service',
    [string]$KeycloakUrl = 'http://localhost:8085'
)

$secret = "course-$Client-secret"
$body = @{
    grant_type    = 'client_credentials'
    client_id     = "course-$Client-client"
    client_secret = $secret
}
$response = Invoke-RestMethod -Method Post -Uri "$KeycloakUrl/realms/course/protocol/openid-connect/token" `
    -ContentType 'application/x-www-form-urlencoded' -Body $body
$response.access_token
