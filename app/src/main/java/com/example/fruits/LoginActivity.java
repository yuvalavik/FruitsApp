package com.example.fruits;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
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
    private static final String SERVER_URL = "https://4609-132-70-66-11.ngrok-free.app/upload";

    private ExecutorService cameraExecutor;
    private boolean isCameraPermissionGranted = false;
    private ImageButton cameraButton;
    private Button sendButton;
    private Handler handler = new Handler();
    private ImageCapture imageCapture;
    private ProcessCameraProvider cameraProvider;

    private String base64ImageString;
    private String encodedImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        cameraButton = findViewById(R.id.cameraIcon);
        sendButton = findViewById(R.id.stop);

        cameraButton.setOnClickListener(v -> {
            if (isCameraPermissionGranted) {
                openCamera();
            } else {
                requestCameraPermission();
            }
        });

        sendButton.setOnClickListener(v -> {
            if (imageCapture != null) {
                captureAndSendImage();
                sendButton.setVisibility(View.GONE);
                TextView t = findViewById(R.id.wait);
                t.setVisibility(View.VISIBLE);
            }
        });

        cameraExecutor = Executors.newSingleThreadExecutor();
    }

    private void openCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();

                // Set up the camera preview
                Preview preview = new Preview.Builder().build();
                imageCapture = new ImageCapture.Builder().build();

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

                // Set the preview to the PreviewView
                PreviewView previewView = findViewById(R.id.previewView);
                previewView.setVisibility(View.VISIBLE);
                cameraButton.setVisibility(View.GONE);
                sendButton.setVisibility(View.VISIBLE);
                TextView t = findViewById(R.id.textView);
                t.setVisibility(View.GONE);

                preview.setSurfaceProvider(previewView.getSurfaceProvider());

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void captureAndSendImage() {
        imageCapture.takePicture(cameraExecutor, new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy image) {
                byte[] imageData = convertImageProxyToByteArray(image);

                // Send image to server

                    CompletableFuture<Integer> future = sendImageToServer(imageData);

                    future.thenAccept(result -> {
                        if (result ==1) {
                            image.close();
                            closeCamera();
                        }
                    });



            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                super.onError(exception);
                Log.e("ImageCapture", "Error capturing image: " + exception.getMessage());
            }
        });
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
                openCamera();
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

            return bytes;
        }
        return null;
    }

    private CompletableFuture<Integer> sendImageToServer(byte[] imageData) {
        CompletableFuture<Integer> i = new CompletableFuture<>();
        new Thread(() -> {
            try {

                OkHttpClient client = new OkHttpClient();
                MediaType MEDIA_TYPE_JPEG = MediaType.parse("image/jpeg");

                // Encode the byte array using Base64
                encodedImage = Base64.encodeToString(imageData, Base64.DEFAULT);
//                Log.d("front",encodedImage);

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
                String res = response.body().string();
                base64ImageString = extractBase64Image(res);
                Log.d("ServerResponse", res);
                i.complete(1);

            } catch (IOException e) {
                Log.e("SendImage", "Error sending image to server", e);
                i.complete(-1);
            }
        }).start();
        return i;
    }

    private String extractBase64Image(String jsonResponse) {
        // Assuming the JSON response is in the format {"processed_image":"<base64_string>"}
        return jsonResponse.split("\"processed_image\":\"")[1].split("\"")[0];
    }


    private void closeCamera() {
        if (cameraProvider != null) {
            // Unbind the cameraProvider

            // option to save the pic and than send its pass if the pic is too large
            File imageFile = saveImageToFile(base64ImageString,"processed_image.jpg");
            File oe = saveBase64StringToFile(encodedImage,"encoded_image.jpg");

            // Start the DisplayImageActivity
            Intent intent = new Intent(LoginActivity.this, DisplayImageActivity.class);
            intent.putExtra("imageFilePath", imageFile.getAbsolutePath());
            intent.putExtra("original", oe.getAbsolutePath());
            startActivity(intent);
            cameraProvider.unbindAll();
            cameraProvider = null;

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        handler.removeCallbacksAndMessages(null); // Remove all pending callbacks and messages
    }

    private File saveImageToFile(String base64ImageString,String s) {
        // Decode Base64 string to byte array
        byte[] decodedBytes = Base64.decode(base64ImageString, Base64.DEFAULT);

        // Create a bitmap from the decoded byte array
        Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);

        // Create a file in the external storage directory
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (storageDir != null && !storageDir.exists()) {
            storageDir.mkdirs();
        }
        File imageFile = new File(storageDir, s);

        // Write the bitmap to the file
        try (FileOutputStream fos = new FileOutputStream(imageFile)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
        } catch (IOException e) {
            Log.e("SaveImage", "Error saving image to file", e);
        }

        return imageFile;
    }
    private File saveBase64StringToFile(String base64String, String fileName) {
        File storageDir = getFilesDir();
        File base64File = new File(storageDir, fileName);

        try (FileOutputStream fos = new FileOutputStream(base64File)) {
            fos.write(base64String.getBytes());
        } catch (IOException e) {
            Log.e("SaveBase64String", "Error saving Base64 string to file", e);
        }

        return base64File;
    }

    @Override
    protected void onResume() {
        super.onResume();
        PreviewView previewView = findViewById(R.id.previewView);
        previewView.setVisibility(View.GONE);
        TextView text = findViewById(R.id.wait);
        text.setVisibility(View.GONE);
        sendButton.setVisibility(View.GONE);
        cameraButton.setVisibility(View.VISIBLE);
        TextView t = findViewById(R.id.textView);
        t.setVisibility(View.VISIBLE);

    }

}
