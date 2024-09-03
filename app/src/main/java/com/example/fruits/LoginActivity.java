package com.example.fruits;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import androidx.exifinterface.media.ExifInterface;
import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
    private static final String SERVER_URL = "https://a4e6-132-70-66-10.ngrok-free.app/upload";

    private ExecutorService cameraExecutor;
    private boolean isCameraPermissionGranted = false;
    private ImageButton cameraButton;
    private Button sendButton;
    private Handler handler = new Handler();
    private ImageCapture imageCapture;
    private ProcessCameraProvider cameraProvider;

    private String encodedImage;
    private String res;


    @SuppressLint("SetTextI18n")
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

            }
        });

        cameraExecutor = Executors.newSingleThreadExecutor();
    }

    /**
     * This function is opening the camera after the user presses the camera icon so the user will be able to
     * take picture of his fruit.
     */
    private void openCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();

                // Set up the camera preview
                Preview preview = new Preview.Builder().build();
                imageCapture = new ImageCapture.Builder().build();
                // choosing the back camera in the phone
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

    /**
     * after pressing the pic button we first display the image to the user and than we are letting him draw rectangle over the fruit and send it
     * to the server to detection after the user press the send button.
     */
    private void captureAndSendImage() {
        imageCapture.takePicture(cameraExecutor, new ImageCapture.OnImageCapturedCallback() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy image) {
                byte[] imageData = convertImageProxyToByteArray(image);
                CompletableFuture<Integer> future = displayCapturedImage(imageData);
                // after getting the future we know that the displayCapturedImage worked so we let him draw rec and change the text.
                future.thenAccept(res ->{
                    if (res == 1){
                        runOnUiThread(() -> {
                            TextView t = findViewById(R.id.wait);
                            t.setText("Draw rec around your fruit");
                            t.setVisibility(View.VISIBLE);
                            Button send  = findViewById(R.id.send);
                            send.setVisibility(View.VISIBLE);
                            send.setOnClickListener(v-> {
                                // we let him draw rec.
                                DrawRectImageView drawRectImageView = findViewById(R.id.res);
                                RectF drawnRect = drawRectImageView.getDrawnRect();
                                // if the user pressed send after he draw a rec we want to take that rec and send to the server
                                if (drawnRect != null) {
                                    // Get the original image bitmap and than we calculate the right coordinates.
                                    Bitmap originalBitmap = ((BitmapDrawable) drawRectImageView.getDrawable()).getBitmap();
                                    int o_width = originalBitmap.getWidth();
                                    int o_height = originalBitmap.getHeight();
                                    int my_width = drawRectImageView.getWidth();
                                    int my_height = drawRectImageView.getHeight();

                                    float drawnRectLeft = drawnRect.left;
                                    float drawnRectTop = drawnRect.top;
                                    float drawnRectWidth = drawnRect.width();
                                    float drawnRectHeight = drawnRect.height();


                                    float xScaleFactor = o_width / (float) my_width;
                                    float yScaleFactor = o_height / (float) my_height;


                                    int x = (int) (drawnRectLeft * xScaleFactor);
                                    int y = (int) (drawnRectTop * yScaleFactor);
                                    int width = (int) (drawnRectWidth * xScaleFactor);
                                    int height = (int) (drawnRectHeight * yScaleFactor);

                                    Log.d("Coordinates", "x: " + x + ", y: " + y + ", width: " + width + ", height: " + height);

                                    // Ensure that the cropping rectangle does not exceed the bounds of the original image
                                    x = Math.max(0, Math.min(x, originalBitmap.getWidth() - 1));
                                    y = Math.max(0, Math.min(y, originalBitmap.getHeight() - 1));
                                    width = Math.max(1, Math.min(width, originalBitmap.getWidth() - x));
                                    height = Math.max(1, Math.min(height, originalBitmap.getHeight() - y));

                                    Log.d("Adjusted Coordinates", "x: " + x + ", y: " + y + ", width: " + width + ", height: " + height);

                                    // Crop the original image
                                    Bitmap croppedBitmap = Bitmap.createBitmap(originalBitmap, x, y, width, height);

                                    // Convert the cropped bitmap to a byte array
                                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                                    croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                                    byte[] croppedImageData = outputStream.toByteArray();



                                    // Now you have the cropped image data, you can send it to the server
                                    CompletableFuture<Integer> i = sendCroppedImageToServer(croppedImageData);
                                    i.thenAccept(ret->{
                                        if (ret == 1){
                                            closeCamera();
                                        }
                                    });
                                } else {
                                    Log.d("Coordinates", "drawnRect is null");
                                }
                            });

                        });
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

    /**
     *  send the cropped image to the server and get the response.
     * @param croppedImageData the image we want to send.
     * @return future that we got the res from the server.
     */

    private CompletableFuture<Integer> sendCroppedImageToServer(byte[] croppedImageData){
        CompletableFuture<Integer> i = new CompletableFuture<>();
        new Thread(() -> {
            try {

                OkHttpClient client = new OkHttpClient();
                MediaType MEDIA_TYPE_JPEG = MediaType.parse("image/jpeg");

                // Encode the byte array using Base64
                encodedImage = Base64.encodeToString(croppedImageData, Base64.DEFAULT);

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

                String responseBody = response.body().string();

                JSONObject jsonResponse = new JSONObject(responseBody);

                res = jsonResponse.getString("predicted_fruit");


                Log.d("ServerResponse", res);
                i.complete(1);

            } catch (IOException e) {
                Log.e("SendImage", "Error sending image to server", e);
                //change it
                i.complete(1);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }).start();
        return i;


    }


    /**
     * function to display to the user the picture that he took so he can draw rec.
     * @param imageData the image we want to show.
     * @return future that we displayed it.
     */
    private CompletableFuture<Integer> displayCapturedImage(byte[] imageData) {
        CompletableFuture<Integer> i = new CompletableFuture<>();
        // Decode the byte array into a Bitmap
        Bitmap capturedBitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);

        if (capturedBitmap != null) {
            // Get the rotation degrees
            int rotationDegrees = getRotationDegrees(imageData);

            // Rotate the bitmap if necessary
            if (rotationDegrees != 0) {
                Matrix matrix = new Matrix();
                matrix.postRotate(rotationDegrees);
                final Bitmap rotatedBitmap = Bitmap.createBitmap(capturedBitmap, 0, 0, capturedBitmap.getWidth(), capturedBitmap.getHeight(), matrix, true);

                // Use a Handler to post UI update operation to the main thread
                new Handler(Looper.getMainLooper()).post(() -> {
                    PreviewView previewView = findViewById(R.id.previewView);
                    previewView.setVisibility(View.GONE);
                    // Display the Bitmap in an ImageView
                    ImageView capturedImageView = findViewById(R.id.res);
                    capturedImageView.setImageBitmap(rotatedBitmap);
                    capturedImageView.setVisibility(View.VISIBLE);
                });
            } else {
                // Use a Handler to post UI update operation to the main thread
                new Handler(Looper.getMainLooper()).post(() -> {
                    PreviewView previewView = findViewById(R.id.previewView);
                    previewView.setVisibility(View.GONE);
                    // Display the Bitmap in an ImageView
                    ImageView capturedImageView = findViewById(R.id.res);
                    capturedImageView.setImageBitmap(capturedBitmap);
                    capturedImageView.setVisibility(View.VISIBLE);
                    // Set appropriate scale type
                    capturedImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                });
            }
            i.complete(1);
        }
        return  i;
    }

    /**
     *  check if the image need to rotate and if so do it so the user will be able to see it right.
     * @param imageData the image that the user took
     * @return the degrees.
     */
    private int getRotationDegrees(byte[] imageData) {
        try {
            ExifInterface exifInterface = new ExifInterface(new ByteArrayInputStream(imageData));
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            Log.d("Orientation", "Original Orientation: " + orientation);

            int rotationDegrees = 0;
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotationDegrees = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotationDegrees = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotationDegrees = 270;
                    break;
            }
            Log.d("Orientation", "Rotation Degrees: " + rotationDegrees);
            return rotationDegrees;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }


    /**
     * request camera Permission from the user so we can use the phone camera.
     */

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

    /**
     * convert the image to byte array so we can use it and show it to the user.
     * @param imageProxy the image that the user took.
     * @return the pic in byte.
     */
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



//    private String extractBase64Image(String jsonResponse) {
//        // Assuming the JSON response is in the format {"processed_image":"<base64_string>"}
//        return jsonResponse.split("\"processed_image\":\"")[1].split("\"")[0];
//    }

    /**
     * close the camera and open new intent after saving the cropped pic for the rest of the app.
     */
    private void closeCamera() {
        if (cameraProvider != null) {
            File oe = saveBase64StringToFile(encodedImage);
            // Start the DisplayImageActivity
            Intent intent = new Intent(LoginActivity.this, DisplayImageActivity.class);
            intent.putExtra("original", oe.getAbsolutePath());
            intent.putExtra("type",res);
            startActivity(intent);
            cameraProvider.unbindAll();
            cameraProvider = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        handler.removeCallbacksAndMessages(null); // Remove all pending callbacks and messages
    }

    /**
     * save the image so we will be able to show it to the user at the new event.
      * @param base64String the image.
     * @return path to the file.
     */
private File saveBase64StringToFile(String base64String) {
    File storageDir = getFilesDir();
    File base64File = new File(storageDir, "encoded_image.jpg");

    try {
        byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);
        FileOutputStream fos = new FileOutputStream(base64File);
        fos.write(decodedBytes);
        fos.close();
    } catch (IOException e) {
        Log.e("SaveBase64String", "Error saving Base64 string to file", e);
    }

    return base64File;
}

    @Override
    protected void onResume() {
        super.onResume();
        TextView text = findViewById(R.id.wait);
        text.setVisibility(View.GONE);
        sendButton.setVisibility(View.GONE);
        cameraButton.setVisibility(View.VISIBLE);
        TextView t = findViewById(R.id.textView);
        t.setVisibility(View.VISIBLE);
        Button send  = findViewById(R.id.send);
        send.setVisibility(View.GONE);
        res = null;
        DrawRectImageView drawRectImageView = findViewById(R.id.res);
        drawRectImageView.setVisibility(View.GONE);
        drawRectImageView.clearDrawnRect();


    }

}
