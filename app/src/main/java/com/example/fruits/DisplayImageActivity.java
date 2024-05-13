package com.example.fruits;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.util.DisplayMetrics;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class DisplayImageActivity extends AppCompatActivity {
    private String original;
    List<Pair<Float, Float>> userChoice = new ArrayList<>();
    List<Pair<Float, Float>> badReview = new ArrayList<>();
    private static final String SERVER_URL = "http://172.20.10.8:3000/review";
    private static final String SERVER_URL2 = "http://172.20.10.8:3000/oneImage";
    private ImageView imageView;
    private String emailAddress;
    private List<String> reviews = new ArrayList<>();
    private boolean isDialogShown = false;



    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display);

        // Retrieve the image data from intent extras
        String imageData = getIntent().getStringExtra("imageData");
        original = getIntent().getStringExtra("original");
        Button done = findViewById(R.id.sendServer);
        done.setOnClickListener(v -> sendForRec());

        // Display the image in the ImageView
         imageView = findViewById(R.id.response);
        if (imageData != null && !imageData.isEmpty()) {
            // Set the image directly to ImageView using the URI
            imageView.setImageURI(Uri.parse(imageData));
            imageView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent e) {
                    // Calculate relative coordinates
                    float x = e.getX();
                    float y = e.getY();
                    Pair<Float, Float> clicked = new Pair<>(x, y);
                    userChoice.add(clicked);
                    return true; // Indicates that the touch event has been handled

                }
            });
        }


        // Set onClickListener for the buttons
        ImageButton good_rec = findViewById(R.id.good_rec);
        good_rec.setOnClickListener(v -> finish());

        ImageButton bad_rec = findViewById(R.id.bad_rec);
        bad_rec.setOnClickListener(v -> askToHelpImprove());


    }

    /**
     * after pressing the bad rec button ask him to help to improve.
     */

    @SuppressLint("SetTextI18n")
    private void askToHelpImprove() {
        // Create a dialog for inputting email address
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_email_input, null);
        EditText editTextEmail = dialogView.findViewById(R.id.editTextEmail);
        builder.setView(dialogView)
                .setTitle("Receive email notification")
                .setMessage("Would you like to receive an email when the issue is repaired?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Get the email address entered by the user
                    emailAddress = editTextEmail.getText().toString().trim();
                    // Validate email address (optional)
                    if (isValidEmail(emailAddress)) {
                        // Send the email or perform other actions
                        Toast.makeText(DisplayImageActivity.this, "Great!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        getWrongDetections();
                    } else {
                        askToHelpImprove();
                        Toast.makeText(DisplayImageActivity.this, "Invalid email address", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("No", (dialog, which) -> {
                    // Finish the activity
                    finish();
                })
                .show();
        ImageView good = findViewById(R.id.good_rec);
        ImageView bad = findViewById(R.id.bad_rec);
        good.setVisibility(View.GONE);
        bad.setVisibility(View.GONE);
        Button done = findViewById(R.id.sendServer);
        done.setVisibility(View.VISIBLE);
        TextView view = findViewById(R.id.tap);
        view.setText("Choose all the wrong recognitions");
        view.setVisibility(View.VISIBLE);
    }

    /**
     * check if the email is valid.
     * @param target what the user entered.
     * @return bool if it is valid or not.
     */
    private boolean isValidEmail(CharSequence target) {
        return !TextUtils.isEmpty(target) && android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
    }

    /**
     *  send the email address and the user bad review to the server with the original picture.
     * @param email
     */
    private void sendToTheServer(String email) {
        // Create OkHttpClient instance
        OkHttpClient client = new OkHttpClient();

        // Assume you have a server endpoint where you send data

        // Create a JSON object to hold the data to be sent
        JSONObject postData = new JSONObject();
        try {
            postData.put("email", email);
            postData.put("originalImageData", original);
        } catch (JSONException e) {
            e.printStackTrace();
            return; // Return early if there's an error creating JSON data
        }

        // Create a request body with the JSON data
        RequestBody requestBody = RequestBody.create(postData.toString(), MediaType.parse("application/json"));

        // Build the request
        Request request = new Request.Builder()
                .url(SERVER_URL)
                .post(requestBody)
                .build();

        // Enqueue the request asynchronously
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                }

                String responseData = response.body().string();
            }
        });
    }

    /**
     * send to the server all the coordinates that the user pressed and the original picture so he will get
     *  the rec of each fruit.
     */
    private void sendForRec() {
        // Get screen dimensions
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        float screenWidth = displayMetrics.widthPixels;

        // Prepare JSON object with user choices and original data
        JSONObject requestData = new JSONObject();
        try {
            // Add original data
            requestData.put("original", original);

            // Add user choices as an array of tuples with relative coordinates
            JSONArray userChoicesArray = new JSONArray();
            for (Pair<Float, Float> pair : userChoice) {
                float r_x = pair.first / screenWidth;  // Calculate relative x-coordinate
                float r_y = pair.second / dpToPx();  // Calculate relative y-coordinate
                JSONArray tuple = new JSONArray();
                tuple.put(r_x);
                tuple.put(r_y);
                userChoicesArray.put(tuple);
            }
            requestData.put("user_choices", userChoicesArray);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }
        // Create OkHttp client
        OkHttpClient client = new OkHttpClient();

// Create request body with JSON data
        RequestBody requestBody = RequestBody.create( requestData.toString(),MediaType.parse("application/json"));
// Create HTTP request
        Request request = new Request.Builder()
                .url(SERVER_URL2)
                .post(requestBody)
                .build();

        // Execute the request asynchronously
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
                ////// לבטלללללללללללללל
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ImageView good = findViewById(R.id.good_rec);
                        ImageView bad = findViewById(R.id.bad_rec);
                        good.setVisibility(View.VISIBLE);
                        bad.setVisibility(View.VISIBLE);
                        Button done = findViewById(R.id.sendServer);
                        done.setVisibility(View.GONE);
                        TextView view = findViewById(R.id.tap);
                        view.setVisibility(View.GONE);
                    }
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                }

                // Get the response body as a byte array
                byte[] imageData = response.body().bytes();

                // Decode the byte array into a Bitmap
                Bitmap bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);

                // Update UI on the main thread
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Set the Bitmap to the ImageView
                        imageView.setImageBitmap(bitmap);
                        ImageView good = findViewById(R.id.good_rec);
                        ImageView bad = findViewById(R.id.bad_rec);
                        good.setVisibility(View.VISIBLE);
                        bad.setVisibility(View.VISIBLE);
                        Button done = findViewById(R.id.sendServer);
                        done.setVisibility(View.GONE);
                        TextView view = findViewById(R.id.tap);
                        view.setVisibility(View.GONE);
                    }
                });
            }
        });




    }

    /**
     * Function that converts dp to pixels
     * @return converted pixels
     */

    private int dpToPx() {
        float density = getResources().getDisplayMetrics().density;
        return Math.round((float) 250 * density);
    }
    @SuppressLint("ClickableViewAccessibility")
    private void getWrongDetections() {
        imageView.setOnTouchListener((v, e) -> {
            askTheRightRec(recognition -> {
                if (!recognition.isEmpty()) {
                    // Calculate relative coordinates
                    float x = e.getX();
                    float y = e.getY();
                    Pair<Float, Float> clicked = new Pair<>(x, y);
                    userChoice.add(clicked);
                    reviews.add(recognition);
                }
            });
            return true; // Indicates that the touch event has been handled
        });

        Button done = findViewById(R.id.sendServer);
        done.setOnClickListener(v -> sendToTheServer(emailAddress));
    }

    @SuppressLint("SetTextI18n")
    private void askTheRightRec(RecognitionCallback callback) {
        // Create a dialog for inputting recognition
        if (!isDialogShown) { // Add this check
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View dialogView = getLayoutInflater().inflate(R.layout.dialog, null);
            EditText editTextRecognition = dialogView.findViewById(R.id.rightRec);
            builder.setView(dialogView)
                    .setTitle("Right Recognition")
                    .setMessage("Please write the right Recognition")
                    .setPositiveButton("Done", (dialog, which) -> {
                        // Get the recognition string entered by the user
                        String recognition = editTextRecognition.getText().toString().trim();
                        callback.onRecognitionProvided(recognition);
                        dialog.dismiss(); // Dismiss the dialog here
                        isDialogShown = false; // Reset the flag after dismissing the dialog
                    })
                    .setNegativeButton("Return", (dialog, which) -> {
                        dialog.dismiss();
                        isDialogShown = false; // Reset the flag after dismissing the dialog
                    });

            AlertDialog dialog = builder.create();
            dialog.show();
            isDialogShown = true; // Set the flag to true indicating that the dialog is shown
        }
    }




    // Callback interface for returning recognition string
    interface RecognitionCallback {
        void onRecognitionProvided(String recognition);
    }




}
