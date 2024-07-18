package com.example.fruits;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import android.os.Bundle;
import android.text.TextUtils;

import android.util.Base64;
import android.util.Log;

import android.view.View;

import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.speech.tts.TextToSpeech;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Locale;


import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class DisplayImageActivity extends AppCompatActivity {
    private static final String SERVER_URL = "https://5cc3-132-70-66-11.ngrok-free.app/review";
    private String emailAddress;

    private boolean isDialogShown = false;
    private TextToSpeech textToSpeech;
    private Bitmap bitmap;



    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display);

        String original = getIntent().getStringExtra("original");
        String type = getIntent().getStringExtra("type");

        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            /**
             * this is override for the init of TextToSpeech API it checks if it got right status of sucsses and than try to set the
             * voice lang to ENG and get status again if its worked and supported
             */
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = textToSpeech.setLanguage(Locale.US);
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTS", "Language not supported");
                    }
                } else {
                    Log.e("TTS", "Initialization failed");
                }
            }
        });



        // Display the image in the ImageView
        ImageView imageView = findViewById(R.id.response);
        File imgFile = new File(original);
        if (imgFile.exists()) {
            Log.d("DisplayImageActivity", "Image file exists: " + imgFile.getAbsolutePath());

            // Load bitmap from file
            bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());


            // Set bitmap to ImageView
            imageView.setImageBitmap(bitmap);
        } else {
            Log.e("DisplayImageActivity", "Image file not found: " + imgFile.getAbsolutePath());
        }
        TextView rec = findViewById(R.id.modelAnswer);
        if (type != null && !type.isEmpty()){
            rec.setText(type);
//
        }



        // Set onClickListener for the buttons
        ImageButton good_rec = findViewById(R.id.good_rec);
        good_rec.setOnClickListener(v -> finish());

        ImageButton bad_rec = findViewById(R.id.bad_rec);
        bad_rec.setOnClickListener(v -> askToHelpImprove());


        OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true) {
            /**
             * this function handle the case that if the user pressed return it finishes this intent and return to login.
             */
            @Override
            public void handleOnBackPressed() {
                finish(); // Uncomment this line if you want to finish the activity on back press
            }
        };
        getOnBackPressedDispatcher().addCallback(this, onBackPressedCallback);
        ImageButton speaks = findViewById(R.id.voice);
        speaks.setOnClickListener(v -> {
            if (type != null) {
                speakText(type);
            }
        });

    }

    /**
     * this function read the string that it gets loud in the phone.
     * @param text that should be spoken.
     */
    private void speakText(String text) {
        Log.d("TTS", "Speaking text: " + text);
        if (textToSpeech != null) {
            textToSpeech.setSpeechRate(0.8f);
            int result = textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
            if (result == TextToSpeech.ERROR) {
                Log.e("TTS", "Error while speaking text");
            }
        } else {
            Log.e("TTS", "TextToSpeech instance is null");
        }
    }






    /**
     * after pressing the bad rec button ask him to help to improve and write his email adress so he will get
     * email when the model will update.
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
                        askTheRightRec(recognition -> {
                            if (!recognition.isEmpty()) {
                                sendRightTotheServer(emailAddress,recognition);
                                finish();
                            }
                        });


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
     *  convert the bitmap image to byte so we can encode it to base64 to send it to the server
     * @param bitmap the picture
     * @return the picture in byte[]
     */
    public byte[] convertBitmapToByteArray(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }


    /**
     * This function is called when the detection is not good so we send to the server the right recognition that the
     * user entered and his email and the picture so the model will be able to update.
     * @param email the user email address
     * @param rightRec the user  right recognition.
     */
    private void sendRightTotheServer(String email,String rightRec) {
        // Create OkHttpClient instance
        OkHttpClient client = new OkHttpClient();
        String encodedImage = Base64.encodeToString(convertBitmapToByteArray(bitmap), Base64.DEFAULT);



        // Create a JSON object to hold the data to be sent
        JSONObject postData = new JSONObject();
        try {
            postData.put("email", email);
            postData.put("original", encodedImage);
            postData.put("rightRec",rightRec);
            /////////
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
        finish();
    }


    /**
     * Here we ask the user to give the right recognition to the fruit so we will able to send it to the server.
     * @param callback for the function that called this function.
     */

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


    /**
     * Callback interface for returning recognition string.
     */
    interface RecognitionCallback {
        void onRecognitionProvided(String recognition);
    }




}
