# Run PingPin JVM Unit Tests via Gradle
param(
    [switch]$Help
)

if ($Help) {
    Write-Host "Usage: .\tests\unit\run_unit_tests.ps1"
    exit 0
}

$repoRoot = (Resolve-Path "$PSScriptRoot\..\..").Path
Push-Location $repoRoot

try {
    Write-Host "==========================================================" -ForegroundColor Cyan
    Write-Host " Running PingPin Logic & Unit Test Suite (Gradlew)" -ForegroundColor Cyan
    Write-Host "==========================================================" -ForegroundColor Cyan

    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    & ".\gradlew.bat" testDebugUnitTest --console=plain
    $exitCode = $LASTEXITCODE
    $sw.Stop()

    $reportPath = "$repoRoot\app\build\reports\tests\testDebugUnitTest\index.html"
    if ($exitCode -eq 0) {
        Write-Host "`n[PASS] All JVM Unit Tests passed in $($sw.Elapsed.TotalSeconds.ToString('0.00'))s" -ForegroundColor Green
        Write-Host "HTML Report: file:///$($reportPath.Replace('\', '/'))" -ForegroundColor DarkGray
    } else {
        Write-Host "`n[FAIL] Unit tests failed with exit code $exitCode" -ForegroundColor Red
        Write-Host "Review Report: file:///$($reportPath.Replace('\', '/'))" -ForegroundColor Yellow
    }
    exit $exitCode
} finally {
    Pop-Location
}
