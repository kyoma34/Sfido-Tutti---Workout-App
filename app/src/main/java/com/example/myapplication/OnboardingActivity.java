package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OnboardingActivity extends AppCompatActivity {

    // Step 1 views
    EditText etUsername, etAge, etWeight, etHeight;
    RadioGroup rgGender;
    RadioButton rbMale, rbFemale;

    // Step 2 views
    RadioGroup rgActivity;
    RadioButton rbNotActive, rbSlightly, rbActive, rbVeryActive;

    // Step 3 views
    android.widget.CheckBox cbLoseWeight, cbMaintainWeight, cbGainWeight,
            cbGainMuscle, cbCutBodyFat, cbManageStress;

    // Navigation
    Button btnCancel, btnNext;
    View step1Layout, step2Layout, step3Layout;
    int currentStep = 1;

    // Data
    String username, gender, activityLevel;
    int age, weight, height;
    List<String> goals = new ArrayList<>();

    FirebaseFirestore db;
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Step layouts
        step1Layout = findViewById(R.id.step1Layout);
        step2Layout = findViewById(R.id.step2Layout);
        step3Layout = findViewById(R.id.step3Layout);

        // Step 1
        etUsername = findViewById(R.id.etUsername);
        etAge = findViewById(R.id.etAge);
        etWeight = findViewById(R.id.etWeight);
        etHeight = findViewById(R.id.etHeight);
        rgGender = findViewById(R.id.rgGender);
        rbMale = findViewById(R.id.rbMale);
        rbFemale = findViewById(R.id.rbFemale);

        // Step 2
        rgActivity = findViewById(R.id.rgActivity);
        rbNotActive = findViewById(R.id.rbNotActive);
        rbSlightly = findViewById(R.id.rbSlightly);
        rbActive = findViewById(R.id.rbActive);
        rbVeryActive = findViewById(R.id.rbVeryActive);

        // Step 3
        cbLoseWeight = findViewById(R.id.cbLoseWeight);
        cbMaintainWeight = findViewById(R.id.cbMaintainWeight);
        cbGainWeight = findViewById(R.id.cbGainWeight);
        cbGainMuscle = findViewById(R.id.cbGainMuscle);
        cbCutBodyFat = findViewById(R.id.cbCutBodyFat);
        cbManageStress = findViewById(R.id.cbManageStress);

        btnCancel = findViewById(R.id.btnCancel);
        btnNext = findViewById(R.id.btnNext);

        showStep(1);

        btnNext.setOnClickListener(v -> {
            if (currentStep == 1) {
                if (validateStep1()) showStep(2);
            } else if (currentStep == 2) {
                if (validateStep2()) showStep(3);
            } else if (currentStep == 3) {
                if (validateStep3()) saveToFirestore();
            }
        });

        btnCancel.setOnClickListener(v -> {
            if (currentStep == 1) finish();
            else showStep(currentStep - 1);
        });

        ImageButton btnClose = findViewById(R.id.btnClose);
        btnClose.setOnClickListener(v -> finish());
    }

    void showStep(int step) {
        currentStep = step;
        step1Layout.setVisibility(step == 1 ? View.VISIBLE : View.GONE);
        step2Layout.setVisibility(step == 2 ? View.VISIBLE : View.GONE);
        step3Layout.setVisibility(step == 3 ? View.VISIBLE : View.GONE);
        btnNext.setText(step == 3 ? "Finish" : "Next");
        btnCancel.setText(step == 1 ? "Cancel" : "Back");
    }

    boolean validateStep1() {
        username = etUsername.getText().toString().trim();
        String ageStr = etAge.getText().toString().trim();
        String weightStr = etWeight.getText().toString().trim();
        String heightStr = etHeight.getText().toString().trim();

        if (username.isEmpty() || ageStr.isEmpty() || weightStr.isEmpty() || heightStr.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (rgGender.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Please select your gender", Toast.LENGTH_SHORT).show();
            return false;
        }
        age = Integer.parseInt(ageStr);
        weight = Integer.parseInt(weightStr);
        height = Integer.parseInt(heightStr);
        gender = rbMale.isChecked() ? "Male" : "Female";
        return true;
    }

    boolean validateStep2() {
        if (rgActivity.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Please select your activity level", Toast.LENGTH_SHORT).show();
            return false;
        }
        int id = rgActivity.getCheckedRadioButtonId();
        if (id == R.id.rbNotActive) activityLevel = "Not Very Active";
        else if (id == R.id.rbSlightly) activityLevel = "Slightly Active";
        else if (id == R.id.rbActive) activityLevel = "Active";
        else activityLevel = "Very Active";
        return true;
    }

    boolean validateStep3() {
        goals.clear();
        if (cbLoseWeight.isChecked()) goals.add("Lose Weight");
        if (cbMaintainWeight.isChecked()) goals.add("Maintain Weight");
        if (cbGainWeight.isChecked()) goals.add("Gain Weight");
        if (cbGainMuscle.isChecked()) goals.add("Gain Muscle");
        if (cbCutBodyFat.isChecked()) goals.add("Cut Body Fat");
        if (cbManageStress.isChecked()) goals.add("Manage Stress");

        if (goals.isEmpty()) {
            Toast.makeText(this, "Please select at least one goal", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    void saveToFirestore() {
        btnNext.setEnabled(false);
        btnNext.setText("Saving...");

        String uid = mAuth.getCurrentUser().getUid();

        Map<String, Object> userData = new HashMap<>();
        userData.put("username", username);
        userData.put("age", age);
        userData.put("weight", weight);
        userData.put("height", height);
        userData.put("gender", gender);
        userData.put("activityLevel", activityLevel);
        userData.put("goals", goals);
        userData.put("onboardingComplete", true);

        db.collection("users").document(uid)
                .set(userData)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Let's go! 🔥", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(OnboardingActivity.this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    btnNext.setEnabled(true);
                    btnNext.setText("Finish");
                });
    }
}