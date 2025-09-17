# MisterWhisper: Copilot Coding Agent Instructions

## Project Overview
MisterWhisper is a Java desktop application for real-time voice-to-text transcription, integrating with active software and supporting both local and remote Whisper AI backends. It is cross-platform (Windows, Linux, macOS) and leverages [whisper.cpp](https://github.com/ggerganov/whisper.cpp) for GPU-accelerated recognition.

## Architecture & Key Components
- **Entry Point:** `whisper/MisterWhisper.java` (main application logic/UI)
- **Transcription Logic:**
  - Local: `whisper/LocalWhisperCPP.java` (JNI/JNA integration with whisper.cpp)
  - Remote: `whisper/RemoteWhisperCPP.java` (HTTP client for remote whisper.cpp server)
- **Model & State:** `io/github/ggerganov/whispercpp/model/WhisperModel.java`, `WhisperState.java`
- **Parameters:** `io/github/ggerganov/whispercpp/params/WhisperFullParams.java` and related param classes
- **Callbacks:** `io/github/ggerganov/whispercpp/callbacks/` (event hooks for transcription progress, segment creation, etc.)
- **JNI/JNA Bindings:** `WhisperCppJnaLibrary.java`, `jna.jar`, `jnativehook-2.2.2.jar`
- **JSON Utilities:** `org/json/` (custom JSON implementation)

## Developer Workflows
- **Build:** Standard Java build (no build scripts found; use `javac` or IDE)
- **Run (Local):**
  - Compile whisper.cpp as a shared library (not included for Linux/macOS)
  - Start whisper.cpp server: `whisper-server --no-timestamps -l auto --port 9595 -t 8 -m "models/ggml-large-v3-turbo-q8_0.bin"`
  - Run app: `java -jar MisterWhisper.jar "http://127.0.0.1:9595/inference"`
- **Run (Windows):** Launch `MisterWhisper.exe` (precompiled binary)
- **Remote Mode:** Point app to remote whisper.cpp server URL

## Project-Specific Patterns & Conventions
- **Hotkey-driven recording:** F9 key (configurable) triggers recording/transcription
- **Transcription is event-driven:** Uses silence detection and segment callbacks
- **Model files:** Expected in `models/` directory; not bundled
- **Custom JSON:** Uses `org/json/` instead of external libraries
- **JNA/JNI:** Native integration for local whisper.cpp; fallback to HTTP for remote
- **No standard test suite or CI detected**

## Integration Points
- **whisper.cpp:** Local (JNI/JNA) or remote (HTTP)
- **Active application integration:** RobotTyper for simulating text input
- **Tray icon and UI:** PNG assets in `whisper/`, tray logic in main class

## Examples
- To add a new transcription parameter, extend `WhisperFullParams.java` and update usage in `LocalWhisperCPP.java` and/or `RemoteWhisperCPP.java`.
- To support a new callback, implement in `callbacks/` and register in context/model classes.

## Key Files/Directories
- `src/whisper/MisterWhisper.java` (main)
- `src/whisper/LocalWhisperCPP.java`, `RemoteWhisperCPP.java`
- `src/io/github/ggerganov/whispercpp/model/`
- `src/io/github/ggerganov/whispercpp/params/`
- `src/io/github/ggerganov/whispercpp/callbacks/`
- `src/org/json/`
- `lib/` (native jars)

---
For unclear build/run steps, see README.md or ask for clarification. If you discover undocumented conventions, update this file.
