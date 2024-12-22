# MisterWhisper
 

MisterWhisper is an open-source application designed to simplify your workflow by transforming spoken words into text in real-time. When you press a designated key (F8 or F9), the application records your voice, transcribes it using the powerful Whisper AI model (GPU-accelerated for fast and efficient recognition), and directly inputs the resulting text into the currently active software.

MisterWhisper supports over 100 languages, making it a robust multilingual transcription tool.

![MisterWhisper](https://raw.githubusercontent.com/openconcerto/MisterWhisper/refs/heads/main/tray.png)


# Features

- Quick voice transcription: Record and transcribe speech only while the designated key is pressed, like a walkie-talkie.

- Integration with active software: Automatically inputs transcribed text into the application you are currently using.

- GPU acceleration (optional): Powered by [whisper.cpp](https://github.com/ggerganov/whisper.cpp) for fast and accurate voice recognition.

- Local or remote : You can use WhisperCPP locally or connect to a remote service for transcription.

# Installation

- extract the provided zip file or compile your own version of MisterWhisper
- download a Whisper model and copy it in the *models* folder from : https://huggingface.co/ggerganov/whisper.cpp/tree/main

# Usage
Just launch the *MisterWhisper.exe* (or MisterWhisper.jar).

MisterWhisper requires a Java runtime to be installed (version 8 or newer)

Keep F8 pressed while talking, the text will be inserted into the currently active software after key release.

To change or access settings, right click on the tray icon.

# Advanced Usage
If you want to use a remote server, launch the *whisper.cpp* server on the remote machine, for example (the server ip is 192.168.1.100) :

`` 
server.exe -l auto --port 9595 -t 8 -m "models/ggml-large-v3-turbo-q8_0.bin"
``

On the local machine, add the remote url as first parameter : 

`` 
MisterWhisper.exe "http://192.168.1.100:9595/inference"
``

# Acknowledgements

Georgi Gerganov : For its state-of-the-art, efficient [whisper.cpp](https://github.com/ggerganov/whisper.cpp. Priving that we don't need tons of poor Python software for 

OpenAI : For the open-source [Whisper](https://github.com/openai/whisper) project.

