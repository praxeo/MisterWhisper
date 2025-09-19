# MisterWhisper — Windows Portable Installer Guide

This document explains how to build a fully portable Windows installer (NSIS) for MisterWhisper that can run offline by bundling a private JRE.

Summary of what the build does:
- Stages app files into build/windows-installer/
- Optionally includes a private JRE if present at ./jre/
- Generates MisterWhisper_Installer.exe with NSIS
- Installs to %LOCALAPPDATA%\MisterWhisper and creates shortcuts
- Launcher prefers the bundled JRE; falls back to system Java

Prerequisites
- Java Development Kit (JDK) 8+ (JDK 17 LTS recommended)
- NSIS (makensis) CLI available in PATH
  - Windows: Install NSIS from https://nsis.sourceforge.io/Download
  - Linux: Install nsis package (e.g., sudo apt-get install nsis)

Building the Installer (without a bundled JRE)
1) From the repository root:
   ./gradlew buildInstaller

2) On success, the installer is created at:
   build/windows-installer/MisterWhisper_Installer.exe

3) The installer writes to:
   %LOCALAPPDATA%\MisterWhisper
   and creates Start menu and Desktop shortcuts.

Bundling a Private JRE (fully offline install)
The build will copy a local ./jre directory into the installer if present. This allows the app to run without requiring a system Java installation.

Option A: Download a prebuilt JRE (recommended)
- Use an OpenJDK distribution that permits redistribution (e.g., Temurin from Adoptium).
- Download “JRE” Zip for Windows x64 (e.g., Temurin 17 LTS JRE).
- Extract the Zip into a folder named jre at the repository root so that:
  ./jre/bin/java.exe
  ./jre/lib/...

- Rebuild the installer:
  ./gradlew buildInstaller

- The resulting installer will embed the JRE and the launcher will use it by default.

Option B: Create a minimized JRE with jlink (advanced, size-optimized)
- Requires JDK 17+ to run jlink.
- MisterWhisper uses Swing/AWT, Sound, Preferences, Logging, etc.
- A minimal set of modules that works well:
  java.base, java.desktop, java.logging, java.prefs

- Create the runtime image into ./jre:
  jlink \
    --add-modules java.base,java.desktop,java.logging,java.prefs \
    --output jre \
    --strip-debug \
    --no-man-pages \
    --no-header-files \
    --compress=2

- Verify jre/bin/java.exe was produced, then build the installer:
  ./gradlew buildInstaller

What the Installer Contains
- MisterWhisper.jar
- run.bat (launcher)
- lib\*.jar (JNI, native libs)
- Optional jre\... if you provided ./jre
- models\ (created empty; user can add whisper models)

Launcher Behavior (run.bat)
- Prefers jre\bin\java.exe (bundled JRE if present)
- Falls back to java on PATH if no bundled JRE

Fully Offline Usage
To ensure a completely offline install:
- Bundle the private JRE as described above.
- Add your Whisper model(s) into models\ before shipping the installer, or instruct users to download and place .bin files in %LOCALAPPDATA%\MisterWhisper\models.

Verifying the Build
1) Clean previous staging:
   rm -rf build/windows-installer

2) Build:
   ./gradlew buildInstaller

3) Run the resulting EXE on a clean Windows VM or machine without Java installed to verify the bundled JRE path.

Notes
- License: Ensure your chosen JRE distribution’s license allows redistribution.
- NSIS warning “File: 'jre\*.*' -> no files found” simply indicates no ./jre was present; it’s safe to ignore if you intend to rely on system Java.
- Installer installs per-user in %LOCALAPPDATA% and does not require admin rights.

Artifacts & Paths
- Installer EXE:
  build/windows-installer/MisterWhisper_Installer.exe
- Staged content before NSIS:
  build/windows-installer/
- Private JRE to bundle (place here in repo root):
  ./jre/