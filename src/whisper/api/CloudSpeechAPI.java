package whisper.api;

import java.io.File;
import java.io.IOException;

public interface CloudSpeechAPI {
    /**
     * Transcribe audio file using cloud API
     * @param audioFile Audio file to transcribe
     * @return Transcribed text
     * @throws IOException If transcription fails
     */
    String transcribe(File audioFile) throws IOException;

    /**
     * Transcribe audio data using cloud API
     * @param audioData Audio data to transcribe
     * @return Transcribed text
     * @throws IOException If transcription fails
     */
    String transcribe(byte[] audioData) throws IOException;

    /**
     * Get the name of the transcription service
     * @return Service name
     */
    String getServiceName();

    /**
     * Check if the API is properly configured
     * @return true if API is configured, false otherwise
     */
    boolean isConfigured();
}