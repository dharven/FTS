package com.dharven.fts;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText fullNameEditText, emailEditText, passwordEditText, confirmPasswordEditText, age, weight, goalWeight, height;
    private Button registerButton;
    private TextView loginTextView;

    String emailPattern = "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}";
    String passwordPattern = "^(?=.*[0-9])(?=.*[^a-zA-Z0-9])(.{6,})$";
    FirebaseAuth mAuth;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        fullNameEditText = findViewById(R.id.fullName);
        emailEditText = findViewById(R.id.email);
        passwordEditText = findViewById(R.id.password);
        confirmPasswordEditText = findViewById(R.id.confirmPassword);
        registerButton = findViewById(R.id.register);
        loginTextView = findViewById(R.id.click_to_login);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        height = findViewById(R.id.height);
        age = findViewById(R.id.Age);
        weight = findViewById(R.id.weight);
        goalWeight = findViewById(R.id.GoalWeight);

        loginTextView.setOnClickListener(v -> {
            finish();
        });

        registerButton.setOnClickListener(v -> {
            registerUser();
        });
    }

    private void registerUser() {
        String fullName = fullNameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();

        if (TextUtils.isEmpty(fullName)) {
            Toast.makeText(this, "Please enter full name", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!email.matches(emailPattern)) {
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Please enter email", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please enter password", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.matches(passwordPattern)) {
            Toast.makeText(this, "Password must be at least 6 characters long and contain at least one number and one special character.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!TextUtils.equals(password, confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        if (validateFields()) {
            saveUserRegistrationData(fullName, email, password);
        }
    }

    private void saveUserRegistrationData(String fullName, String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Map<String, Object> user = new HashMap<>();
                        user.put("name", fullName);
                        user.put("email", email);
                        user.put("age", Integer.parseInt(age.getText().toString()));
                        user.put("height", Double.parseDouble(height.getText().toString()));
                        user.put("weight", Double.parseDouble(weight.getText().toString()));
                        user.put("goalWeight",Double.parseDouble(goalWeight.getText().toString()));
                        db.collection("users").document(email)
                                .set(user)
                                .addOnSuccessListener(aVoid -> Toast.makeText(RegisterActivity.this, "Registration successful", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e -> Toast.makeText(RegisterActivity.this, "Registration failed", Toast.LENGTH_SHORT).show());

                        finish();
                    } else {
                        Toast.makeText(RegisterActivity.this, "Email already exist", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private boolean validateFields() {
        boolean isValid = true;

        // Validate height
        if (height.getText().toString().isEmpty()) {
            height.setError("Height is required");
            isValid = false;
        } else {
            try {
                float heightValue = Float.parseFloat(height.getText().toString());
                if (heightValue <= 0 || heightValue > 300) { // Assuming height is in cm
                    height.setError("Please enter a valid height in cm");
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                height.setError("Please enter a numeric value");
                isValid = false;
            }
        }

        // Validate age
        if (age.getText().toString().isEmpty()) {
            age.setError("Age is required");
            isValid = false;
        } else {
            try {
                int ageValue = Integer.parseInt(age.getText().toString());
                if (ageValue <= 0 || ageValue > 120) { // Assuming a reasonable age range
                    age.setError("Please enter a valid age");
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                age.setError("Please enter a numeric value");
                isValid = false;
            }
        }

        // Validate weight
        if (weight.getText().toString().isEmpty()) {
            weight.setError("Weight is required");
            isValid = false;
        } else {
            try {
                float weightValue = Float.parseFloat(weight.getText().toString());
                if (weightValue <= 0 || weightValue > 500) { // Assuming weight is in kg
                    weight.setError("Please enter a valid weight in kg");
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                weight.setError("Please enter a numeric value");
                isValid = false;
            }
        }

        // Validate goal weight
        if (goalWeight.getText().toString().isEmpty()) {
            goalWeight.setError("Goal weight is required");
            isValid = false;
        } else {
            try {
                float goalWeightValue = Float.parseFloat(goalWeight.getText().toString());
                if (goalWeightValue <= 0 || goalWeightValue > 500) { // Assuming weight is in kg
                    goalWeight.setError("Please enter a valid goal weight in kg");
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                goalWeight.setError("Please enter a numeric value");
                isValid = false;
            }
        }

        return isValid;
    }
}
