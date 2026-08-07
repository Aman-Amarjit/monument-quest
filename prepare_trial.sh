#!/bin/bash

# Monument Quest - Trial Preparation Script
# This script helps bypass build blockers (Firebase/Mapbox) for a local trial.

echo "--- Monument Quest Trial Preparation ---"

# 1. Check for google-services.json
if [ ! -f "app/google-services.json" ]; then
    echo "[!] google-services.json not found."
    echo "    Temporarily disabling Firebase plugin to allow build..."
    sed -i 's/id("com.google.gms.google-services")/\/\/id("com.google.gms.google-services")/g' app/build.gradle.kts
else
    echo "[✓] google-services.json found."
fi

# 2. Check for Mapbox Token
if ! grep -q "sk\." ~/.gradle/gradle.properties 2>/dev/null; then
    echo "[!] Mapbox Secret Token (sk.xxx) not found in ~/.gradle/gradle.properties."
    echo "    Please add: MAPBOX_DOWNLOADS_TOKEN=your_secret_token"
fi

echo "----------------------------------------"
echo "You can now attempt to build the project in Android Studio."
echo "Note: If you disabled Firebase, social features will crash if accessed."
