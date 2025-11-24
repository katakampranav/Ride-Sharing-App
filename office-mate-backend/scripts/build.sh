#!/bin/bash

# Build script for Officemate application
# This script builds the application with various options

set -e

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Default values
SKIP_TESTS=false
CLEAN_BUILD=false
BUILD_PROFILE="dev"
VERSION="1.0.0"

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --skip-tests)
            SKIP_TESTS=true
            shift
            ;;
        --clean)
            CLEAN_BUILD=true
            shift
            ;;
        --profile)
            BUILD_PROFILE="$2"
            shift 2
            ;;
        --version)
            VERSION="$2"
            shift 2
            ;;
        --help)
            echo "Usage: $0 [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  --skip-tests    Skip running tests"
            echo "  --clean         Clean build artifacts before building"
            echo "  --profile       Build profile (dev, staging, prod)"
            echo "  --version       Application version"
            echo "  --help          Show this help message"
            exit 0
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            exit 1
            ;;
    esac
done

echo "=========================================="
echo "Officemate Build Script"
echo "=========================================="
echo ""
echo "Profile: $BUILD_PROFILE"
echo "Version: $VERSION"
echo "Skip Tests: $SKIP_TESTS"
echo "Clean Build: $CLEAN_BUILD"
echo ""

# Check if Gradle wrapper exists
if [ ! -f "./gradlew" ]; then
    echo -e "${RED}Error: Gradle wrapper not found${NC}"
    exit 1
fi

# Make gradlew executable
chmod +x ./gradlew

# Clean build if requested
if [ "$CLEAN_BUILD" = true ]; then
    echo "Cleaning build artifacts..."
    ./gradlew clean
    echo -e "${GREEN}✓ Clean complete${NC}"
    echo ""
fi

# Build command
BUILD_CMD="./gradlew build"

# Add skip tests flag if requested
if [ "$SKIP_TESTS" = true ]; then
    BUILD_CMD="$BUILD_CMD -x test"
fi

# Add profile
BUILD_CMD="$BUILD_CMD -Pprofile=$BUILD_PROFILE"

# Add version
BUILD_CMD="$BUILD_CMD -Pversion=$VERSION"

# Run build
echo "Building application..."
echo "Command: $BUILD_CMD"
echo ""

if $BUILD_CMD; then
    echo ""
    echo -e "${GREEN}✓ Build successful${NC}"
    
    # Display build artifacts
    echo ""
    echo "Build artifacts:"
    ls -lh build/libs/*.jar
    
    # Display JAR info
    echo ""
    echo "JAR details:"
    JAR_FILE=$(ls build/libs/*.jar | head -1)
    echo "  File: $JAR_FILE"
    echo "  Size: $(du -h $JAR_FILE | cut -f1)"
    
    # Extract version from JAR manifest
    if command -v unzip &> /dev/null; then
        echo "  Manifest:"
        unzip -p $JAR_FILE META-INF/MANIFEST.MF | grep -E "Implementation-Version|Implementation-Title" || true
    fi
    
else
    echo ""
    echo -e "${RED}✗ Build failed${NC}"
    exit 1
fi

echo ""
echo "=========================================="
echo -e "${GREEN}Build Complete!${NC}"
echo "=========================================="
echo ""
echo "Next steps:"
echo "  Run locally:  ./gradlew bootRun"
echo "  Run tests:    ./gradlew test"
echo "  Build Docker: docker build -t officemate-app ."
echo ""

