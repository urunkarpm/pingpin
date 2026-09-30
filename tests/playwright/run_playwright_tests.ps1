# Run PingPin Component & Integration Tests via Playwright
param(
    [string]$Spec = "",
    [switch]$Snapshots,
    [switch]$Help
)

if ($Help) {
    Write-Host "Usage: .\tests\playwright\run_playwright_tests.ps1 [-Spec <spec-name>] [-Snapshots]"
    Write-Host "Examples:"
    Write-Host "  .\tests\playwright\run_playwright_tests.ps1"
    Write-Host "  .\tests\playwright\run_playwright_tests.ps1 -Snapshots"
    Write-Host "  .\tests\playwright\run_playwright_tests.ps1 -Spec pre-release-suite.spec.js"
    exit 0
}

$playwrightDir = (Resolve-Path "$PSScriptRoot\..\..\playwright_tests").Path
Push-Location $playwrightDir

try {
    Write-Host "==========================================================" -ForegroundColor Cyan
    Write-Host " Running PingPin Playwright Component & UI Suite" -ForegroundColor Cyan
    Write-Host "==========================================================" -ForegroundColor Cyan

    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    if ($Snapshots) {
        Write-Host "Rendering mobile UI snapshots..." -ForegroundColor Yellow
        node render_ui_snapshots.js
        $exitCode = $LASTEXITCODE
    } elseif ($Spec -ne "") {
        Write-Host "Running specific spec: $Spec" -ForegroundColor Yellow
        npx playwright test "tests/$Spec"
        $exitCode = $LASTEXITCODE
    } else {
        npm test
        $exitCode = $LASTEXITCODE
    }
    $sw.Stop()

    if ($exitCode -eq 0) {
        Write-Host "`n[PASS] Playwright suite passed in $($sw.Elapsed.TotalSeconds.ToString('0.00'))s" -ForegroundColor Green
    } else {
        Write-Host "`n[FAIL] Playwright suite failed with exit code $exitCode" -ForegroundColor Red
    }
    exit $exitCode
} finally {
    Pop-Location
}
