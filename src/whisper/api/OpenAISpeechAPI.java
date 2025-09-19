package whisper.api;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.prefs.Preferences;

import org.json.JSONObject;

public class OpenAISpeechAPI implements CloudSpeechAPI {
    private static final String API_URL_PREF = "openai.api.url";
    private static final String API_KEY_PREF = "openai.api.key";
    private static final String MODEL_PREF = "openai.model";
    private static final String DEFAULT_MODEL = "whisper-1";
    private static final String DEFAULT_API_URL = "https://api.openai.com/v1/audio/transcriptions";

    private final Preferences prefs;
    private final String apiKey;
    private final String model;
    private final String apiUrl;

    public OpenAISpeechAPI() {
        this.prefs = Preferences.userRoot().node("mister-whisper");
        this.apiKey = this.prefs.get(API_KEY_PREF, "");
        this.model = this.prefs.get(MODEL_PREF, DEFAULT_MODEL);
        this.apiUrl = this.prefs.get(API_URL_PREF, DEFAULT_API_URL);
    }

    @Override
    public String transcribe(File audioFile) throws IOException {
        if (!isConfigured()) {
            throw new IllegalStateException("OpenAI API key not configured");
        }

        String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();

        URL url = new URL(apiUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        connection.setRequestProperty("Authorization", "Bearer " + apiKey);

        try (var outputStream = connection.getOutputStream()) {
            // File part
            outputStream.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
            outputStream.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + audioFile.getName() + "\"\r\n").getBytes(StandardCharsets.UTF_8));
            outputStream.write(("Content-Type: audio/wav\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            java.nio.file.Files.copy(audioFile.toPath(), outputStream);
            outputStream.write(("\r\n").getBytes(StandardCharsets.UTF_8));

            // Model part
            outputStream.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
            outputStream.write(("Content-Disposition: form-data; name=\"model\"\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            outputStream.write(model.getBytes(StandardCharsets.UTF_8));
            outputStream.write(("\r\n").getBytes(StandardCharsets.UTF_8));

            // End of request
            outputStream.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = connection.getResponseCode();
        if (responseCode >= 400) {
            String response = new String(connection.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            throw new IOException("OpenAI API error: " + responseCode + " " + response);
        }

        String response = new String(connection.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        JSONObject jsonResponse = new JSONObject(response);

        return jsonResponse.optString("text", "");
    }

    @Override
    public String transcribe(byte[] audioData) throws IOException {
        File tempFile = File.createTempFile("openai_audio_", ".wav");
        java.nio.file.Files.write(tempFile.toPath(), audioData);
        try {
            return transcribe(tempFile);
        } finally {
            tempFile.delete();
        }
    }

    @Override
    public String getServiceName() {
        return "OpenAI";
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

    public void setApiUrl(String apiUrl) {
        this.prefs.put(API_URL_PREF, apiUrl);
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