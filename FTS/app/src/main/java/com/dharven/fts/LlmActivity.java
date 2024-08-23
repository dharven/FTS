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
import android.content.res.AssetFileDescriptor;
import android.graphics.BitmapFactory;
import android.util.Base64;
import org.tensorflow.lite.Interpreter;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
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
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LlmActivity extends AppCompatActivity implements SensorEventListener {
    private TextView outputTextView;
    private ImageView camera;
    private EditText stepsEditText;
    private ProgressBar progressBar;
    private String base64Image;
    private ChatGPTClient chatGPTClient;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;

    private double height = 0, weight = 0, calories = 0, goalWeight = 0, age = 0;
    FirebaseFirestore db;
    LinearLayout QuestionLayout;
    Button YesButton, NoButton;
    CollectionReference historyRef;
    Map<String, Object> historyData;
    Advise advise;
    private SensorManager sensorManager;
    private boolean running = false;
    private float totalSteps = 0f;
    private float previousTotalSteps = 0f;
    private long lastSavedDate;
    private User user;
    private ImageButton backButton;
    LinearLayout questionLayout;

    // TensorFlow Lite interpreter
    private Interpreter tflite;
    private List<String> labelList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_llm);
        Toast.makeText(this, "Successfully logged in.", Toast.LENGTH_SHORT).show();

        // Initialize UI elements
        outputTextView = findViewById(R.id.output);
        MaterialButton testApiButton = findViewById(R.id.testApi);
        YesButton = findViewById(R.id.yes);
        NoButton = findViewById(R.id.no);
        questionLayout = findViewById(R.id.questionLayout);
        camera = findViewById(R.id.camera);
        stepsEditText = findViewById(R.id.steps);
        progressBar = findViewById(R.id.progressBar);
        backButton = findViewById(R.id.back_button);
        QuestionLayout = findViewById(R.id.questionLayout);
        db = FirebaseFirestore.getInstance();
        chatGPTClient = new ChatGPTClient();
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        // Load previous data
        loadData();
        checkIfNewDay();

        // Load TensorFlow Lite model
        try {
            tflite = new Interpreter(loadModelFile("food_model.tflite"));
            labelList = loadLabelList();
            Toast.makeText(this, "Model loaded successfully.", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error loading model: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        // Handle intent data
        Intent intent = getIntent();
        if (intent != null) {
            user = intent.getParcelableExtra("user");
            height = user.getHeight();
            age = user.getAge();
            weight = user.getWeight();
            goalWeight = user.getGoalWeight();
        }

        // Set up camera button listener
        camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(LlmActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    // Request camera permission
                    ActivityCompat.requestPermissions(LlmActivity.this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
                } else {
                    // Open camera if permission granted
                    openCamera();
                }
            }
        });

        // Set up back button listener
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Set up test API button listener
        testApiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (base64Image == null) {
                    Toast.makeText(LlmActivity.this, "Please take a picture first", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (stepsEditText.getText().toString().isEmpty()) {
                    Toast.makeText(LlmActivity.this, "Please enter the number of steps", Toast.LENGTH_SHORT).show();
                    return;
                }

                progressBar.setVisibility(View.VISIBLE);
                processImage(base64Image);  // Process image using TensorFlow Lite
            }
        });

        // Set up "No" button listener
        NoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stepsEditText.setText("");
                outputTextView.setText("");
                base64Image = null;
                questionLayout.setVisibility(View.GONE);
            }
        });

        // Set up "Yes" button listener
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

    // Open camera method
    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
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
            Toast.makeText(this, "Image captured successfully.", Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("NewApi")
    private String convertImageToBase64FromCam(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();

        // Updated to use android.util.Base64
        return android.util.Base64.encodeToString(byteArray, android.util.Base64.DEFAULT);
    }

    // Method to process the captured image using TensorFlow Lite
    private void processImage(String base64Image) {
        // Convert base64 to Bitmap
        byte[] decodedString = android.util.Base64.decode(base64Image, android.util.Base64.DEFAULT);
        Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

        // Preprocess the image
        ByteBuffer inputBuffer = convertBitmapToByteBuffer(decodedBitmap);

        // Run inference
        float[][] output = new float[1][10];
        try {
            tflite.run(inputBuffer, output);

        } catch (Exception e) {
            e.printStackTrace();

            Log.e("LlmActivity", "Inference error", e);
            return;
        }

        // Handle only the first two classes ("apple" and "banana")
        float[] relevantOutput = Arrays.copyOfRange(output[0], 0, 2);

        // Get the predicted label
        int predictedLabel = argmax(output[0]);
        String foodItem = labelList.get(predictedLabel);

        // Fetch food info and display the result
        fetchFoodInfo(foodItem);
    }

    private int argmax(float[] array) {
        int maxIndex = -1;
        float maxValue = Float.MIN_VALUE;
        for (int i = 0; i < array.length; i++) {
            if (array[i] > maxValue) {
                maxValue = array[i];
                maxIndex = i;
            }
        }
        return maxIndex;
    }

    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        // Ensure the bitmap is resized to the required size (e.g., 224x224)
        bitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true);

        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * 224 * 224 * 3);
        byteBuffer.order(ByteOrder.nativeOrder());
        int[] intValues = new int[224 * 224];
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        int pixel = 0;
        for (int i = 0; i < 224; ++i) {
            for (int j = 0; j < 224; ++j) {
                int val = intValues[pixel++];
                byteBuffer.putFloat(((val >> 16) & 0xFF) / 255.0f);
                byteBuffer.putFloat(((val >> 8) & 0xFF) / 255.0f);
                byteBuffer.putFloat((val & 0xFF) / 255.0f);
            }
        }

        return byteBuffer;
    }

    private MappedByteBuffer loadModelFile(String modelPath) throws IOException {
        AssetFileDescriptor fileDescriptor = this.getAssets().openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();

        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private List<String> loadLabelList() {
        // Ensure you have 2 labels to match the output of your model
        return Arrays.asList("apple","banana");
    }


    private void fetchFoodInfo(String foodItem) {
        progressBar.setVisibility(View.GONE);

        String foodInfo = "";
        String calories = "";

        switch (foodItem) {
            case "apple":
                calories = "52 kcal per 100g";
                foodInfo = "A nutritious fruit high in fiber.";
                break;
            case "banana":
                calories = "89 kcal per 100g";
                foodInfo = "A popular fruit high in potassium.";
                break;
            default:
                foodInfo = "Unknown food item.";
                calories = "Unknown calories.";
                break;
        }

        if (foodItem.equals("Unknown food item.")) {
            outputTextView.setText("Unable to identify the food item. Please use the ChatGPT LLM to check your food item");
            Toast.makeText(this, "Food item not recognized.", Toast.LENGTH_LONG).show();
        } else {
            outputTextView.setText("Food: " + foodItem + "\n" + foodInfo + "\nCalories: " + calories);
            QuestionLayout.setVisibility(View.VISIBLE);
            Toast.makeText(this, "Food item recognized: " + foodItem, Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        running = true;

        // Get the step counter sensor
        Sensor stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        if (stepSensor == null) {
            Toast.makeText(this, "No sensor detected on this device", Toast.LENGTH_SHORT).show();
        } else {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (running) {
            totalSteps = event.values[0];
            int currentSteps = (int) (totalSteps - previousTotalSteps);
            stepsEditText.setText(String.valueOf(currentSteps));
            Toast.makeText(this, "Current steps: " + currentSteps, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // No implementation needed for step counting
    }

    private void saveData(long date, float steps) {
        SharedPreferences sharedPreferences = getSharedPreferences("myPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong("lastSavedDate", date);
        editor.putFloat("previousTotalSteps", steps);
        editor.apply();
        Toast.makeText(this, "Data saved locally.", Toast.LENGTH_SHORT).show();
    }

    private void loadData() {
        SharedPreferences sharedPreferences = getSharedPreferences("myPrefs", Context.MODE_PRIVATE);
        previousTotalSteps = sharedPreferences.getFloat("previousTotalSteps", 0f);
        lastSavedDate = sharedPreferences.getLong("lastSavedDate", 0);

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
