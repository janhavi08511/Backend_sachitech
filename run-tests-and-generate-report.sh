#!/bin/bash

# SachITech Test Execution and Report Generation Script
# This script runs all JUnit tests and generates comprehensive reports

echo "========================================"
echo "  SachITech Test Report Generator"
echo "========================================"
echo ""

# Step 1: Clean previous test results
echo "[1/4] Cleaning previous test results..."
mvn clean
if [ $? -ne 0 ]; then
    echo "❌ Clean failed!"
    exit 1
fi
echo "✅ Clean completed"
echo ""

# Step 2: Run tests
echo "[2/4] Running JUnit tests..."
mvn test
if [ $? -ne 0 ]; then
    echo "⚠️  Some tests failed, but continuing to generate report..."
fi
echo "✅ Tests executed"
echo ""

# Step 3: Generate Surefire HTML report
echo "[3/4] Generating Surefire HTML report..."
mvn surefire-report:report
if [ $? -ne 0 ]; then
    echo "❌ Report generation failed!"
    exit 1
fi
echo "✅ Surefire report generated"
echo ""

# Step 4: Generate JaCoCo coverage report
echo "[4/4] Generating code coverage report..."
mvn jacoco:report
if [ $? -ne 0 ]; then
    echo "⚠️  Coverage report generation failed"
else
    echo "✅ Coverage report generated"
fi
echo ""

# Display report locations
echo "========================================"
echo "  📊 Reports Generated Successfully!"
echo "========================================"
echo ""
echo "📁 Report Locations:"
echo "   1. Surefire HTML Report:"
echo "      target/site/surefire-report.html"
echo ""
echo "   2. JaCoCo Coverage Report:"
echo "      target/site/jacoco/index.html"
echo ""
echo "   3. XML Test Results:"
echo "      target/surefire-reports/*.xml"
echo ""

# Check if reports exist and offer to open them
SUREFIRE_REPORT="target/site/surefire-report.html"
JACOCO_REPORT="target/site/jacoco/index.html"

if [ -f "$SUREFIRE_REPORT" ]; then
    echo "Would you like to open the Surefire report? (y/n): "
    read -r response
    if [ "$response" = "y" ] || [ "$response" = "Y" ]; then
        if [[ "$OSTYPE" == "darwin"* ]]; then
            open "$SUREFIRE_REPORT"
        elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
            xdg-open "$SUREFIRE_REPORT"
        fi
    fi
fi

if [ -f "$JACOCO_REPORT" ]; then
    echo "Would you like to open the Coverage report? (y/n): "
    read -r response
    if [ "$response" = "y" ] || [ "$response" = "Y" ]; then
        if [[ "$OSTYPE" == "darwin"* ]]; then
            open "$JACOCO_REPORT"
        elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
            xdg-open "$JACOCO_REPORT"
        fi
    fi
fi

echo ""
echo "========================================"
echo "  ✅ Test execution completed!"
echo "========================================"
