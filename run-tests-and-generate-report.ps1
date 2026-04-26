# SachITech Test Execution and Report Generation Script
# This script runs all JUnit tests and generates comprehensive reports

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  SachITech Test Report Generator" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Step 1: Clean previous test results
Write-Host "[1/4] Cleaning previous test results..." -ForegroundColor Yellow
mvn clean
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Clean failed!" -ForegroundColor Red
    exit 1
}
Write-Host "✅ Clean completed" -ForegroundColor Green
Write-Host ""

# Step 2: Run tests
Write-Host "[2/4] Running JUnit tests..." -ForegroundColor Yellow
mvn test
if ($LASTEXITCODE -ne 0) {
    Write-Host "⚠️  Some tests failed, but continuing to generate report..." -ForegroundColor Yellow
}
Write-Host "✅ Tests executed" -ForegroundColor Green
Write-Host ""

# Step 3: Generate Surefire HTML report
Write-Host "[3/4] Generating Surefire HTML report..." -ForegroundColor Yellow
mvn surefire-report:report
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Report generation failed!" -ForegroundColor Red
    exit 1
}
Write-Host "✅ Surefire report generated" -ForegroundColor Green
Write-Host ""

# Step 4: Generate JaCoCo coverage report
Write-Host "[4/4] Generating code coverage report..." -ForegroundColor Yellow
mvn jacoco:report
if ($LASTEXITCODE -ne 0) {
    Write-Host "⚠️  Coverage report generation failed" -ForegroundColor Yellow
} else {
    Write-Host "✅ Coverage report generated" -ForegroundColor Green
}
Write-Host ""

# Display report locations
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  📊 Reports Generated Successfully!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "📁 Report Locations:" -ForegroundColor White
Write-Host "   1. Surefire HTML Report:" -ForegroundColor Yellow
Write-Host "      target/site/surefire-report.html" -ForegroundColor White
Write-Host ""
Write-Host "   2. JaCoCo Coverage Report:" -ForegroundColor Yellow
Write-Host "      target/site/jacoco/index.html" -ForegroundColor White
Write-Host ""
Write-Host "   3. XML Test Results:" -ForegroundColor Yellow
Write-Host "      target/surefire-reports/*.xml" -ForegroundColor White
Write-Host ""

# Check if reports exist and offer to open them
$surefireReport = "target/site/surefire-report.html"
$jacocoReport = "target/site/jacoco/index.html"

if (Test-Path $surefireReport) {
    Write-Host "Would you like to open the Surefire report? (Y/N): " -ForegroundColor Cyan -NoNewline
    $response = Read-Host
    if ($response -eq "Y" -or $response -eq "y") {
        Start-Process $surefireReport
    }
}

if (Test-Path $jacocoReport) {
    Write-Host "Would you like to open the Coverage report? (Y/N): " -ForegroundColor Cyan -NoNewline
    $response = Read-Host
    if ($response -eq "Y" -or $response -eq "y") {
        Start-Process $jacocoReport
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  ✅ Test execution completed!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
