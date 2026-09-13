#!/bin/bash

# Load environment variables from .env file
if [ -f .env ]; then
    export $(grep -v '^#' .env | xargs)
    echo "✅ Loaded environment variables from .env"
else
    echo "⚠️  No .env file found!"
fi

# Run the Spring Boot application
echo "🚀 Starting Dynamic Surge Engine..."
./gradlew bootRun
