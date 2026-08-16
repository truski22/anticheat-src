<#
.SYNOPSIS
  Batería de pruebas de comportamiento para el user-service migrado a Spring Boot.
  Habla JSON contra el @RestController y contra Actuator para el health check.

.PARAMETER ApiUrl
  Base URL del servidor Spring Boot (endpoints /users/... y /actuator/...).
  Por defecto http://localhost:8080

.EXAMPLE
  .\smoke-test.ps1
  .\smoke-test.ps1 -ApiUrl http://localhost:8080
#>
param(
    [string]$ApiUrl = "http://localhost:8080"
)

$ErrorActionPreference = "Stop"
$script:pass = 0
$script:fail = 0

function ConvertTo-TextContent {
    param($Content)
    if ($Content -is [byte[]]) {
        return [System.Text.Encoding]::UTF8.GetString($Content)
    }
    return $Content
}

function Invoke-Api {
    param(
        [string]$Method,
        [string]$Url,
        [hashtable]$RequestBody = $null
    )
    try {
        if ($RequestBody) {
            $json = $RequestBody | ConvertTo-Json -Compress
            $resp = Invoke-WebRequest -Uri $Url -Method $Method -Body $json `
                -ContentType "application/json" -UseBasicParsing
        } else {
            $resp = Invoke-WebRequest -Uri $Url -Method $Method -UseBasicParsing
        }
        return [pscustomobject]@{ Status = [int]$resp.StatusCode; Body = ConvertTo-TextContent $resp.Content }
    } catch [System.Net.WebException] {
        $webResponse = $_.Exception.Response
        if (-not $webResponse) { throw }
        $status = [int]$webResponse.StatusCode
        $stream = $webResponse.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($stream)
        $responseText = $reader.ReadToEnd()
        return [pscustomobject]@{ Status = $status; Body = $responseText }
    }
}

function Assert-Test {
    param(
        [string]$Name,
        [scriptblock]$Check
    )
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

Write-Host "=== user-service smoke test ===" -ForegroundColor Cyan
Write-Host "API: $ApiUrl`n"

# --- health (Actuator) ---
$health = Invoke-Api GET "$ApiUrl/actuator/health"
Assert-Test "GET /actuator/health -> 200" { $health.Status -eq 200 }
Assert-Test "GET /actuator/health -> status UP" { ($health.Body | ConvertFrom-Json).status -eq "UP" }

# --- unique test user per run, so it's safe to re-run without wiping the DB ---
$suffix = Get-Random -Maximum 999999
$user = "smoketest_$suffix"
$email = "smoketest_$suffix@example.com"
$password = "InitialPass123"
$newPassword = "ChangedPass456"

# --- register ---
$reg = Invoke-Api POST "$ApiUrl/users/register" @{ name = $user; email = $email; password = $password }
Assert-Test "register new user -> 200" { $reg.Status -eq 200 }
Assert-Test "register new user -> success true / OK" {
    $j = $reg.Body | ConvertFrom-Json
    $j.success -eq $true -and $j.message -eq "OK"
}

$regDupUser = Invoke-Api POST "$ApiUrl/users/register" @{ name = $user; email = "other_$suffix@example.com"; password = "x" }
Assert-Test "register duplicate username -> UNV" {
    ($regDupUser.Body | ConvertFrom-Json).message -eq "UNV"
}

$regDupEmail = Invoke-Api POST "$ApiUrl/users/register" @{ name = "other_$suffix"; email = $email; password = "x" }
Assert-Test "register duplicate email -> ENV" {
    ($regDupEmail.Body | ConvertFrom-Json).message -eq "ENV"
}

$regBlank = Invoke-Api POST "$ApiUrl/users/register" @{ name = ""; email = ""; password = "" }
Assert-Test "register blank fields -> 400" { $regBlank.Status -eq 400 }

# --- login ---
$loginOk = Invoke-Api POST "$ApiUrl/users/login" @{ name = $user; password = $password }
Assert-Test "login correct password -> success true / OK" {
    $j = $loginOk.Body | ConvertFrom-Json
    $j.success -eq $true -and $j.message -eq "OK"
}

$loginBad = Invoke-Api POST "$ApiUrl/users/login" @{ name = $user; password = "wrongpassword" }
Assert-Test "login wrong password -> success false / KO" {
    $j = $loginBad.Body | ConvertFrom-Json
    $j.success -eq $false -and $j.message -eq "KO"
}

$loginMissing = Invoke-Api POST "$ApiUrl/users/login" @{ name = $user }
Assert-Test "login missing password -> 400" { $loginMissing.Status -eq 400 }

# --- user info ---
$info = Invoke-Api GET "$ApiUrl/users/info?user=$user"
Assert-Test "get info -> 200" { $info.Status -eq 200 }
Assert-Test "get info -> matches registered data" {
    $j = $info.Body | ConvertFrom-Json
    $j.email -eq $email -and $j.totalGames -eq 0 -and $j.cheatGames -eq 0 -and $j.legalGames -eq 0
}

$infoMissing = Invoke-Api GET "$ApiUrl/users/info?user=does_not_exist_$suffix"
Assert-Test "get info for unknown user -> 404" { $infoMissing.Status -eq 404 }

# --- change password ---
$changePw = Invoke-Api POST "$ApiUrl/users/password" @{ name = $user; password = $newPassword }
Assert-Test "change password -> success true" {
    ($changePw.Body | ConvertFrom-Json).success -eq $true
}

$changePwSame = Invoke-Api POST "$ApiUrl/users/password" @{ name = $user; password = $newPassword }
Assert-Test "change password to same value again -> success false" {
    ($changePwSame.Body | ConvertFrom-Json).success -eq $false
}

$loginNewPw = Invoke-Api POST "$ApiUrl/users/login" @{ name = $user; password = $newPassword }
Assert-Test "login with new password -> success true" {
    ($loginNewPw.Body | ConvertFrom-Json).success -eq $true
}

$loginOldPw = Invoke-Api POST "$ApiUrl/users/login" @{ name = $user; password = $password }
Assert-Test "login with old password -> success false" {
    ($loginOldPw.Body | ConvertFrom-Json).success -eq $false
}

$changePwMissingUser = Invoke-Api POST "$ApiUrl/users/password" @{ name = "does_not_exist_$suffix"; password = "whatever" }
Assert-Test "change password for unknown user -> success false" {
    ($changePwMissingUser.Body | ConvertFrom-Json).success -eq $false
}

# --- change password by email ---
$changePwByEmail = Invoke-Api POST "$ApiUrl/users/password/by-email" @{ email = $email; password = $password }
Assert-Test "change password by email -> success true" {
    ($changePwByEmail.Body | ConvertFrom-Json).success -eq $true
}

# --- password reset email ---
$resetKnown = Invoke-Api POST "$ApiUrl/users/password/reset-email" @{ email = $email }
Assert-Test "reset email for known address -> 200" { $resetKnown.Status -eq 200 }
Assert-Test "reset email response has 'sent' field" {
    $null -ne ($resetKnown.Body | ConvertFrom-Json).sent
}

$resetUnknown = Invoke-Api POST "$ApiUrl/users/password/reset-email" @{ email = "nobody_$suffix@example.com" }
Assert-Test "reset email for unknown address -> sent false" {
    ($resetUnknown.Body | ConvertFrom-Json).sent -eq $false
}

# --- unknown route ---
$notFound = Invoke-Api GET "$ApiUrl/does/not/exist"
Assert-Test "unknown route -> 404" { $notFound.Status -eq 404 }

Write-Host "`n=== Resultado: $script:pass PASS / $script:fail FAIL ===" -ForegroundColor $(if ($script:fail -eq 0) { "Green" } else { "Red" })
if ($script:fail -gt 0) { exit 1 }
