package com.dharven.fts;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.dharven.fts.repository.Advise;
import com.dharven.fts.repository.ApiRequest;
import com.dharven.fts.repository.ChatGPTClient;
import com.dharven.fts.repository.User;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    FirebaseFirestore db;
    private TextView outputTextView;
    private ImageView camera;
    private EditText stepsEditText;
    private RadioGroup weightRadioGroup;
    private RadioButton weightGainRadioButton;
    private RadioButton weightLoseeRadioButton;
    private ProgressBar progressBar;
    private String base64Image;
    private ChatGPTClient chatGPTClient;
    private ImageButton menuButton;
    private double height = 0, weight = 0, calories = 0, goalWeight = 0, age = 0;
    MaterialButton YesButton, NoButton;
    CollectionReference historyRef;
    Map<String, Object> historyData;
    Advise advise;
    LinearLayout questionLayout;
    private SensorManager sensorManager;
    private boolean running = false;
    private float totalSteps = 0f;
    private float previousTotalSteps = 0f;

    private long lastSavedDate;
    private static final int PERMISSION_REQUEST_CODE = 101;

    User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toast.makeText(this, "Successfully logged in.", Toast.LENGTH_SHORT).show();

        outputTextView = findViewById(R.id.output);
        MaterialButton testApiButton = findViewById(R.id.testApi);

        YesButton = findViewById(R.id.yes);
        NoButton = findViewById(R.id.no);
        db = FirebaseFirestore.getInstance();

//        Intent stepCounterIntent = new Intent(this, StepCounterService.class);
//        startService(stepCounterIntent);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        loadData();
        checkIfNewDay();

//        resetSteps();
//        Intent stepCounterIntent = new Intent(this, StepCounterService.class);
//        startService(stepCounterIntent);
        chatGPTClient = new ChatGPTClient();
        camera = findViewById(R.id.camera);
        stepsEditText = findViewById(R.id.steps);
        weightRadioGroup = findViewById(R.id.radioGroup);
        weightGainRadioButton = findViewById(R.id.radioButton);
        weightLoseeRadioButton = findViewById(R.id.radioButton2);
        progressBar = findViewById(R.id.progressBar);
        menuButton = findViewById(R.id.menuButton);
        questionLayout = findViewById(R.id.questionLayout);

        if (!hasRequiredPermissions()) {
            showPermissionDialog();
        }

        camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    // Permission not granted, request it
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
                } else {
                    // Permission already granted, proceed to open camera
                    openCamera();
                }
            }
        });


        testApiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // sendApiRequest();
                if (base64Image == null) {
                    Toast.makeText(MainActivity.this, "Please take a picture first", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (stepsEditText.getText().toString().isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please enter the number of steps", Toast.LENGTH_SHORT).show();
                    return;
                }
                progressBar.setVisibility(View.VISIBLE);
                sendApiRequest(base64Image);
            }
        });

        menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPopupMenu(v);
            }
        });

        NoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stepsEditText.setText("");
                outputTextView.setText("");
                base64Image = null;
                questionLayout.setVisibility(View.GONE);
            }
        });

        FirebaseFirestore.getInstance().collection("users").document(getSharedPreferences("MyPrefs", MODE_PRIVATE).getString("email", "")).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()) {
                    user = documentSnapshot.toObject(User.class);
                    weight = user.getWeight();
                    goalWeight = user.getGoalWeight();
                    age = user.getAge();
                    height = user.getHeight();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

            }
        });

        YesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                historyData.put("calories", advise.getCalories());
                historyData.put("advise", advise.getAdvise());
                historyData.put("date", System.currentTimeMillis());
                historyData.put("steps", Integer.parseInt(stepsEditText.getText().toString()));
                // Create a new document with an auto-generated ID
                historyRef.add(historyData)
                        .addOnSuccessListener(documentReference -> {
                            // Document successfully written
                            Log.d("Firebase", "DocumentSnapshot added with ID: " + documentReference.getId());
                            stepsEditText.setText("");
                            outputTextView.setText("");
                            base64Image = null;
                            questionLayout.setVisibility(View.GONE);
                        })
                        .addOnFailureListener(e -> {
                            // Error writing document
                            Log.w("Firebase", "Error writing document", e);
                        });
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        running = true;

        // Get the step counter sensor
        Sensor stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        if (stepSensor == null) {
            // No sensor found, show a toast
            Toast.makeText(this, "No sensor detected on this device", Toast.LENGTH_SHORT).show();
        } else {
            // Register the listener
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (running) {
            totalSteps = event.values[0];

            // Calculate the current steps by subtracting the previous total steps
            int currentSteps = (int) (totalSteps - previousTotalSteps);

            // Update the UI with the current steps
            stepsEditText.setText(String.valueOf(currentSteps));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // We do not need to implement this for step counting
    }

//    private void resetSteps() {
//        stepsEditText.setOnClickListener(v ->
//                Toast.makeText(MainActivity.this, "Long tap to reset steps", Toast.LENGTH_SHORT).show());
//
//        stepsEditText.setOnLongClickListener(v -> {
//            previousTotalSteps = totalSteps;
//
//            // Reset the steps to 0
//            stepsEditText.setText(String.valueOf(0));
//
//            // Save the new step count
//            saveData();
//
//            return true;
//        });
//    }

    private void saveData(long date, float steps) {
        SharedPreferences sharedPreferences = getSharedPreferences("myPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong("lastSavedDate", date);
        editor.putFloat("previousTotalSteps", steps);
        editor.apply();
    }

    private void loadData() {
        SharedPreferences sharedPreferences = getSharedPreferences("myPrefs", Context.MODE_PRIVATE);
        previousTotalSteps = sharedPreferences.getFloat("previousTotalSteps", 0f);
        lastSavedDate = sharedPreferences.getLong("lastSavedDate", 0);
    }


    // Method to open the camera
    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Camera permission granted, proceed to open camera
                openCamera();
            } else {
                // Camera permission denied, show a message or handle the denial
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            base64Image = convertImageToBase64FromCam(imageBitmap);
            Toast.makeText(this, "Image added successfully", Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("NewApi")
    private String convertImageToBase64FromCam(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.getEncoder().encodeToString(byteArray);
    }

    private boolean hasRequiredPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED;
    }

    private void showPermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Permissions Required")
                .setMessage("This app requires Body Sensors and Physical Activity permissions to function properly. Please enable them in the app settings.")
                .setPositiveButton("OK", (dialog, which) -> openAppSettings())
                .setCancelable(false)
                .show();
    }

    private void openAppSettings() {
        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(android.net.Uri.fromParts("package", getPackageName(), null));
        startActivity(intent);
    }

    private void showPopupMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenuInflater().inflate(R.menu.menu, popup.getMenu());
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.history:
                        // Handle history action
                        startActivity(new Intent(MainActivity.this, HistoryActivity.class));
                        return true;
                    case R.id.profile:
                        // Handle profile action
                        startActivity(new Intent(MainActivity.this, StatsActivity.class));
                        return true;
                    case R.id.logout:
                        // Handle logout action
                        FirebaseAuth.getInstance().signOut();
                        startActivity(new Intent(MainActivity.this, LoginActivity.class));
                        return true;
                    default:
                        return false;
                }
            }
        });
        popup.show();
    }

    private void sendApiRequest(String base64Image) {
        // Convert the image to a base64 string

        // Create content list with text and image
        List<ApiRequest.Message.Content> contentList = Arrays.asList(
                new ApiRequest.Message.Content("text", getPromt()),
                new ApiRequest.Message.Content("image_url", new ApiRequest.Message.Content.ImageUrl("data:image/jpeg;base64," + base64Image))
        );

        // Create the message with the content list
        ApiRequest.Message message = new ApiRequest.Message("user", contentList);

        // Create the request
        ApiRequest request = new ApiRequest("gpt-4o", Arrays.asList(message));

        // Send the request and handle the response
        chatGPTClient.getChatResponse(request, new ChatGPTClient.ChatResponseCallback() {
            @Override
            public void onSuccess(String reply) {
                progressBar.setVisibility(View.GONE);
                Gson gson = new Gson();
                advise = gson.fromJson(reply.replace("```json", "").replace("```", ""), Advise.class);
                //
                CollectionReference usersRef = db.collection("users");

                // Get a reference to the history collection within the user document
                historyRef = usersRef.document(getSharedPreferences("MyPrefs", MODE_PRIVATE).getString("email", "")).collection("history");
                historyData = new HashMap<>();
                calories = advise.getCalories();
                if (calories < 0) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("Item not found!");
                    builder.setMessage("Please use our in-app feature to get info about this food item.");
                    builder.setPositiveButton("OK", (dialog, which) -> {
                        Intent intent = new Intent(MainActivity.this, LlmActivity.class);
                        intent.putExtra("user", user);
                        startActivity(intent);
                        dialog.dismiss();
                    });
                    builder.setNegativeButton("Cancel", (dialog, which) -> {
                        dialog.dismiss();
                    });
                    builder.show();
                    return;
                }
                outputTextView.setText("Calories: " + advise.getCalories() + "\n\n" + advise.getAdvise());
                questionLayout.setVisibility(View.VISIBLE);

            }

            @Override
            public void onError(String errorMessage) {
                outputTextView.setText(errorMessage);
            }
        });

    }



    private String getPromt() {
        StringBuilder prompt = new StringBuilder("You are a fitness trainer, and work as a professional trainer. I'll ask you for your advise and with my weight, height, and total calories eaten today (these data is subject to availability)"
                + "But you'll surely get a picture of what I am going to eat, how many steps I have walked today, and if I want to gain or lose weight. You have to advise me if this food item will help me in achieving my goal. If not, give me alternate options. But if you find picture of apple or banana just reply \"Not found\" and give calories -1 OTHERWISE FOR ALL OTHER FOOD YOU'LL PROVIDE CALORIES AND DATA" +
                "And Major point, you'll send me this data in JSON code with format of {'calories':[calories of the image that I sent you if you don't know exact number, just return probably possible number otherwise app would crash], 'advise': [your advise as a professional fitness trainer]}" +
                "\n\nToday I am going to eat what is in the image attached and I have walked these many steps, steps: ")
                .append(stepsEditText.getText().toString())
                .append(" and I want to ")
                .append(". please advise me if this food item will help me in achieving my goal. if no give me alternate options.");

        if (height != 0) {
            prompt.append("\n\nMy height is: ")
                    .append(height)
                    .append(" cm");
        }

        if (weight != 0) {
            prompt.append("\nMy weight is: ")
                    .append(weight)
                    .append(" kg");
        }

        if (goalWeight != 0) {
            prompt.append("\nMy goal weight is " + goalWeight + " Kg");
        }

        if (age != 0){
            prompt.append("\nMy age is " + age + " years old");
        }

        if (calories != 0) {
            prompt.append("\nTotal calories eaten today: ")
                    .append(calories);
        }

        return prompt.toString();
    }

    private void checkIfNewDay() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(lastSavedDate);
        int lastSavedDay = calendar.get(Calendar.DAY_OF_YEAR);

        calendar.setTimeInMillis(System.currentTimeMillis());
        int today = calendar.get(Calendar.DAY_OF_YEAR);

        if (today != lastSavedDay) {
            previousTotalSteps = totalSteps;
            saveData(System.currentTimeMillis(), previousTotalSteps);
        }
    }
}
