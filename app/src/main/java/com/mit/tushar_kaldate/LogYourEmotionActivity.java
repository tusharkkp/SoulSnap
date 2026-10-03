package com.mit.tushar_kaldate;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.mit.tushar_kaldate.database.AppDatabase;
import com.mit.tushar_kaldate.ml.EmotionClassifier;
import com.mit.tushar_kaldate.model.Emotion;
import com.mit.tushar_kaldate.utils.SessionManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LogYourEmotionActivity extends AppCompatActivity {

    private static final String TAG = "LogYourEmotionActivity";

    // Views
    private PreviewView viewFinder;
    private View layoutCamera;
    private View layoutAnalysis;
    private View layoutPermissionDenied;
    private TextView tvPermissionTitle;
    private TextView tvPermissionDesc;
    private AppCompatButton btnGrantPermission;
    private AppCompatButton btnChooseGalleryPermission;
    private LinearLayout llAnalyzing;
    private LinearLayout llDetectionError;
    private TextView tvErrorHeader;
    private TextView tvErrorDetail;
    private ImageView ivCapturedPhoto;
    private ImageView ivCroppedFace;
    private MaterialCardView cardCroppedFace;
    private MaterialCardView cardEmotionResult;
    private MaterialCardView cardNoteInput;
    private TextView tvEmotionEmoji;
    private TextView tvEmotionName;
    private TextView tvEmotionConfidence;
    private EditText etNote;
    private TextView tvTimestamp;
    private TextView tvCharCount;
    private AppCompatButton btnSaveEmotion;
    private AppCompatButton btnRetakePhoto;
    private AppCompatButton btnRetakeError;
    private AppCompatButton btnPickGalleryError;
    private FloatingActionButton btnCapture;
    private ImageButton btnChooseGallery;
    private ImageButton btnSwitchCamera;
    private ImageButton btnBack;

    // CameraX
    private ImageCapture imageCapture;
    private int lensFacing = CameraSelector.LENS_FACING_FRONT;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    // ML Components
    private FaceDetector faceDetector;
    private EmotionClassifier emotionClassifier;

    // State & Database
    private Bitmap currentCapturedBitmap;
    private Bitmap currentCroppedFaceBitmap;
    private EmotionClassifier.RecognitionResult currentEmotionResult;
    private String currentDate;
    private String currentTime;
    private AppDatabase database;
    private SessionManager sessionManager;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    // Permission Launcher: Never call finish() on denial!
    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    showCameraView();
                    startCamera();
                } else {
                    showPermissionDeniedView("Camera permission is needed to take a live photo. You can enable it below or select a photo from your device.");
                }
            });

    // Zero-permission modern Android Photo Picker
    private final ActivityResultLauncher<PickVisualMediaRequest> pickMediaLauncher =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    processImageUri(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_your_emotion);

        sessionManager = new SessionManager(this);
        database = AppDatabase.getInstance(this);

        initViews();
        initMLComponents();

        // Check Camera permission gracefully
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            showCameraView();
            startCamera();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void initViews() {
        viewFinder = findViewById(R.id.viewFinder);
        layoutCamera = findViewById(R.id.layoutCamera);
        layoutAnalysis = findViewById(R.id.layoutAnalysis);
        layoutPermissionDenied = findViewById(R.id.layoutPermissionDenied);
        tvPermissionTitle = findViewById(R.id.tvPermissionTitle);
        tvPermissionDesc = findViewById(R.id.tvPermissionDesc);
        btnGrantPermission = findViewById(R.id.btnGrantPermission);
        btnChooseGalleryPermission = findViewById(R.id.btnChooseGalleryPermission);
        llAnalyzing = findViewById(R.id.llAnalyzing);
        llDetectionError = findViewById(R.id.llDetectionError);
        tvErrorHeader = findViewById(R.id.tvErrorHeader);
        tvErrorDetail = findViewById(R.id.tvErrorDetail);
        ivCapturedPhoto = findViewById(R.id.ivCapturedPhoto);
        ivCroppedFace = findViewById(R.id.ivCroppedFace);
        cardCroppedFace = findViewById(R.id.cardCroppedFace);
        cardEmotionResult = findViewById(R.id.cardEmotionResult);
        cardNoteInput = findViewById(R.id.cardNoteInput);
        tvEmotionEmoji = findViewById(R.id.tvEmotionEmoji);
        tvEmotionName = findViewById(R.id.tvEmotionName);
        tvEmotionConfidence = findViewById(R.id.tvEmotionConfidence);
        etNote = findViewById(R.id.etNote);
        tvTimestamp = findViewById(R.id.tvTimestamp);
        tvCharCount = findViewById(R.id.tvCharCount);
        btnSaveEmotion = findViewById(R.id.btnSaveEmotion);
        btnRetakePhoto = findViewById(R.id.btnRetakePhoto);
        btnRetakeError = findViewById(R.id.btnRetakeError);
        btnPickGalleryError = findViewById(R.id.btnPickGalleryError);
        btnCapture = findViewById(R.id.btnCapture);
        btnChooseGallery = findViewById(R.id.btnChooseGallery);
        btnSwitchCamera = findViewById(R.id.btnSwitchCamera);
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        btnSwitchCamera.setOnClickListener(v -> {
            lensFacing = (lensFacing == CameraSelector.LENS_FACING_FRONT)
                    ? CameraSelector.LENS_FACING_BACK
                    : CameraSelector.LENS_FACING_FRONT;
            startCamera();
        });

        btnCapture.setOnClickListener(v -> takePhoto());
        btnChooseGallery.setOnClickListener(v -> openPhotoPicker());
        btnChooseGalleryPermission.setOnClickListener(v -> openPhotoPicker());
        btnPickGalleryError.setOnClickListener(v -> openPhotoPicker());
        btnGrantPermission.setOnClickListener(v -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA));

        btnRetakePhoto.setOnClickListener(v -> resetToCamera());
        btnRetakeError.setOnClickListener(v -> resetToCamera());
        btnSaveEmotion.setOnClickListener(v -> saveEmotionRecord());

        etNote.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int length = s != null ? s.length() : 0;
                tvCharCount.setText(length + "/200");
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void openPhotoPicker() {
        try {
            pickMediaLauncher.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        } catch (Exception e) {
            Log.e(TAG, "Photo picker launch error: " + e.getMessage());
            Toast.makeText(this, "Could not open photo picker: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showPermissionDeniedView(String description) {
        layoutCamera.setVisibility(View.GONE);
        layoutAnalysis.setVisibility(View.GONE);
        layoutPermissionDenied.setVisibility(View.VISIBLE);
        if (description != null) {
            tvPermissionDesc.setText(description);
        }
    }

    private void showCameraView() {
        layoutPermissionDenied.setVisibility(View.GONE);
        layoutAnalysis.setVisibility(View.GONE);
        layoutCamera.setVisibility(View.VISIBLE);
    }

    private void initMLComponents() {
        // Fast & accurate ML Kit Face Detector setup
        try {
            FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                    .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                    .setMinFaceSize(0.15f)
                    .build();
            faceDetector = FaceDetection.getClient(options);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing FaceDetector: " + e.getMessage(), e);
        }

        // Pre-trained TensorFlow Lite Emotion Classifier
        try {
            emotionClassifier = new EmotionClassifier(this);
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize TensorFlow Lite EmotionClassifier: " + e.getMessage(), e);
        }
    }

    private void startCamera() {
        try {
            cameraProviderFuture = ProcessCameraProvider.getInstance(this);
            cameraProviderFuture.addListener(() -> {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    bindPreview(cameraProvider);
                } catch (ExecutionException | InterruptedException e) {
                    Log.e(TAG, "Camera binding error: " + e.getMessage(), e);
                    showPermissionDeniedView("Camera is unavailable on this device. You can pick a photo from your gallery instead.");
                }
            }, ContextCompat.getMainExecutor(this));
        } catch (Exception ex) {
            Log.e(TAG, "ProcessCameraProvider error: " + ex.getMessage(), ex);
            showPermissionDeniedView("Camera is unavailable on this device. You can pick a photo from your gallery instead.");
        }
    }

    private void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        try {
            cameraProvider.unbindAll();

            boolean hasFront = cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA);
            boolean hasBack = cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA);

            if (!hasFront && !hasBack) {
                showPermissionDeniedView("No camera hardware detected on this device. Please select a photo from your device.");
                return;
            }

            // Adjust facing if current selection is not supported
            if (lensFacing == CameraSelector.LENS_FACING_FRONT && !hasFront) {
                lensFacing = CameraSelector.LENS_FACING_BACK;
            } else if (lensFacing == CameraSelector.LENS_FACING_BACK && !hasBack) {
                lensFacing = CameraSelector.LENS_FACING_FRONT;
            }

            Preview preview = new Preview.Builder().build();
            CameraSelector cameraSelector = new CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build();

            imageCapture = new ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build();

            preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
            showCameraView();
        } catch (Exception e) {
            Log.e(TAG, "Use case binding failed: " + e.getMessage(), e);
            showPermissionDeniedView("Unable to initialize camera preview. You can choose a photo from your device.");
        }
    }

    private void takePhoto() {
        if (imageCapture == null) {
            Toast.makeText(this, "Camera not ready. You can also pick a photo from gallery.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnCapture.setEnabled(false);

        File photoFile;
        try {
            File outputDir = getCacheDir();
            photoFile = File.createTempFile("capture_", ".jpg", outputDir);
        } catch (IOException e) {
            btnCapture.setEnabled(true);
            Toast.makeText(this, "Could not create temporary capture file.", Toast.LENGTH_SHORT).show();
            return;
        }

        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                btnCapture.setEnabled(true);
                processCapturedImageFile(photoFile);
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                btnCapture.setEnabled(true);
                Log.e(TAG, "Photo capture failed: " + exception.getMessage(), exception);
                Toast.makeText(LogYourEmotionActivity.this, "Photo capture failed: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void processImageUri(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            if (is != null) {
                is.close();
            }
            if (bitmap != null) {
                startFaceDetectionOnBitmap(bitmap);
            } else {
                Toast.makeText(this, "Could not load selected photo.", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading image from URI: " + e.getMessage(), e);
            Toast.makeText(this, "Error loading image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void processCapturedImageFile(File file) {
        Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
        if (bitmap == null) {
            showDetectionError("Capture Error", "Failed to load captured photograph. Please try again.");
            return;
        }

        // If front camera, mirror horizontally for natural appearance
        if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
            Matrix matrix = new Matrix();
            matrix.preScale(-1.0f, 1.0f);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        }

        startFaceDetectionOnBitmap(bitmap);
    }

    private void startFaceDetectionOnBitmap(Bitmap bitmap) {
        // Switch view to analysis layout
        layoutCamera.setVisibility(View.GONE);
        layoutPermissionDenied.setVisibility(View.GONE);
        layoutAnalysis.setVisibility(View.VISIBLE);
        llAnalyzing.setVisibility(View.VISIBLE);
        llDetectionError.setVisibility(View.GONE);
        cardEmotionResult.setVisibility(View.GONE);
        cardNoteInput.setVisibility(View.GONE);
        btnSaveEmotion.setVisibility(View.GONE);
        btnRetakePhoto.setVisibility(View.GONE);

        currentCapturedBitmap = bitmap;
        ivCapturedPhoto.setImageBitmap(currentCapturedBitmap);

        // Setup current date and time
        Date now = new Date();
        currentDate = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(now);
        currentTime = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(now);
        tvTimestamp.setText(String.format("Date: %s  •  %s", currentDate, currentTime));

        // Step 1: Run ML Kit Face Detection
        if (faceDetector == null) {
            initMLComponents();
        }

        InputImage image = InputImage.fromBitmap(currentCapturedBitmap, 0);
        faceDetector.process(image)
                .addOnSuccessListener(this::handleFaceDetectionSuccess)
                .addOnFailureListener(e -> {
                    // Even if face detector encounters issues, fall back to analyzing the photo directly
                    handleFaceDetectionSuccess(null);
                });
    }

    private void handleFaceDetectionSuccess(List<Face> faces) {
        Face bestFace = null;
        if (faces != null && !faces.isEmpty()) {
            int maxArea = 0;
            for (Face f : faces) {
                int area = f.getBoundingBox().width() * f.getBoundingBox().height();
                if (area > maxArea) {
                    maxArea = area;
                    bestFace = f;
                }
            }
        }

        if (bestFace != null) {
            Rect bounds = bestFace.getBoundingBox();
            int marginX = Math.round(bounds.width() * 0.15f);
            int marginY = Math.round(bounds.height() * 0.15f);

            int left = Math.max(0, bounds.left - marginX);
            int top = Math.max(0, bounds.top - marginY);
            int right = Math.min(currentCapturedBitmap.getWidth(), bounds.right + marginX);
            int bottom = Math.min(currentCapturedBitmap.getHeight(), bounds.bottom + marginY);

            int width = right - left;
            int height = bottom - top;

            if (width > 0 && height > 0) {
                currentCroppedFaceBitmap = Bitmap.createBitmap(currentCapturedBitmap, left, top, width, height);
            } else {
                currentCroppedFaceBitmap = currentCapturedBitmap;
            }
        } else {
            // When no specific frontal face landmarks detected (e.g. artistic or non-standard stock photos),
            // use a centered square crop of the photograph
            int minDim = Math.min(currentCapturedBitmap.getWidth(), currentCapturedBitmap.getHeight());
            int x = (currentCapturedBitmap.getWidth() - minDim) / 2;
            int y = (currentCapturedBitmap.getHeight() - minDim) / 2;
            currentCroppedFaceBitmap = Bitmap.createBitmap(currentCapturedBitmap, x, y, minDim, minDim);
        }

        ivCroppedFace.setImageBitmap(currentCroppedFaceBitmap);
        cardCroppedFace.setVisibility(View.VISIBLE);

        // Step 2: Run Emotion Classification on background thread
        llAnalyzing.setVisibility(View.VISIBLE);
        executorService.execute(() -> {
            if (emotionClassifier == null) {
                try {
                    emotionClassifier = new EmotionClassifier(this);
                } catch (Exception ignored) {}
            }

            if (emotionClassifier == null) {
                runOnUiThread(() -> {
                    llAnalyzing.setVisibility(View.GONE);
                    showDetectionError("Model Error", "Emotion model is unavailable.");
                });
                return;
            }

            EmotionClassifier.RecognitionResult result = emotionClassifier.classifyFace(currentCroppedFaceBitmap);

            runOnUiThread(() -> {
                llAnalyzing.setVisibility(View.GONE);
                currentEmotionResult = result;

                // Always display Emotion Result cleanly
                tvEmotionEmoji.setText(result.emoji);
                tvEmotionName.setText(result.emotion);
                tvEmotionConfidence.setText(result.getConfidencePercent() + "% confidence");

                llDetectionError.setVisibility(View.GONE);
                cardEmotionResult.setVisibility(View.VISIBLE);
                cardNoteInput.setVisibility(View.VISIBLE);
                btnSaveEmotion.setVisibility(View.VISIBLE);
                btnRetakePhoto.setVisibility(View.VISIBLE);
            });
        });
    }

    private void showDetectionError(String header, String detail) {
        tvErrorHeader.setText(header);
        tvErrorDetail.setText(detail);
        llDetectionError.setVisibility(View.VISIBLE);
        cardCroppedFace.setVisibility(View.GONE);
        cardEmotionResult.setVisibility(View.GONE);
        cardNoteInput.setVisibility(View.GONE);
        btnSaveEmotion.setVisibility(View.GONE);
        btnRetakePhoto.setVisibility(View.GONE);
    }

    private void resetToCamera() {
        layoutAnalysis.setVisibility(View.GONE);
        llDetectionError.setVisibility(View.GONE);
        etNote.setText("");
        currentEmotionResult = null;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            showCameraView();
            startCamera();
        } else {
            showPermissionDeniedView(null);
        }
    }

    private void saveEmotionRecord() {
        if (currentEmotionResult == null || currentCapturedBitmap == null) {
            Toast.makeText(this, "No valid emotion detected to save.", Toast.LENGTH_SHORT).show();
            return;
        }

        String note = etNote.getText().toString().trim();
        if (TextUtils.isEmpty(note)) {
            Toast.makeText(this, "Please write a short note about how you are feeling.", Toast.LENGTH_SHORT).show();
            etNote.requestFocus();
            return;
        }

        int userId = sessionManager.getUserId();
        if (userId <= 0) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnSaveEmotion.setEnabled(false);

        executorService.execute(() -> {
            try {
                // 1. Save captured photo into application private storage
                File emotionsDir = new File(getFilesDir(), "emotions");
                if (!emotionsDir.exists()) {
                    emotionsDir.mkdirs();
                }

                String filename = "emotion_" + userId + "_" + System.currentTimeMillis() + ".jpg";
                File photoFile = new File(emotionsDir, filename);

                FileOutputStream fos = new FileOutputStream(photoFile);
                currentCapturedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                fos.flush();
                fos.close();

                // 2. Save Emotion entity in Room Database
                Emotion emotion = new Emotion(
                        userId,
                        photoFile.getAbsolutePath(),
                        currentEmotionResult.emotion,
                        currentEmotionResult.confidence,
                        note,
                        currentDate,
                        currentTime
                );

                database.emotionDao().insertEmotion(emotion);

                runOnUiThread(() -> {
                    Toast.makeText(LogYourEmotionActivity.this, "Emotion logged to your SoulSnap journal ❤️", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(LogYourEmotionActivity.this, MyEmotionsActivity.class);
                    startActivity(intent);
                    finish();
                });

            } catch (Exception e) {
                Log.e(TAG, "Error saving emotion record: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    btnSaveEmotion.setEnabled(true);
                    Toast.makeText(LogYourEmotionActivity.this, "Failed to save: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (emotionClassifier != null) {
            emotionClassifier.close();
        }
        if (faceDetector != null) {
            faceDetector.close();
        }
        executorService.shutdown();
    }
}
