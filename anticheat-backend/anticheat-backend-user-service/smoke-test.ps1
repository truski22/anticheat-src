<#
.SYNOPSIS
  Smoke test for user-service. Since the service went back to speaking gRPC
  (see README.md), PowerShell can no longer exercise register/login/etc. on
  its own -- it doesn't speak HTTP/2 + Protobuf without extra tooling (grpcurl).
  The real functional coverage (register, login, UNV/ENV duplicates, race
  condition, INVALID_ARGUMENT, NOT_FOUND) now lives in
  src/test/java/.../grpc/UserGrpcServiceTest.java -- run it with `mvn test`.
  The "change password while authenticated" flow (POST /auth/change-password
  with JWT) lives in the Gateway, not here -- see anticheat-backend-gateway/README.md.

  This script only checks that the process started and that Actuator sees the
  Postgres connection as healthy (port 9091, plain HTTP).

.PARAMETER HealthUrl
  Base URL of the health-check port. Defaults to http://localhost:9091

.EXAMPLE
  .\smoke-test.ps1
  .\smoke-test.ps1 -HealthUrl http://localhost:9091
#>
param(
    [string]$HealthUrl = "http://localhost:9091"
)

$ErrorActionPreference = "Stop"
$script:pass = 0
$script:fail = 0

function Invoke-Health {
    param([string]$Path)
    try {
        $resp = Invoke-WebRequest -Uri "$HealthUrl$Path" -Method GET -UseBasicParsing
        $content = $resp.Content
        if ($content -is [byte[]]) { $content = [System.Text.Encoding]::UTF8.GetString($content) }
        return [pscustomobject]@{ Status = [int]$resp.StatusCode; Body = $content }
    } catch [System.Net.WebException] {
        $webResponse = $_.Exception.Response
        if (-not $webResponse) { throw }
        $stream = $webResponse.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($stream)
        return [pscustomobject]@{ Status = [int]$webResponse.StatusCode; Body = $reader.ReadToEnd() }
    }
}

function Assert-Test {
    param([string]$Name, [scriptblock]$Check)
    try {
        $ok = & $Check
    } catch {
        $ok = $false
        $err = $_.Exception.Message
    }
    if ($ok) {
        Write-Host "  PASS  $Name" -ForegroundColor Green
        $script:pass++
    } else {
        $msg = if ($err) { " ($err)" } else { "" }
        Write-Host "  FAIL  $Name$msg" -ForegroundColor Red
        $script:fail++
    }
}

Write-Host "=== user-service smoke test (health only; see README for the rest) ===" -ForegroundColor Cyan
Write-Host "Health: $HealthUrl`n"

$overall = Invoke-Health "/actuator/health"
Assert-Test "GET /actuator/health -> 200" { $overall.Status -eq 200 }
Assert-Test "GET /actuator/health -> status UP" { ($overall.Body | ConvertFrom-Json).status -eq "UP" }

$readiness = Invoke-Health "/actuator/health/readiness"
Assert-Test "GET /actuator/health/readiness -> 200 (includes Postgres connection)" { $readiness.Status -eq 200 }

$liveness = Invoke-Health "/actuator/health/liveness"
Assert-Test "GET /actuator/health/liveness -> 200" { $liveness.Status -eq 200 }

Write-Host "`n=== Result: $script:pass PASS / $script:fail FAIL ===" -ForegroundColor $(if ($script:fail -eq 0) { "Green" } else { "Red" })
if ($script:fail -gt 0) { exit 1 }
