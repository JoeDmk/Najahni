package tn.esprit.utils;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;

import javax.sound.sampled.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class AudioTranscriber {

    // Keep track of active recording sessions by button to allow toggling
    private static final Map<Button, RecordingSession> activeSessions = new HashMap<>();

    private static class RecordingSession {
        TargetDataLine line;
        Thread recorderThread;
        File wavFile;

        public RecordingSession(TargetDataLine line, Thread recorderThread, File wavFile) {
            this.line = line;
            this.recorderThread = recorderThread;
            this.wavFile = wavFile;
        }
    }

    public static void toggleRecording(TextArea textArea, Button micButton) {
        if (activeSessions.containsKey(micButton)) {
            // Stop recording
            stopRecordingAndTranscribe(textArea, micButton);
        } else {
            // Start recording
            startRecording(textArea, micButton);
        }
    }

    private static void startRecording(TextArea textArea, Button micButton) {
        micButton.setText("⏹ Stop");
        micButton.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand;");

        new Thread(() -> {
            try {
                // Whisper likes standard sample rates, standard PCM wave does fine.
                AudioFormat format = new AudioFormat(16000, 16, 1, true, true);
                DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

                if (!AudioSystem.isLineSupported(info)) {
                    Platform.runLater(() -> {
                        textArea.appendText("\n[Microphone not supported]");
                        resetButton(micButton);
                    });
                    return;
                }

                TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
                line.open(format);
                line.start();

                File wavFile = new File("temp_record_" + System.currentTimeMillis() + ".wav");
                AudioInputStream ais = new AudioInputStream(line);

                Thread recorderThread = new Thread(() -> {
                    try {
                        AudioSystem.write(ais, AudioFileFormat.Type.WAVE, wavFile);
                    } catch (Exception ex) {
                        // Exception will be thrown when stream is closed intentionally
                    }
                });
                recorderThread.start();

                // Store session
                activeSessions.put(micButton, new RecordingSession(line, recorderThread, wavFile));

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    textArea.appendText("\n[Error recording: " + e.getMessage() + "]");
                    resetButton(micButton);
                });
            }
        }).start();
    }

    private static void stopRecordingAndTranscribe(TextArea textArea, Button micButton) {
        RecordingSession session = activeSessions.remove(micButton);
        if (session == null) return;

        micButton.setDisable(true);
        micButton.setText("⏳ Transcribing...");
        micButton.setStyle("-fx-background-color: #f59e0b; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: wait;");

        new Thread(() -> {
            try {
                // Stop audio capture
                session.line.stop();
                session.line.close();
                
                // Wait briefly for file write to complete
                Thread.sleep(200);

                // Upload to FastAPI
                String transcription = uploadAndTranscribe(session.wavFile);

                Platform.runLater(() -> {
                    String currentText = textArea.getText();
                    if (!currentText.isEmpty() && !currentText.endsWith("\n")) {
                        textArea.appendText("\n");
                    }
                    textArea.appendText(transcription);
                    resetButton(micButton);
                });

                // Cleanup temp file
                if (session.wavFile.exists()) {
                    session.wavFile.delete();
                }

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    textArea.appendText("\n[Transcription error: " + e.getMessage() + "]");
                    resetButton(micButton);
                });
            }
        }).start();
    }

    private static void resetButton(Button micButton) {
        micButton.setText("🎤 Record");
        micButton.setDisable(false);
        micButton.setStyle("-fx-background-color: #ffffff; -fx-text-fill: #4f46e5; -fx-border-color: #4f46e5; -fx-border-radius: 8; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand;");
    }

    private static String uploadAndTranscribe(File file) {
        String boundary = "----WebKitFormBoundary7MAbmHpw9nAh30iM";
        String LINE_FEED = "\r\n";
        try {
            URL url = new URL("http://localhost:8001/transcribe");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(("--" + boundary + LINE_FEED).getBytes());
                outputStream.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\""
                        + LINE_FEED).getBytes());
                outputStream.write(("Content-Type: audio/wav" + LINE_FEED).getBytes());
                outputStream.write((LINE_FEED).getBytes());

                try (FileInputStream inputStream = new FileInputStream(file)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                }
                outputStream.write((LINE_FEED).getBytes());
                outputStream.write(("--" + boundary + "--" + LINE_FEED).getBytes());
                outputStream.flush();
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                try (Scanner scanner = new Scanner(connection.getInputStream(), "utf-8")) {
                    String response = scanner.useDelimiter("\\A").next();
                    int start = response.indexOf("\"text\":\"");
                    if (start != -1) {
                        start += 8;
                        int end = response.lastIndexOf("\"");
                        if (end > start) {
                            String answer = response.substring(start, end);
                            return answer.replace("\\n", "\n").replace("\\\"", "\"");
                        }
                    }
                    return response;
                }
            } else {
                return "[Error transcribing: HTTP " + responseCode + "]";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "[Error uploading file: " + e.getMessage() + "]";
        }
    }
}
