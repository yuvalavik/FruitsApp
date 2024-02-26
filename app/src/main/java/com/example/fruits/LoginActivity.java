package com.example.fruits;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LoginActivity extends AppCompatActivity {
    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private static final String SERVER_URL = "http://172.20.10.8:3000/upload";

    private ExecutorService cameraExecutor;
    private boolean isCameraPermissionGranted = false;
    private ImageButton cameraButton;
    private Handler handler = new Handler();
    private boolean captureImages = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        cameraButton = findViewById(R.id.cameraIcon);

        cameraButton.setOnClickListener(v -> {
            if (isCameraPermissionGranted) {
                startCapturingImages();
            } else {
                requestCameraPermission();
            }
        });

        cameraExecutor = Executors.newSingleThreadExecutor();
    }

    private void startCapturingImages() {
        captureImages = true; // Set captureImages flag to true
        takePhotoRepeatedly();
    }

    private void takePhotoRepeatedly() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // Set up the camera preview
                Preview preview = new Preview.Builder().build();
                ImageCapture imageCapture = new ImageCapture.Builder().build();

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

                // Set the preview to the PreviewView
                PreviewView previewView = findViewById(R.id.previewView);
                previewView.setVisibility(View.VISIBLE);
                Button stop = findViewById(R.id.stop);
                stop.setVisibility(View.VISIBLE);
                cameraButton.setVisibility(View.GONE);

                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // Capture images repeatedly
                captureImageRepeatedly(imageCapture);

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void captureImageRepeatedly(ImageCapture imageCapture) {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (captureImages) {
                    imageCapture.takePicture(cameraExecutor, new ImageCapture.OnImageCapturedCallback() {
                        @Override
                        public void onCaptureSuccess(@NonNull ImageProxy image) {
                            runOnUiThread(() -> {
                                // Convert ImageProxy to byte array
                                byte[] imageData = convertImageProxyToByteArray(image);
                                String imageDataString = new String(imageData);

                                // Log the byte array as a string
                                // Send image to server
                                sendImageToServer(imageData);

                                image.close();
                            });
                        }

                        @Override
                        public void onError(@NonNull ImageCaptureException exception) {
                            super.onError(exception);
                            Log.e("ImageCapture", "Error capturing image: " + exception.getMessage());
                        }
                    });
                    // Capture the next image after 0.5 seconds
                    handler.postDelayed(this, 500);
                }
            }
        }, 500); // Initial delay of 0.5 seconds
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA},
                REQUEST_CAMERA_PERMISSION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                isCameraPermissionGranted = true;
                startCapturingImages();
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @OptIn(markerClass = androidx.camera.core.ExperimentalGetImage.class)
    private byte[] convertImageProxyToByteArray(ImageProxy imageProxy) {
        Image image = imageProxy.getImage();
        if (image != null) {

            Image.Plane[] planes = image.getPlanes();
            ByteBuffer buffer = planes[0].getBuffer();

            // Calculate total bytes in the buffer
            int bufferSize = buffer.remaining();

            // Create a new byte array with the correct size
            byte[] bytes = new byte[bufferSize];

            // Read all bytes from the buffer into the byte array
            buffer.get(bytes);

            // Release the ImageProxy
            imageProxy.close();

            return bytes;
        }
        return null;
    }

    private void sendImageToServer(byte[] imageData) {
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                MediaType MEDIA_TYPE_JPEG = MediaType.parse("image/jpeg");

                // Encode the byte array using Base64
                String encodedImage = Base64.encodeToString(imageData, Base64.DEFAULT);

                // Create request body with the encoded image data
                RequestBody requestBody = RequestBody.create(encodedImage, MEDIA_TYPE_JPEG);

                Request request = new Request.Builder()
                        .url(SERVER_URL)
                        .post(requestBody)
                        .build();

                Response response = client.newCall(request).execute();
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                }

                // If you need to process the response from the server, you can do it here
                String responseBody = response.body().string();
                Log.d("ServerResponse", responseBody);

            } catch (IOException e) {
                Log.e("SendImage", "Error sending image to server", e);
            }
        }).start();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        captureImages = false; // Stop capturing images when the activity is destroyed
        handler.removeCallbacksAndMessages(null); // Remove all pending callbacks and messages
    }
}




