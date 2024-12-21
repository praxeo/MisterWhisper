package whisper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import io.github.ggerganov.whispercpp.WhisperCpp;
import io.github.ggerganov.whispercpp.params.CBool;
import io.github.ggerganov.whispercpp.params.WhisperFullParams;
import io.github.ggerganov.whispercpp.params.WhisperSamplingStrategy;

public class LocalWhisperCPP {
    private static WhisperCpp whisper = new WhisperCpp();

    public LocalWhisperCPP(File model) throws FileNotFoundException {
        whisper.initContext(model);
    }

    public String transcribe(File file) throws UnsupportedAudioFileException, IOException {
        String result = "";
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(file);

        byte[] b = new byte[audioInputStream.available()];
        float[] floats = new float[b.length / 2];

        WhisperFullParams params = whisper.getFullDefaultParams(WhisperSamplingStrategy.WHISPER_SAMPLING_BEAM_SEARCH);
        params.setProgressCallback((ctx, state, progress, user_data) -> System.out.println("progress: " + progress));
        params.print_progress = CBool.FALSE;
        params.language = "auto";

        params.n_threads = Runtime.getRuntime().availableProcessors();

        try {
            int r = audioInputStream.read(b);

            for (int i = 0, j = 0; i < r; i += 2, j++) {
                int intSample = (int) (b[i + 1]) << 8 | (int) (b[i]) & 0xFF;
                floats[j] = intSample / 32767.0f;
            }

            result = whisper.fullTranscribe(params, floats);

        } finally {
            audioInputStream.close();
        }
        return result;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("-1");
        LocalWhisperCPP w = new LocalWhisperCPP(new File("models", "ggml-large-v3-turbo-q8_0.bin"));
        System.out.println("-");
        w.transcribe(new File("jfk.wav"));
        long t1 = System.currentTimeMillis();
        w.transcribe(new File("jfk.wav"));
        long t2 = System.currentTimeMillis();
        System.out.println("LocalWhisperCPP.main() " + (t2 - t1) + " ms");
    }
}
