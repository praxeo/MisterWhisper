package whisper.api;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.prefs.Preferences;

import org.json.JSONObject;

public class VoxtralTranscriptionService implements CloudSpeechAPI {
    private static final String API_URL = "https://api.mistral.ai/v1/audio/transcriptions";
    private static final String API_KEY_PREF = "voxtral.api.key";
    private static final String MODEL_PREF = "voxtral.model";
    private static final String DEFAULT_MODEL = "voxtral-mini-latest";

    private final Preferences prefs;
    private final String apiKey;
    private final String model;

    public VoxtralTranscriptionService() {
        this.prefs = Preferences.userRoot().node("mister-whisper");
        this.apiKey = this.prefs.get(API_KEY_PREF, "");
        this.model = this.prefs.get(MODEL_PREF, DEFAULT_MODEL);
    }

    @Override
    public String transcribe(File audioFile) throws IOException {
        if (!isConfigured()) {
            throw new IllegalStateException("Voxtral API key not configured");
        }

        // Create boundary for multipart form data
        String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();

        // Create connection
        URL url = new URL(API_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        connection.setRequestProperty("x-api-key", apiKey);

        // Build request body
        StringBuilder requestBody = new StringBuilder();
        requestBody.append("--").append(boundary).append("\r\n");
        requestBody.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(audioFile.getName()).append("\"\r\n");
        requestBody.append("Content-Type: audio/wav\r\n\r\n");

        // Read file content
        byte[] fileContent = java.nio.file.Files.readAllBytes(audioFile.toPath());

        // Add file content to request
        requestBody.append(new String(fileContent, StandardCharsets.ISO_8859_1));

        // Add model parameter
        requestBody.append("\r\n--").append(boundary).append("\r\n");
        requestBody.append("Content-Disposition: form-data; name=\"model\"\r\n\r\n");
        requestBody.append(model).append("\r\n");

        // End of request
        requestBody.append("--").append(boundary).append("--\r\n");

        // Send request
        byte[] requestBytes = requestBody.toString().getBytes(StandardCharsets.UTF_8);
        connection.getOutputStream().write(requestBytes);

        // Get response
        int responseCode = connection.getResponseCode();
        if (responseCode >= 400) {
            throw new IOException("Voxtral API error: " + responseCode);
        }

        String response = new String(connection.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        JSONObject jsonResponse = new JSONObject(response);

        return jsonResponse.optString("text", "");
    }

    @Override
    public String transcribe(byte[] audioData) throws IOException {
        // For byte array, we need to save to temp file first
        File tempFile = File.createTempFile("voxtral_audio_", ".wav");
        java.nio.file.Files.write(tempFile.toPath(), audioData);
        return transcribe(tempFile);
    }

    @Override
    public String getServiceName() {
        return "Voxtral";
    }

    @Override
    public boolean isConfigured() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }

    public void setApiKey(String apiKey) {
        this.prefs.put(API_KEY_PREF, apiKey);
        try {
            this.prefs.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setModel(String model) {
        this.prefs.put(MODEL_PREF, model);
        try {
            this.prefs.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}