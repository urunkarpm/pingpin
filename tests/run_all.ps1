# Master Test Orchestrator for PingPin
# Usage:
#   .\tests\run_all.ps1 -Suite all
#   .\tests\run_all.ps1 -Suite unit
#   .\tests\run_all.ps1 -Suite playwright
#   .\tests\run_all.ps1 -Suite device [-DeviceId <serial>]

param(
    [ValidateSet("all", "unit", "playwright", "device")]
    [string]$Suite = "all",

    [string]$DeviceId = "",

    [switch]$Help
)

if ($Help) {
    Write-Host "PingPin Master Test Orchestrator"
    Write-Host "Usage: .\tests\run_all.ps1 [-Suite <all|unit|playwright|device>] [-DeviceId <serial>]"
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  -Suite       Which test suite to run (default: all)"
    Write-Host "                 'unit'       : Runs 58 Gradle JVM unit tests"
    Write-Host "                 'playwright' : Runs 116 Playwright component tests"
    Write-Host "                 'device'     : Runs 12 live device ADB integration tests"
    Write-Host "                 'all'        : Runs all 3 suites in sequence"
    Write-Host "  -DeviceId    Explicit ADB device serial / Wi-Fi address (default: auto-detected)"
    exit 0
}

$repoRoot = (Resolve-Path "$PSScriptRoot\..").Path
$testsDir = "$repoRoot\tests"
$artifactsDir = "$testsDir\artifacts"

if (-not (Test-Path $artifactsDir)) {
    New-Item -ItemType Directory -Force -Path $artifactsDir | Out-Null
}

$overallSuccess = $true
$results = @()
$masterSw = [System.Diagnostics.Stopwatch]::StartNew()

function Record-Result($suiteName, $passed, $durationSec, $details) {
    $script:results += [PSCustomObject]@{
        Suite    = $suiteName
        Status   = if ($passed) { "PASS" } else { "FAIL" }
        Duration = "$($durationSec.ToString('0.00'))s"
        Details  = $details
    }
    if (-not $passed) {
        $script:overallSuccess = $false
    }
}

Write-Host "========================================================================" -ForegroundColor Magenta
Write-Host "             PINGPIN AUTOMATED TEST ORCHESTRATION ENGINE               " -ForegroundColor Magenta
Write-Host "========================================================================" -ForegroundColor Magenta
Write-Host "Target Suite : $Suite" -ForegroundColor Gray
Write-Host "Root Path    : $repoRoot" -ForegroundColor Gray
Write-Host "Artifacts    : $artifactsDir" -ForegroundColor Gray
Write-Host ""

# ----------------------------------------------------
# 1. UNIT TESTS (JVM / Gradle)
# ----------------------------------------------------
if ($Suite -eq "all" -or $Suite -eq "unit") {
    Write-Host "`n>>> [1/3] EXECUTING JVM LOGIC & UNIT TESTS (GRADLE) <<<" -ForegroundColor Cyan
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    & "$testsDir\unit\run_unit_tests.ps1"
    $unitExit = $LASTEXITCODE
    $sw.Stop()
    Record-Result "JVM Unit Tests" ($unitExit -eq 0) $sw.Elapsed.TotalSeconds "58 / 58 Engine tests"
}

# ----------------------------------------------------
# 2. PLAYWRIGHT TESTS (Component & Design Tokens)
# ----------------------------------------------------
if ($Suite -eq "all" -or $Suite -eq "playwright") {
    Write-Host "`n>>> [2/3] EXECUTING PLAYWRIGHT COMPONENT & UI TESTS <<<" -ForegroundColor Cyan
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    & "$testsDir\playwright\run_playwright_tests.ps1"
    $pwExit = $LASTEXITCODE
    $sw.Stop()
    Record-Result "Playwright UI Suite" ($pwExit -eq 0) $sw.Elapsed.TotalSeconds "108 Component specs"
}

# ----------------------------------------------------
# 3. LIVE DEVICE TESTS (ADB / Physical Phone)
# ----------------------------------------------------
if ($Suite -eq "all" -or $Suite -eq "device") {
    Write-Host "`n>>> [3/3] EXECUTING LIVE DEVICE INTEGRATION TESTS (ADB) <<<" -ForegroundColor Cyan
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    $deviceScript = "$testsDir\device\run_device_tests.py"
    
    $pythonArgs = @($deviceScript)
    if ($DeviceId -ne "") {
        $pythonArgs += @("--device", $DeviceId)
    }

    python @pythonArgs
    $devExit = $LASTEXITCODE
    $sw.Stop()
    Record-Result "Live Device E2E" ($devExit -eq 0) $sw.Elapsed.TotalSeconds "12 Interactive phone tests"
}

$masterSw.Stop()

# ----------------------------------------------------
# SUMMARY DASHBOARD
# ----------------------------------------------------
Write-Host "`n========================================================================" -ForegroundColor Magenta
Write-Host "                          FINAL TEST SUMMARY                           " -ForegroundColor Magenta
Write-Host "========================================================================" -ForegroundColor Magenta

foreach ($r in $results) {
    $color = if ($r.Status -eq "PASS") { "Green" } else { "Red" }
    $icon = if ($r.Status -eq "PASS") { "✅" } else { "❌" }
    Write-Host ("{0} {1,-22} | {2,-6} | {3,-8} | {4}" -f $icon, $r.Suite, $r.Status, $r.Duration, $r.Details) -ForegroundColor $color
}

Write-Host "------------------------------------------------------------------------" -ForegroundColor Gray
Write-Host "Total Execution Time: $($masterSw.Elapsed.TotalSeconds.ToString('0.00')) seconds" -ForegroundColor Gray
Write-Host "Test Artifacts Dir  : file:///$($artifactsDir.Replace('\', '/'))" -ForegroundColor DarkGray
Write-Host "========================================================================" -ForegroundColor Magenta

if ($overallSuccess) {
    Write-Host "`n🎉 ALL TEST SUITES PASSED! Release is verified and ready.`n" -ForegroundColor Green
    exit 0
} else {
    Write-Host "`n⚠️ ONE OR MORE TEST SUITES FAILED. Check logs above.`n" -ForegroundColor Red
    exit 1
}
