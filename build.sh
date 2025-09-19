#!/bin/bash
set -euo pipefail

# Clean previous build staging
rm -rf build/windows-installer
mkdir -p build/windows-installer/lib
mkdir -p build/windows-installer/models

# Build the application JAR
./gradlew -q jar

# Stage files
cp build/libs/MisterWhisper.jar build/windows-installer/MisterWhisper.jar
cp -v lib/*.jar build/windows-installer/lib/ 2>/dev/null || true
[ -f LICENSE ] && cp -v LICENSE build/windows-installer/ || true

# Copy NSIS script
if [ -f build/windows/installer.nsi ]; then
  cp build/windows/installer.nsi build/windows-installer/installer.nsi
else
  echo "ERROR: NSIS script build/windows/installer.nsi not found."
  exit 1
fi

# Optionally copy a bundled JRE if present at ./jre
if [ -d jre ]; then
  echo "Copying bundled JRE into staging..."
  mkdir -p build/windows-installer/jre
  cp -R jre/* build/windows-installer/jre/
else
  echo "No ./jre directory found. Installer will rely on system Java unless a JRE is added."
fi

# Create the Windows launcher (prefers bundled JRE if present)
cat > build/windows-installer/run.bat << 'EOL'
@echo off
setlocal ENABLEDELAYEDEXPANSION
set "SCRIPT_DIR=%~dp0"
set "JRE_DIR=!SCRIPT_DIR!jre"

if exist "!JRE_DIR!\bin\java.exe" (
  set "JAVA_EXE=!JRE_DIR!\bin\java.exe"
) else (
  where java >nul 2>&1
  if %ERRORLEVEL% EQU 0 (
    for /f "delims=" %%i in ('where java') do (
      set "JAVA_EXE=%%i"
      goto have_java
    )
  ) else (
    echo Java runtime not found. Install Java 8+ or bundle a JRE in a "jre" folder next to this script.
    pause
    exit /b 1
  )
)
:have_java
set "CLASSPATH=!SCRIPT_DIR!MisterWhisper.jar;!SCRIPT_DIR!lib\jna.jar;!SCRIPT_DIR!lib\win32-x86-64.jar;!SCRIPT_DIR!lib\jnativehook-2.2.2.jar"
"!JAVA_EXE!" -cp "!CLASSPATH!" whisper.MisterWhisper %*
endlocal
EOL

# Check makensis presence
if ! command -v makensis >/dev/null 2>&1; then
  echo "ERROR: makensis not found. Install NSIS (makensis) to build the installer."
  echo "On Windows: install NSIS. On Linux: sudo apt-get install nsis"
  exit 1
fi

# Build the installer
( cd build/windows-installer && makensis installer.nsi )

echo "Installer created at build/windows-installer/MisterWhisper_Installer.exe"