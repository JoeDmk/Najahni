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
import java.util.Scanner;

public class AudioTranscriber {

    public static void recordAndTranscribe(TextArea textArea, Button micButton, int recordSeconds) {
        micButton.setDisable(true);
        micButton.setText("🔴 Recording...");

        new Thread(() -> {
            try {
                // Whisper likes standard sample rates, standard PCM wave does fine.
                AudioFormat format = new AudioFormat(16000, 16, 1, true, true);
                DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

                if (!AudioSystem.isLineSupported(info)) {
                    Platform.runLater(() -> {
                        textArea.appendText("\n[Microphone not supported]");
                        resetButton(micButton, recordSeconds);
                    });
                    return;
                }

                TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
                line.open(format);
                line.start();

                File wavFile = new File("temp_record.wav");
                AudioInputStream ais = new AudioInputStream(line);

                // Start recording in a separate thread so we can close line on timer
                Thread recorderThread = new Thread(() -> {
                    try {
                        AudioSystem.write(ais, AudioFileFormat.Type.WAVE, wavFile);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
                recorderThread.start();

                Thread.sleep(recordSeconds * 1000L); // configurable recording time
                line.stop();
                line.close();

                Platform.runLater(() -> micButton.setText("⏳ Transcribing..."));

                // Upload to FastAPI
                String transcription = uploadAndTranscribe(wavFile);

                Platform.runLater(() -> {
                    String currentText = textArea.getText();
                    if (!currentText.isEmpty() && !currentText.endsWith("\n")) {
                        textArea.appendText("\n");
                    }
                    textArea.appendText(transcription);
                    resetButton(micButton, recordSeconds);
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    textArea.appendText("\n[Error recording: " + e.getMessage() + "]");
                    resetButton(micButton, recordSeconds);
                });
            }
        }).start();
    }

    private static void resetButton(Button micButton, int recordSeconds) {
        micButton.setText("🎤 Record (" + recordSeconds + "s)");
        micButton.setDisable(false);
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
