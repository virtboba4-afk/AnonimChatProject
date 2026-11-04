$ProgressPreference = 'SilentlyContinue'
$urls = 'http://localhost:8088/readyz', 'http://localhost:8089/readyz', 'http://localhost:8081/readyz',
    'http://localhost:8088/livez', 'http://localhost:8089/livez', 'http://localhost:8081/livez',
    'http://localhost:9088/actuator/health', 'http://localhost:9089/actuator/health', 'http://localhost:9081/actuator/health',
    'http://localhost:9090/-/ready', 'http://localhost:9093/-/ready', 'http://localhost:3100/ready',
    'http://localhost:3200/ready', 'http://localhost:3000/api/health',
    'http://localhost:8085/realms/course/.well-known/openid-configuration'
foreach ($url in $urls) {
    try {
        $status = [int](Invoke-WebRequest -UseBasicParsing -Uri $url).StatusCode
    } catch {
        $status = if ($_.Exception.Response) { [int]$_.Exception.Response.StatusCode } else { 'недоступен' }
    }
    "{0,-12} {1}" -f $status, $url
}
