#!/bin/bash

# SachITech Test Report Generator
# This script runs JUnit tests and generates a detailed HTML report

echo "=========================================="
echo "  SachITech Testing Report Generator"
echo "=========================================="
echo ""

# Clean previous test results
echo "Cleaning previous test results..."
./mvnw clean

# Run tests with detailed reporting
echo "Running JUnit tests..."
./mvnw test -Dtest=AuthControllerTest,AdminControllerTest,FeeServiceTest

# Generate Surefire report
echo "Generating test reports..."
./mvnw surefire-report:report

echo ""
echo "=========================================="
echo "Test execution completed!"
echo "Report location: target/site/surefire-report.html"
echo "=========================================="
