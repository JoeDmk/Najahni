package services;

import models.User;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_face.LBPHFaceRecognizer;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import org.bytedeco.opencv.opencv_videoio.VideoCapture;

import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.Java2DFrameConverter;

import javafx.embed.swing.SwingFXUtils;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import static org.bytedeco.opencv.global.opencv_imgproc.*;

/**
 * Face Recognition Service using OpenCV (JavaCV).
 *
 * Features:
 * - Face detection with Haar Cascade
 * - Face enrollment (capture + train LBPH model per user)
 * - Face recognition (match against all trained models)
 * - Webcam capture
 *
 * Face data is stored in: {user.home}/.najahni/faces/{userId}/
 */
public class FaceRecognitionService {

    private static FaceRecognitionService instance;

    /** Base directory for face data */
    private static final String FACE_DATA_DIR = System.getProperty("user.home")
            + File.separator + ".najahni" + File.separator + "faces";

    /** Number of face samples to capture during enrollment */
    public static final int ENROLLMENT_SAMPLES = 20;

    /** Path to the persisted global model file */
    private static final String GLOBAL_MODEL_PATH = FACE_DATA_DIR
            + File.separator + "global_model.yml";

    /** Confidence threshold: lower = stricter (LBPH returns distance, lower is better) */
    public static final double RECOGNITION_THRESHOLD = 65.0;

    /** Haar cascade for frontal face detection */
    private CascadeClassifier faceDetector;

    /** LBPH Face Recognizer */
    private LBPHFaceRecognizer recognizer;

    /** Whether the recognizer model has been trained */
    private boolean modelTrained = false;

    /**
     * Retained training data — prevents native memory deallocation that can
     * corrupt the LBPH model's internal histogram references in JavaCV.
     */
    private List<Mat> retainedFaceList;
    private MatVector retainedFaceMats;
    private Mat retainedLabels;

    /**
     * Frame converters are NOT thread-safe in JavaCV.
     * Each call to matToJavaFXImage creates local converter instances.
     */

    private FaceRecognitionService() {
        ensureFaceDataDirExists();
        initializeFaceDetector();
    }

    /**
     * Ensures the face data base directory exists.
     */
    private void ensureFaceDataDirExists() {
        File dir = new File(FACE_DATA_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
            System.out.println("Created face data directory: " + FACE_DATA_DIR);
        }
    }

    public static FaceRecognitionService getInstance() {
        if (instance == null) {
            instance = new FaceRecognitionService();
        }
        return instance;
    }

    // ==================== Initialization ====================

    private void initializeFaceDetector() {
        try {
            // Extract Haar cascade XML from OpenCV resources
            String cascadePath = extractCascadeFile();
            faceDetector = new CascadeClassifier(cascadePath);
            if (faceDetector.empty()) {
                System.err.println("ERROR: Could not load Haar cascade classifier!");
            } else {
                System.out.println("Face detector loaded successfully.");
            }
        } catch (Exception e) {
            System.err.println("Error initializing face detector: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Extracts or downloads the Haar cascade XML file.
     * Checks classpath first, then local cache, then downloads from OpenCV GitHub.
     */
    private String extractCascadeFile() throws Exception {
        String najahniDir = System.getProperty("user.home") + File.separator + ".najahni";
        File cascadeFile = new File(najahniDir, "haarcascade_frontalface_default.xml");

        // 1. If already cached locally, reuse it
        if (cascadeFile.exists() && cascadeFile.length() > 0) {
            System.out.println("Using cached Haar cascade: " + cascadeFile.getAbsolutePath());
            return cascadeFile.getAbsolutePath();
        }

        cascadeFile.getParentFile().mkdirs();

        // 2. Try classpath locations
        String[] resourcePaths = {
                "/haarcascade_frontalface_default.xml",
                "/org/bytedeco/opencv/haarcascades/haarcascade_frontalface_default.xml",
                "haarcascade_frontalface_default.xml",
                "/opencv/data/haarcascades/haarcascade_frontalface_default.xml"
        };
        for (String path : resourcePaths) {
            InputStream is = getClass().getResourceAsStream(path);
            if (is == null) {
                is = Thread.currentThread().getContextClassLoader().getResourceAsStream(path.startsWith("/") ? path.substring(1) : path);
            }
            if (is != null) {
                Files.copy(is, cascadeFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                is.close();
                System.out.println("Extracted Haar cascade from classpath: " + path);
                return cascadeFile.getAbsolutePath();
            }
        }

        // 3. Download from OpenCV GitHub repository
        String downloadUrl = "https://raw.githubusercontent.com/opencv/opencv/4.x/data/haarcascades/haarcascade_frontalface_default.xml";
        System.out.println("Downloading Haar cascade from OpenCV GitHub...");
        try {
            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.ALWAYS)
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(downloadUrl))
                    .GET()
                    .build();
            HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(cascadeFile.toPath()));

            if (response.statusCode() == 200 && cascadeFile.exists() && cascadeFile.length() > 1000) {
                System.out.println("Haar cascade downloaded successfully to: " + cascadeFile.getAbsolutePath());
                return cascadeFile.getAbsolutePath();
            } else {
                // Delete partial/bad download
                cascadeFile.delete();
                throw new RuntimeException("Download failed with status code: " + response.statusCode());
            }
        } catch (Exception e) {
            System.err.println("Failed to download Haar cascade: " + e.getMessage());
            throw new RuntimeException(
                    "Cannot find or download Haar cascade XML.\n" +
                    "Please download it manually from:\n" +
                    "  " + downloadUrl + "\n" +
                    "and place it in: " + najahniDir, e);
        }
    }

    // ==================== Webcam ====================

    /**
     * Opens the default webcam using DirectShow backend (Windows).
     * MSMF backend is buggy on many Windows laptops — CAP_DSHOW is more reliable.
     * Caller must release the camera when done.
     */
    public VideoCapture openWebcam() {
        // Use DirectShow backend (700 = CAP_DSHOW) to avoid MSMF grab errors on Windows
        VideoCapture camera = new VideoCapture(0, org.bytedeco.opencv.global.opencv_videoio.CAP_DSHOW);
        if (!camera.isOpened()) {
            System.err.println("DirectShow failed, trying default backend...");
            camera = new VideoCapture(0);
            if (!camera.isOpened()) {
                System.err.println("ERROR: Cannot open webcam with any backend!");
                return null;
            }
        }
        // Set resolution for better face detection
        camera.set(org.bytedeco.opencv.global.opencv_videoio.CAP_PROP_FRAME_WIDTH, 640);
        camera.set(org.bytedeco.opencv.global.opencv_videoio.CAP_PROP_FRAME_HEIGHT, 480);

        // Warm up: discard first frames (often black while camera initializes)
        System.out.println("Warming up webcam...");
        Mat warmup = new Mat();
        int warmupSuccess = 0;
        for (int i = 0; i < 40; i++) {
            if (camera.read(warmup) && !warmup.empty()) {
                warmupSuccess++;
            }
            try { Thread.sleep(80); } catch (InterruptedException ignored) {}
        }
        warmup.close();
        if (warmupSuccess == 0) {
            System.err.println("WARNING: Webcam opened but no frames captured during warm-up!");
        }
        System.out.println("Webcam ready (" + warmupSuccess + " warm-up frames).");
        return camera;
    }

    /**
     * Captures a single frame from the webcam.
     * Returns an empty Mat if the grab fails (caller should check with frame.empty()).
     * Uses grab() + retrieve() for more reliable frame capture on Windows.
     */
    public Mat captureFrame(VideoCapture camera) {
        Mat frame = new Mat();
        if (camera != null && camera.isOpened()) {
            // Try grab() + retrieve() first (more reliable on DSHOW)
            if (camera.grab()) {
                camera.retrieve(frame);
            }
            // Fallback to read() if retrieve returned empty
            if (frame.empty()) {
                camera.read(frame);
            }
        }
        return frame;
    }

    // ==================== Face Detection ====================

    /**
     * Detects faces in a frame. Returns rectangles around detected faces.
     */
    public RectVector detectFaces(Mat frame) {
        Mat gray = new Mat();
        cvtColor(frame, gray, COLOR_BGR2GRAY);
        equalizeHist(gray, gray);

        RectVector faces = new RectVector();
        if (faceDetector != null && !faceDetector.empty()) {
            faceDetector.detectMultiScale(
                    gray,
                    faces,
                    1.1,        // scaleFactor
                    6,          // minNeighbors (higher = fewer detections but more reliable)
                    0,          // flags
                    new Size(80, 80),   // minSize
                    new Size(400, 400)  // maxSize
            );
        }
        gray.close();
        return faces;
    }

    /**
     * Extracts a grayscale face ROI from a frame given a rectangle.
     * Resizes to a standard 200x200 for consistent recognition.
     */
    public Mat extractFaceROI(Mat frame, Rect faceRect) {
        Mat gray = new Mat();
        cvtColor(frame, gray, COLOR_BGR2GRAY);

        Mat faceROI = new Mat(gray, faceRect);
        Mat resized = new Mat();
        resize(faceROI, resized, new Size(200, 200));

        // Apply bilateral filter to reduce noise while keeping edges sharp
        Mat filtered = new Mat();
        opencv_imgproc.bilateralFilter(resized, filtered, 9, 75, 75);

        // Apply CLAHE (Contrast Limited Adaptive Histogram Equalization) for better contrast
        org.bytedeco.opencv.opencv_imgproc.CLAHE clahe = opencv_imgproc.createCLAHE(2.0, new Size(8, 8));
        Mat equalized = new Mat();
        clahe.apply(filtered, equalized);

        gray.close();
        faceROI.close();
        resized.close();
        filtered.close();
        clahe.close();
        return equalized;
    }

    /**
     * Draws rectangles around detected faces on the frame.
     * Returns the number of faces detected.
     */
    public int drawFaceRectangles(Mat frame, RectVector faces, Scalar color) {
        int count = (int) faces.size();
        for (int i = 0; i < count; i++) {
            Rect face = faces.get(i);
            opencv_imgproc.rectangle(frame, face, color, 3, LINE_8, 0);
        }
        return count;
    }

    // ==================== Enrollment (Registration) ====================

    /**
     * Saves a face sample image for a user during enrollment.
     * @param userId     The user's ID
     * @param sampleIndex The sample number (0 to ENROLLMENT_SAMPLES-1)
     * @param faceROI    The extracted 200x200 grayscale face image
     * @return true if saved successfully
     */
    public boolean saveFaceSample(int userId, int sampleIndex, Mat faceROI) {
        try {
            String userFaceDir = FACE_DATA_DIR + File.separator + userId;
            File dir = new File(userFaceDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String filePath = userFaceDir + File.separator + "face_" + sampleIndex + ".png";
            opencv_imgcodecs.imwrite(filePath, faceROI);
            System.out.println("Saved face sample: " + filePath);
            return true;
        } catch (Exception e) {
            System.err.println("Error saving face sample: " + e.getMessage());
            return false;
        }
    }

    /**
     * Trains the LBPH recognizer model for a specific user using their saved face samples.
     * Also saves the model file for later use.
     */
    public boolean trainUserModel(int userId) {
        try {
            String userFaceDir = FACE_DATA_DIR + File.separator + userId;
            File dir = new File(userFaceDir);
            if (!dir.exists() || dir.listFiles() == null) {
                System.err.println("No face data found for user " + userId);
                return false;
            }

            List<Mat> faces = new ArrayList<>();
            List<Integer> labels = new ArrayList<>();

            File[] files = dir.listFiles((d, name) -> name.startsWith("face_") && name.endsWith(".png"));
            if (files == null || files.length < 3) {
                System.err.println("Not enough face samples for user " + userId);
                return false;
            }

            for (File file : files) {
                Mat img = opencv_imgcodecs.imread(file.getAbsolutePath(), opencv_imgcodecs.IMREAD_GRAYSCALE);
                if (!img.empty()) {
                    faces.add(img);
                    labels.add(userId);
                }
            }

            if (faces.isEmpty()) {
                System.err.println("No valid face images for user " + userId);
                return false;
            }

            // Create MatVector from faces list
            MatVector faceMats = new MatVector(faces.size());
            Mat labelsMat = new Mat(faces.size(), 1, org.bytedeco.opencv.global.opencv_core.CV_32SC1);
            for (int i = 0; i < faces.size(); i++) {
                faceMats.put(i, faces.get(i));
                labelsMat.ptr(i).putInt(labels.get(i));
            }

            // Train LBPH recognizer for this user
            LBPHFaceRecognizer userRecognizer = LBPHFaceRecognizer.create(1, 8, 8, 8, RECOGNITION_THRESHOLD);
            userRecognizer.train(faceMats, labelsMat);

            // Save the model
            String modelPath = userFaceDir + File.separator + "model.yml";
            userRecognizer.save(modelPath);
            System.out.println("Face model trained and saved for user " + userId);

            // Force retrain global model on next recognition attempt
            modelTrained = false;

            // Delete the saved global model so it's rebuilt with fresh data
            File globalModel = new File(GLOBAL_MODEL_PATH);
            if (globalModel.exists()) globalModel.delete();

            // Clean up
            userRecognizer.close();
            for (Mat face : faces) face.close();
            faceMats.close();
            labelsMat.close();

            return true;
        } catch (Exception e) {
            System.err.println("Error training face model: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // ==================== Recognition ====================

    /**
     * Loads and trains a global LBPH model from ALL registered users' face data.
     * This is called before recognition to ensure the model is up to date.
     */
    public boolean loadGlobalModel() {
        try {
            File baseDir = new File(FACE_DATA_DIR);
            if (!baseDir.exists()) {
                System.err.println("No face data directory found.");
                return false;
            }

            List<Mat> allFaces = new ArrayList<>();
            List<Integer> allLabels = new ArrayList<>();

            File[] userDirs = baseDir.listFiles(File::isDirectory);
            if (userDirs == null || userDirs.length == 0) {
                System.err.println("No user face data found.");
                return false;
            }

            for (File userDir : userDirs) {
                int userId;
                try {
                    userId = Integer.parseInt(userDir.getName());
                } catch (NumberFormatException e) {
                    continue;
                }

                File[] faceFiles = userDir.listFiles((d, name) -> name.startsWith("face_") && name.endsWith(".png"));
                if (faceFiles == null) continue;

                for (File faceFile : faceFiles) {
                    Mat img = opencv_imgcodecs.imread(faceFile.getAbsolutePath(), opencv_imgcodecs.IMREAD_GRAYSCALE);
                    if (!img.empty()) {
                        allFaces.add(img);
                        allLabels.add(userId);
                    }
                }
            }

            if (allFaces.isEmpty()) {
                System.err.println("No face images loaded for training.");
                return false;
            }

            // Build MatVector and labels
            MatVector faceMats = new MatVector(allFaces.size());
            Mat labelsMat = new Mat(allFaces.size(), 1, org.bytedeco.opencv.global.opencv_core.CV_32SC1);
            for (int i = 0; i < allFaces.size(); i++) {
                faceMats.put(i, allFaces.get(i));
                labelsMat.ptr(i).putInt(allLabels.get(i));
            }

            // Release previous model and retained training data
            releaseModelData();

            // Train global LBPH recognizer
            recognizer = LBPHFaceRecognizer.create(1, 8, 8, 8, RECOGNITION_THRESHOLD);
            recognizer.train(faceMats, labelsMat);
            modelTrained = true;

            // Save global model to disk for persistence across restarts
            try {
                new File(GLOBAL_MODEL_PATH).getParentFile().mkdirs();
                recognizer.save(GLOBAL_MODEL_PATH);
                System.out.println("Global model saved to: " + GLOBAL_MODEL_PATH);
            } catch (Exception saveEx) {
                System.err.println("Warning: Could not save global model: " + saveEx.getMessage());
            }

            // Retain training data references — prevents native memory deallocation
            // that can corrupt the recognizer's internal histogram pointers
            retainedFaceList = allFaces;
            retainedFaceMats = faceMats;
            retainedLabels = labelsMat;

            System.out.println("Global face model trained with " + allFaces.size() +
                    " samples from " + userDirs.length + " users.");

            return true;
        } catch (Exception e) {
            System.err.println("Error loading global model: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Recognizes a face from a frame.
     * Returns a RecognitionResult with the predicted userId and confidence.
     * Two-pass verification: global model first, then user-specific model for confirmation.
     */
    public RecognitionResult recognizeFace(Mat faceROI) {
        if (!modelTrained) {
            if (!loadGlobalModel()) {
                return new RecognitionResult(-1, Double.MAX_VALUE, false);
            }
        }

        try {
            IntPointer label = new IntPointer(1);
            DoublePointer confidence = new DoublePointer(1);

            recognizer.predict(faceROI, label, confidence);

            int predictedUserId = label.get(0);
            double conf = confidence.get(0);

            label.close();
            confidence.close();

            boolean matched = conf < RECOGNITION_THRESHOLD && predictedUserId > 0;

            // Second pass: verify against user-specific model for stronger uniqueness
            if (matched) {
                matched = verifyWithUserModel(predictedUserId, faceROI);
                if (!matched) {
                    System.out.println("Second-pass verification failed for userId=" + predictedUserId);
                }
            }

            System.out.println("Recognition result: userId=" + predictedUserId + ", confidence=" +
                    String.format("%.2f", conf) + ", matched=" + matched);

            return new RecognitionResult(predictedUserId, conf, matched);
        } catch (Exception e) {
            System.err.println("Error during face recognition: " + e.getMessage());
            return new RecognitionResult(-1, Double.MAX_VALUE, false);
        }
    }

    /**
     * Verifies a face against a specific user's trained model for stronger confirmation.
     * Returns true only if the user-specific model also confidently matches.
     */
    private boolean verifyWithUserModel(int userId, Mat faceROI) {
        String userModelPath = FACE_DATA_DIR + File.separator + userId + File.separator + "model.yml";
        File modelFile = new File(userModelPath);
        if (!modelFile.exists()) {
            System.err.println("No user-specific model found for userId=" + userId);
            return false;
        }

        LBPHFaceRecognizer userRecognizer = null;
        try {
            userRecognizer = LBPHFaceRecognizer.create(1, 8, 8, 8, RECOGNITION_THRESHOLD);
            userRecognizer.read(userModelPath);

            IntPointer userLabel = new IntPointer(1);
            DoublePointer userConf = new DoublePointer(1);
            userRecognizer.predict(faceROI, userLabel, userConf);

            int userPredicted = userLabel.get(0);
            double userConfidence = userConf.get(0);

            userLabel.close();
            userConf.close();

            System.out.println("User model verification: predicted=" + userPredicted +
                    ", confidence=" + String.format("%.2f", userConfidence));

            return userPredicted == userId && userConfidence < RECOGNITION_THRESHOLD;
        } catch (Exception e) {
            System.err.println("Error verifying with user model: " + e.getMessage());
            return false;
        } finally {
            if (userRecognizer != null) {
                try { userRecognizer.close(); } catch (Exception ignored) {}
            }
        }
    }

    // ==================== Resource Management ====================

    /**
     * Releases the current recognizer and any retained training data.
     * Called before retraining to prevent memory leaks.
     */
    private void releaseModelData() {
        if (recognizer != null) {
            try { recognizer.close(); } catch (Exception ignored) {}
            recognizer = null;
        }
        if (retainedFaceList != null) {
            for (Mat m : retainedFaceList) {
                try { m.close(); } catch (Exception ignored) {}
            }
            retainedFaceList = null;
        }
        if (retainedFaceMats != null) {
            try { retainedFaceMats.close(); } catch (Exception ignored) {}
            retainedFaceMats = null;
        }
        if (retainedLabels != null) {
            try { retainedLabels.close(); } catch (Exception ignored) {}
            retainedLabels = null;
        }
        modelTrained = false;
    }

    // ==================== Cleanup ====================

    /**
     * Deletes all face data for a user (photos + model).
     */
    public boolean deleteFaceData(int userId) {
        try {
            String userFaceDir = FACE_DATA_DIR + File.separator + userId;
            File dir = new File(userFaceDir);
            if (dir.exists()) {
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        file.delete();
                    }
                }
                dir.delete();
                modelTrained = false; // Force retrain
                // Delete the saved global model so it's rebuilt
                File globalModel = new File(GLOBAL_MODEL_PATH);
                if (globalModel.exists()) globalModel.delete();
                System.out.println("Face data deleted for user " + userId);
            }
            return true;
        } catch (Exception e) {
            System.err.println("Error deleting face data: " + e.getMessage());
            return false;
        }
    }

    /**
     * Deletes ALL face data for every user and the global model.
     */
    public boolean deleteAllFaceData() {
        try {
            File baseDir = new File(FACE_DATA_DIR);
            if (baseDir.exists()) {
                deleteDirectoryRecursive(baseDir);
                System.out.println("All face data deleted.");
            }
            releaseModelData();
            File globalModel = new File(GLOBAL_MODEL_PATH);
            if (globalModel.exists()) globalModel.delete();
            return true;
        } catch (Exception e) {
            System.err.println("Error deleting all face data: " + e.getMessage());
            return false;
        }
    }

    private void deleteDirectoryRecursive(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteDirectoryRecursive(f);
                }
                f.delete();
            }
        }
        dir.delete();
    }

    /**
     * Checks if a user has face data stored locally.
     */
    public boolean hasFaceData(int userId) {
        String userFaceDir = FACE_DATA_DIR + File.separator + userId;
        File dir = new File(userFaceDir);
        if (!dir.exists()) return false;
        File[] files = dir.listFiles((d, name) -> name.startsWith("face_") && name.endsWith(".png"));
        return files != null && files.length >= 3;
    }

    /**
     * Returns the face data directory path for a user.
     */
    public String getFaceDataDir(int userId) {
        return FACE_DATA_DIR + File.separator + userId;
    }

    // ==================== Utility: Mat to JavaFX Image ====================

    /**
     * Converts an OpenCV Mat (BGR) to a JavaFX Image.
     * Uses JavaCV's Frame converter pipeline for reliable conversion:
     * Mat → Frame → BufferedImage → JavaFX Image
     */
    public javafx.scene.image.Image matToJavaFXImage(Mat mat) {
        if (mat == null || mat.empty()) return null;
        try {
            // Create local converters — OpenCVFrameConverter and Java2DFrameConverter
            // are NOT thread-safe, so we must not share them across threads.
            OpenCVFrameConverter.ToMat localMatConverter = new OpenCVFrameConverter.ToMat();
            Java2DFrameConverter localJava2dConverter = new Java2DFrameConverter();

            // Mat → Frame (OpenCV format → JavaCV universal frame)
            Frame frame = localMatConverter.convert(mat);
            if (frame == null) return null;

            // Frame → BufferedImage (Java2D)
            BufferedImage bi = localJava2dConverter.convert(frame);
            if (bi == null) return null;

            // BufferedImage → JavaFX WritableImage
            return SwingFXUtils.toFXImage(bi, null);
        } catch (Exception e) {
            System.err.println("Error converting Mat to JavaFX Image: " + e.getMessage());
            return null;
        }
    }

    // ==================== Inner Class: Recognition Result ====================

    /**
     * Holds the result of a face recognition attempt.
     */
    public static class RecognitionResult {
        private final int userId;
        private final double confidence;
        private final boolean matched;

        public RecognitionResult(int userId, double confidence, boolean matched) {
            this.userId = userId;
            this.confidence = confidence;
            this.matched = matched;
        }

        public int getUserId() { return userId; }
        public double getConfidence() { return confidence; }
        public boolean isMatched() { return matched; }

        /**
         * Returns a confidence percentage (higher = better match).
         * LBPH confidence is a distance, so we invert it.
         */
        public int getConfidencePercent() {
            if (confidence >= RECOGNITION_THRESHOLD) return 0;
            return (int) Math.round((1.0 - confidence / RECOGNITION_THRESHOLD) * 100);
        }
    }
}
