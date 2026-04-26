# SachITech Test Report Generator (PowerShell)
# This script runs JUnit tests and generates a detailed HTML report

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "  SachITech Testing Report Generator" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""

# Clean previous test results
Write-Host "Cleaning previous test results..." -ForegroundColor Yellow
./mvnw.cmd clean

# Run tests with detailed reporting
Write-Host "Running JUnit tests..." -ForegroundColor Yellow
./mvnw.cmd test

# Generate Surefire report
Write-Host "Generating HTML test reports..." -ForegroundColor Yellow
./mvnw.cmd surefire-report:report

# Generate JaCoCo coverage report
Write-Host "Generating code coverage report..." -ForegroundColor Yellow
./mvnw.cmd jacoco:report

Write-Host ""
Write-Host "==========================================" -ForegroundColor Green
Write-Host "Test execution completed!" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Green
Write-Host ""
Write-Host "Reports generated:" -ForegroundColor White
Write-Host "  - Test Report: target/site/surefire-report.html" -ForegroundColor Cyan
Write-Host "  - Coverage Report: target/site/jacoco/index.html" -ForegroundColor Cyan
Write-Host "  - Detailed Report: TEST_REPORT.md" -ForegroundColor Cyan
Write-Host ""

# Open reports in browser (optional)
$openReports = Read-Host "Open reports in browser? (Y/N)"
if ($openReports -eq "Y" -or $openReports -eq "y") {
    Start-Process "target/site/surefire-report.html"
    Start-Process "target/site/jacoco/index.html"
}
