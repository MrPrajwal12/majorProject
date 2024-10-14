package com.project.intellifit_trainer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureRequest;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraDevice;
import android.content.Context;
import android.view.Surface;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;
import android.widget.ImageView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Collections;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;

public class StartWorkoutActivity extends AppCompatActivity {

    private MyWebSocketClient myWebSocketClient; // WEBSOCKET
    private static final int CAMERA_REQUEST_CODE = 100;
    private String cameraId;
    private CameraDevice cameraDevice;
    private CameraCaptureSession cameraCaptureSession;
    private CaptureRequest.Builder captureRequestBuilder;
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;
    private CameraCharacteristics cameraCharacteristics;
    private AutoFitTextureView textureView;
    private ImageView imageView; // ImageView definition
    private final int IMAGE_SEND_INTERVAL = 100; // milliseconds
    private final Handler imageSendHandler = new Handler();
    private final Runnable imageSendRunnable = new Runnable() {
        @Override
        public void run() {
            sendImage();
            imageSendHandler.postDelayed(this, IMAGE_SEND_INTERVAL);
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();
        imageSendHandler.postDelayed(imageSendRunnable, IMAGE_SEND_INTERVAL);
    }

    @Override
    protected void onPause() {
        stopBackgroundThread();
        super.onPause();
    }

    private final TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
            Size previewSize = chooseOptimalSize(cameraCharacteristics.get(
                            CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(SurfaceTexture.class),
                    width, height);
            if (previewSize != null) {
                textureView.setAspectRatio(previewSize.getWidth(), previewSize.getHeight());
                configureTransform(width, height, previewSize);
            }
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {
            // Handle surface size changes if needed
        }

        @Override
        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
            // Handle surface updates if needed
        }
    };

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            cameraDevice.close();
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
            Log.e("StartWorkoutActivity", "Camera error: " + error);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_startworkout);

        textureView = findViewById(R.id.textureView);
        textureView.setSurfaceTextureListener(textureListener);
        imageView = findViewById(R.id.processedImageView);

        getWorkoutNames();

        myWebSocketClient = new MyWebSocketClient(this);
        myWebSocketClient.start();

        // Check camera permissions and start the camera preview
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
        } else {
            openCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Log.w("StartWorkoutActivity", "Camera permission denied");
            }
        }
    }

    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            cameraId = manager.getCameraIdList()[0];
            cameraCharacteristics = manager.getCameraCharacteristics(cameraId);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            manager.openCamera(cameraId, stateCallback, backgroundHandler);
        } catch (CameraAccessException e) {
            Log.e("StartWorkoutActivity", "Camera access exception: ", e);
        }
    }

    private void createCameraPreviewSession() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(textureView.getWidth(), textureView.getHeight());
            Surface surface = new Surface(texture);

            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);

            cameraDevice.createCaptureSession(Collections.singletonList(surface),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            if (cameraDevice == null) {
                                return;
                            }
                            cameraCaptureSession = session;
                            updatePreview();
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            Log.e("StartWorkoutActivity", "Camera configuration failed");
                        }
                    }, null);
        } catch (CameraAccessException e) {
            Log.e("StartWorkoutActivity", "Camera access exception: ", e);
        }
    }

    private void updatePreview() {
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        try {
            cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, backgroundHandler);
        } catch (CameraAccessException e) {
            Log.e("StartWorkoutActivity", "Camera access exception: ", e);
        }
    }

    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
            try {
                backgroundThread.join();
                backgroundThread = null;
                backgroundHandler = null;
            } catch (InterruptedException e) {
                Log.e("StartWorkoutActivity", "Error stopping background thread", e);
            }
        }
    }

    private Size chooseOptimalSize(Size[] choices, int textureViewWidth, int textureViewHeight) {
        List<Size> bigEnough = new ArrayList<>();
        for (Size option : choices) {
            if (option.getHeight() == option.getWidth() * textureViewHeight / textureViewWidth
                    && option.getWidth() >= textureViewWidth && option.getHeight() >= textureViewHeight) {
                bigEnough.add(option);
            }
        }

        if (!bigEnough.isEmpty()) {
            return Collections.min(bigEnough, Comparator.comparingLong(size -> (long) size.getWidth() * size.getHeight()));
        }

        return choices[0];
    }

    private void configureTransform(int viewWidth, int viewHeight, Size previewSize) {
        if (textureView == null || previewSize == null) {
            return;
        }

        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, previewSize.getWidth(), previewSize.getHeight());

        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();

        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
        }

        textureView.setTransform(matrix);
    }

    private void sendImage() {
        // Get the bitmap from the TextureView
        textureView.setDrawingCacheEnabled(true);
        Bitmap bitmap = textureView.getDrawingCache();

        // Check if bitmap is available
        if (bitmap != null) {
            // Update the ImageView with the processed image
            updateImageView(bitmap);
            // Reset the drawing cache
            textureView.setDrawingCacheEnabled(false);
        }
    }

    // Method to update the ImageView with the processed image
    void updateImageView(Bitmap bitmap) {
        // Here you can perform any image processing if needed
        imageView.setImageBitmap(bitmap);
    }

    private void getWorkoutNames() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("users").child(user.getUid()).child("workoutNames");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot workoutSnapshot : dataSnapshot.getChildren()) {
                    String workoutName = workoutSnapshot.getValue(String.class);
                    // Handle the workout name as needed
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("StartWorkoutActivity", "Database error: " + databaseError.getMessage());
            }
        });
    }
}
