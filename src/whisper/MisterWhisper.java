package whisper;

import java.awt.AWTException;
import java.awt.CheckboxMenuItem;
import java.awt.Desktop;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.Robot;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.Window.Type;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;

// "http://127.0.0.1:9595/inference";
// server.exe --port 9595 -t 24 -l fr -m "models/ggml-large-v3-turbo-q8_0.bin"

public class MisterWhisper implements NativeKeyListener {
    private AudioFormat audioFormat;
    private TargetDataLine targetDataLine;

    private Preferences prefs;
    private String hotkey;
    private LocalWhisperCPP w;
    private TrayIcon trayIcon;
    private Image imageRecording;
    private Image imageTranscribing;
    private Image imageInactive;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private boolean recording;
    private boolean transcribing;
    private String model;
    private String remoteUrl;

    public MisterWhisper(String remoteUrl) throws FileNotFoundException, NativeHookException {
        this.prefs = Preferences.userRoot().node("mister-whisper");
        this.hotkey = this.prefs.get("hotkey", "F8");
        this.model = this.prefs.get("model", "ggml-large-v3-turbo-q8_0.bin");

        GlobalScreen.registerNativeHook();
        GlobalScreen.addNativeKeyListener(this);

        // Create audio format
        float sampleRate = 16000.0F;
        int sampleSizeInBits = 16;
        int channels = 1;
        boolean signed = true;
        boolean bigEndian = false;
        this.audioFormat = new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);

        this.remoteUrl = remoteUrl;
        if (remoteUrl == null) {

            File dir = new File("models");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            boolean hasModels = false;
            for (File f : dir.listFiles()) {
                if (f.getName().endsWith(".bin")) {
                    hasModels = true;
                }
            }
            if (!hasModels) {
                JOptionPane.showMessageDialog(null,
                        "Please download a model (.bin file) from :\nhttps://huggingface.co/ggerganov/whisper.cpp/tree/main\n\n and copy it in :\n" + dir.getAbsolutePath());
                if (Desktop.isDesktopSupported()) {
                    final Desktop desktop = Desktop.getDesktop();
                    if (desktop.isSupported(Desktop.Action.BROWSE)) {
                        try {
                            desktop.browse(new URI("https://huggingface.co/ggerganov/whisper.cpp/tree/main"));
                        } catch (IOException | URISyntaxException e) {
                            e.printStackTrace();
                        }
                    }
                    if (desktop.isSupported(Desktop.Action.OPEN)) {
                        try {
                            desktop.open(dir);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                }
                System.exit(0);
            }

            if (!new File(dir, this.model).exists()) {
                for (File f : dir.listFiles()) {
                    if (f.getName().endsWith(".bin")) {
                        this.model = f.getName();
                        setModelPref(f.getName());
                        break;
                    }
                }
            }

            this.w = new LocalWhisperCPP(new File(dir, this.model));
            System.out.println("MisterWhisper using WhisperCPP with " + this.model);
        } else {
            System.out.println("MisterWhisper using remote speech to text service : " + remoteUrl);
        }
    }

    void createTrayIcon() {
        this.imageRecording = new ImageIcon(this.getClass().getResource("recording.png")).getImage();
        this.imageInactive = new ImageIcon(this.getClass().getResource("inactive.png")).getImage();
        this.imageTranscribing = new ImageIcon(this.getClass().getResource("transcribing.png")).getImage();

        this.trayIcon = new TrayIcon(this.imageInactive, "Press " + this.hotkey + " to record");
        this.trayIcon.setImageAutoSize(true);
        final SystemTray tray = SystemTray.getSystemTray();
        final Frame frame = new Frame("");
        frame.setUndecorated(true);
        frame.setType(Type.UTILITY);
        // Create a pop-up menu components
        final PopupMenu popup = createPopupMenu();
        this.trayIcon.setPopupMenu(popup);
        this.trayIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                stopRecording();
            }

            @Override
            public void mouseClicked(MouseEvent e) {

                if (e.isPopupTrigger()) {
                    frame.add(popup);
                    popup.show(frame, e.getXOnScreen(), e.getYOnScreen());

                }
            }

        });
        try {
            frame.setResizable(false);
            frame.setVisible(true);
            tray.add(this.trayIcon);
        } catch (AWTException e) {
            System.out.println("TrayIcon could not be added.\n" + e.getMessage());
        }

    }

    protected PopupMenu createPopupMenu() {
        final PopupMenu popup = new PopupMenu();

        CheckboxMenuItem autoPaste = new CheckboxMenuItem("Auto paste");
        autoPaste.setState(this.prefs.getBoolean("paste", true));
        autoPaste.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                MisterWhisper.this.prefs.putBoolean("paste", autoPaste.getState());
                try {
                    MisterWhisper.this.prefs.sync();
                } catch (BackingStoreException e1) {
                    e1.printStackTrace();
                    JOptionPane.showMessageDialog(null, "Cannot save preferences\n" + e1.getMessage());
                }
            }
        });

        Menu hotkeysMenu = new Menu("Trigger recording");

        CheckboxMenuItem f8Item = new CheckboxMenuItem("F8");

        CheckboxMenuItem f9Item = new CheckboxMenuItem("F9");
        if (this.hotkey.equals("F8")) {
            f8Item.setState(true);
        } else if (this.hotkey.equals("F9")) {
            f9Item.setState(true);
        }

        MenuItem exitItem = new MenuItem("Exit");
        // Add components to pop-up menu
        // popup.add(aboutItem);

        popup.add(autoPaste);
        if (this.remoteUrl == null) {
            Menu modelMenu = new Menu("Models");

            final File dir = new File("models");
            List<CheckboxMenuItem> allModels = new ArrayList<>();
            if (new File(dir, this.model).exists()) {
                for (File f : dir.listFiles()) {
                    final String name = f.getName();
                    if (name.endsWith(".bin")) {
                        final boolean selected = this.model.equals(name);
                        String cleanName = name.replace(".bin", "");
                        cleanName = cleanName.replace(".bin", "");
                        cleanName = cleanName.replace("ggml", "");
                        cleanName = cleanName.replace("-", " ");
                        cleanName = cleanName.trim();
                        final CheckboxMenuItem modelItem = new CheckboxMenuItem(cleanName);

                        modelItem.setState(selected);

                        modelItem.addItemListener(new ItemListener() {

                            @Override
                            public void itemStateChanged(ItemEvent e) {
                                System.out.println("actionPerformed()" + name + " : " + modelItem.getState());
                                if (modelItem.getState()) {
                                    // Deselected others
                                    for (CheckboxMenuItem item : allModels) {
                                        if (item != modelItem) {
                                            item.setState(false);
                                        }
                                    }
                                    // Apply model
                                    MisterWhisper.this.model = f.getName();
                                    setModelPref(MisterWhisper.this.model);
                                    try {
                                        MisterWhisper.this.w = new LocalWhisperCPP(f);
                                    } catch (FileNotFoundException e1) {
                                        JOptionPane.showMessageDialog(null, e1.getMessage());
                                        e1.printStackTrace();
                                    }
                                }
                            }

                        });
                        allModels.add(modelItem);
                        modelMenu.add(modelItem);
                    }
                }
            }

            popup.add(modelMenu);
        }
        popup.add(hotkeysMenu);
        hotkeysMenu.add(f8Item);
        hotkeysMenu.add(f9Item);
        popup.addSeparator();
        popup.add(exitItem);
        exitItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);

            }
        });

        return popup;
    }

    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        if ((this.hotkey.equals("F8") && e.getKeyCode() == NativeKeyEvent.VC_F8) || e.getKeyCode() == NativeKeyEvent.VC_F9) {
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    boolean paste = MisterWhisper.this.prefs.getBoolean("paste", true);
                    startRecording(paste);
                }
            });
        }
    }

    @Override
    public void nativeKeyReleased(NativeKeyEvent e) {
        if ((this.hotkey.equals("F8") && e.getKeyCode() == NativeKeyEvent.VC_F8) || e.getKeyCode() == NativeKeyEvent.VC_F9) {
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    stopRecording();
                }
            });
        }
    }

    @Override
    public void nativeKeyTyped(NativeKeyEvent e) {
        // Not used but required by the interface
    }

    private void startRecording(boolean paste) {
        if (isRecording()) {
            // Prevent multiple recordings
            return;
        }

        setRecording(true);
        try {
            // Get and open the target data line
            DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, this.audioFormat);

            if (!AudioSystem.isLineSupported(dataLineInfo)) {
                JOptionPane.showMessageDialog(null, "Audio line not supported");
                return;
            }

            this.targetDataLine = (TargetDataLine) AudioSystem.getLine(dataLineInfo);
            this.targetDataLine.open(this.audioFormat);
            this.targetDataLine.start();

            // Create a thread to capture the audio data
            final Thread captureThread = new Thread(() -> {
                final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

                setRecording(true);
                try {
                    byte[] data = new byte[4096];

                    while (this.isRecording()) {
                        int numBytesRead = this.targetDataLine.read(data, 0, data.length);
                        if (numBytesRead > 0) {
                            byteArrayOutputStream.write(data, 0, numBytesRead);
                        }
                    }
                    setRecording(false);
                    // Save the recorded audio to a WAV file
                    byte[] audioData = byteArrayOutputStream.toByteArray();
                    String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                    String fileName = timestamp + ".wav";

                    try (AudioInputStream audioInputStream = new AudioInputStream(new ByteArrayInputStream(audioData), this.audioFormat, audioData.length / this.audioFormat.getFrameSize())) {

                        final File out = File.createTempFile("rec_", fileName);
                        AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, out);
                        this.executorService.execute(new Runnable() {

                            @Override
                            public void run() {
                                setTranscribing(true);
                                try {
                                    if (MisterWhisper.this.remoteUrl == null) {
                                        process(out, paste);
                                    } else {
                                        processRemote(out, paste);
                                    }
                                } catch (Exception e) {
                                    JOptionPane.showMessageDialog(null, "Error processing record : " + e.getMessage());
                                    e.printStackTrace();
                                }
                                setTranscribing(false);
                            }
                        });

                    } catch (Exception e) {
                        e.printStackTrace();
                        JOptionPane.showMessageDialog(null, "Error : " + e.getMessage());
                        setTranscribing(false);
                    }
                } catch (Exception e) {
                    e.printStackTrace();

                }
                setRecording(false);
                setTranscribing(false);
            });

            captureThread.start();

        } catch (LineUnavailableException e) {
            setRecording(false);
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error starting recording: " + e.getMessage());
        }
    }

    protected synchronized void setTranscribing(boolean b) {
        this.transcribing = b;
        updateIcon();
    }

    public synchronized boolean isTranscribing() {
        return this.transcribing;
    }

    public synchronized boolean isRecording() {
        return this.recording;
    }

    public synchronized void setRecording(boolean b) {
        this.recording = b;
        updateIcon();
    }

    private void updateIcon() {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                if (isRecording()) {
                    MisterWhisper.this.trayIcon.setImage(MisterWhisper.this.imageRecording);
                } else {
                    if (isTranscribing()) {
                        MisterWhisper.this.trayIcon.setImage(MisterWhisper.this.imageTranscribing);
                    } else {
                        MisterWhisper.this.trayIcon.setImage(MisterWhisper.this.imageInactive);
                    }

                }

            }
        });

    }

    private void process(File out, boolean paste) throws IOException, AWTException, UnsupportedAudioFileException {
        long t1 = System.currentTimeMillis();

        String response = this.w.transcribe(out);
        System.out.println("Response: " + response);
        long t2 = System.currentTimeMillis();
        System.out.println("Process time  " + (t2 - t1) + " ms");
        String string = response.trim();
        if (!string.isEmpty()) {
            StringSelection stringSelection = new StringSelection(string + "\n");

            // Accède au presse-papier du système
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

            // Copie le texte dans le presse-papier
            clipboard.setContents(stringSelection, null);
            if (paste) {
                Robot robot = new Robot();
                robot.keyPress(KeyEvent.VK_CONTROL);
                robot.keyPress(KeyEvent.VK_V);
                robot.keyRelease(KeyEvent.VK_V);
                robot.keyRelease(KeyEvent.VK_CONTROL);
            }

        }
        out.delete();
    }

    private void processRemote(File out, boolean paste) throws IOException, AWTException {
        long t1 = System.currentTimeMillis();

        String string = new RemoteWhisperCPP(this.remoteUrl).transcribe(out, 0.0, 0.01);

        System.out.println("Response: " + string);
        long t2 = System.currentTimeMillis();
        System.out.println("Response  " + (t2 - t1) + " ms");

        if (!string.isEmpty()) {

            StringSelection stringSelection = new StringSelection(string + "\n");

            // Accède au presse-papier du système
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

            // Copie le texte dans le presse-papier
            clipboard.setContents(stringSelection, null);
            if (paste) {
                Robot robot = new Robot();
                robot.keyPress(KeyEvent.VK_CONTROL);
                robot.keyPress(KeyEvent.VK_V);
                robot.keyRelease(KeyEvent.VK_V);
                robot.keyRelease(KeyEvent.VK_CONTROL);
            }

        }
        out.delete();
    }

    private void stopRecording() {
        if (!this.isRecording()) {
            return;
        }

        setRecording(false);
        this.targetDataLine.stop();
        this.targetDataLine.close();
    }

    public void setModelPref(String name) {

        this.prefs.put("model", name);
        try {
            this.prefs.flush();
        } catch (BackingStoreException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Cannot save preferences");
        }
    }

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
                e.printStackTrace();
            }
            try {
                String url = null;
                if (args.length == 1) {
                    url = args[0];
                }
                MisterWhisper r = new MisterWhisper(url);
                r.createTrayIcon();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Error :\n" + e.getMessage());
                e.printStackTrace();
            }

        });
    }
}
