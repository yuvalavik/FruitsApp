package com.example.fruits;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class LoginActivity extends AppCompatActivity {
    private static final int REQUEST_CAMERA_PERMISSION = 100;

    private ActivityResultLauncher<Intent> videoCaptureLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        ImageButton camera = findViewById(R.id.cameraIcon);

        // Initialize ActivityResultLauncher for video capture
        videoCaptureLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        // Video captured, you can do something with it here if needed
                        // For example, you can get the Uri of the recorded video from 'data' Intent
                        Intent data = result.getData();
                        Uri videoUri = data.getData();
                        Toast.makeText(this, "Video captured: " + videoUri, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Video capture failed", Toast.LENGTH_SHORT).show();
                    }
                });

        camera.setOnClickListener(v -> {
            if (checkCameraPermission()) {
                dispatchTakeVideoIntent();
            } else {
                requestCameraPermission();
            }
        });
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA},
                REQUEST_CAMERA_PERMISSION);
    }

    private void dispatchTakeVideoIntent() {
        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        videoCaptureLauncher.launch(takeVideoIntent);
    }
//sss
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with capturing video
                dispatchTakeVideoIntent();
            } else {
                // Permission denied, handle the scenario accordingly
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}



