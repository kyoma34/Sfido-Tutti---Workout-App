package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.text.SimpleDateFormat;
import java.util.*;

public class ProfileActivity extends AppCompatActivity {

    private EditText etUsername, etAge, etWeight, etHeight, etGender, etActivityLevel;
    private CheckBox cbLoseWeight, cbMaintainWeight, cbGainWeight, cbGainMuscle, cbCutBodyFat, cbManageStress;
    private TextView tvProfileName, tvProfileEmail;
    private Button btnSaveChanges, btnLogout;
    private WeightChartView weightChartView;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String uid;
    private float initialWeight = -1f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        uid = mAuth.getUid();

        // Bind Views
        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfileEmail = findViewById(R.id.tvProfileEmail);
        etUsername = findViewById(R.id.etUsername);
        etAge = findViewById(R.id.etAge);
        etWeight = findViewById(R.id.etWeight);
        etHeight = findViewById(R.id.etHeight);
        etGender = findViewById(R.id.etGender);
        etActivityLevel = findViewById(R.id.etActivityLevel);

        cbLoseWeight = findViewById(R.id.cbLoseWeight);
        cbMaintainWeight = findViewById(R.id.cbMaintainWeight);
        cbGainWeight = findViewById(R.id.cbGainWeight);
        cbGainMuscle = findViewById(R.id.cbGainMuscle);
        cbCutBodyFat = findViewById(R.id.cbCutBodyFat);
        cbManageStress = findViewById(R.id.cbManageStress);

        weightChartView = findViewById(R.id.weightChartView);
        btnSaveChanges = findViewById(R.id.btnSaveChanges);
        btnLogout = findViewById(R.id.btnLogout);

        if (mAuth.getCurrentUser() != null) {
            tvProfileEmail.setText(mAuth.getCurrentUser().getEmail());
        }

        loadUserData();
        loadWeightHistory();

        btnSaveChanges.setOnClickListener(v -> saveUserData());

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Toast.makeText(ProfileActivity.this, "Logged out", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // Navigation
        findViewById(R.id.navHome).setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
        findViewById(R.id.navAddProgram).setOnClickListener(v -> {
            startActivity(new Intent(this, AddProgramActivity.class));
            finish();
        });
        findViewById(R.id.navTrack).setOnClickListener(v -> {
            startActivity(new Intent(this, TrackProgressActivity.class));
            finish();
        });
        findViewById(R.id.navProfile).setOnClickListener(v -> { /* already here */ });
    }

    private void loadUserData() {
        if (uid == null) return;
        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                String name = doc.getString("name");
                tvProfileName.setText(name);
                etUsername.setText(name);

                etAge.setText(String.valueOf(doc.get("age") != null ? doc.get("age") : ""));
                
                Object wObj = doc.get("weight");
                if (wObj != null) {
                    initialWeight = ((Number) wObj).floatValue();
                    etWeight.setText(String.valueOf(initialWeight));
                }

                etHeight.setText(String.valueOf(doc.get("height") != null ? doc.get("height") : ""));
                etGender.setText(doc.getString("gender"));
                etActivityLevel.setText(doc.getString("activityLevel"));

                List<String> goals = (List<String>) doc.get("goals");
                if (goals != null) {
                    if (goals.contains(cbLoseWeight.getText().toString())) cbLoseWeight.setChecked(true);
                    if (goals.contains(cbMaintainWeight.getText().toString())) cbMaintainWeight.setChecked(true);
                    if (goals.contains(cbGainWeight.getText().toString())) cbGainWeight.setChecked(true);
                    if (goals.contains(cbGainMuscle.getText().toString())) cbGainMuscle.setChecked(true);
                    if (goals.contains(cbCutBodyFat.getText().toString())) cbCutBodyFat.setChecked(true);
                    if (goals.contains(cbManageStress.getText().toString())) cbManageStress.setChecked(true);
                }
            }
        });
    }

    private void saveUserData() {
        String name = etUsername.getText().toString().trim();
        String weightStr = etWeight.getText().toString().trim();

        if (name.isEmpty() || weightStr.isEmpty()) {
            Toast.makeText(this, "Name and Weight are required", Toast.LENGTH_SHORT).show();
            return;
        }

        float newWeight = Float.parseFloat(weightStr);
        List<String> goals = new ArrayList<>();
        if (cbLoseWeight.isChecked()) goals.add(cbLoseWeight.getText().toString());
        if (cbMaintainWeight.isChecked()) goals.add(cbMaintainWeight.getText().toString());
        if (cbGainWeight.isChecked()) goals.add(cbGainWeight.getText().toString());
        if (cbGainMuscle.isChecked()) goals.add(cbGainMuscle.getText().toString());
        if (cbCutBodyFat.isChecked()) goals.add(cbCutBodyFat.getText().toString());
        if (cbManageStress.isChecked()) goals.add(cbManageStress.getText().toString());

        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        try { map.put("age", Integer.parseInt(etAge.getText().toString())); } catch (Exception e) {}
        map.put("weight", newWeight);
        try { map.put("height", Integer.parseInt(etHeight.getText().toString())); } catch (Exception e) {}
        map.put("gender", etGender.getText().toString());
        map.put("activityLevel", etActivityLevel.getText().toString());
        map.put("goals", goals);
        map.put("onboardingComplete", true);

        if (newWeight != initialWeight) {
            Map<String, Object> history = new HashMap<>();
            history.put("userId", uid);
            history.put("weight", newWeight);
            history.put("date", new Date());
            db.collection("weight_history").add(history);
            initialWeight = newWeight;
        }

        db.collection("users").document(uid).update(map)
            .addOnSuccessListener(aVoid -> {
                tvProfileName.setText(name);
                Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show();
                loadWeightHistory();
            })
            .addOnFailureListener(e -> Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void loadWeightHistory() {
        if (uid == null) return;
        db.collection("weight_history")
            .whereEqualTo("userId", uid)
            .orderBy("date", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                List<Float> weights = new ArrayList<>();
                List<String> labels = new ArrayList<>();
                SimpleDateFormat sdf = new SimpleDateFormat("MMM d", Locale.getDefault());

                for (DocumentSnapshot doc : querySnapshot) {
                    weights.add(((Number) doc.get("weight")).floatValue());
                    labels.add(sdf.format(doc.getDate("date")));
                }

                if (weights.size() < 2 && initialWeight > 0) {
                    weights.add(0, initialWeight);
                    labels.add(0, "Start");
                }

                weightChartView.setData(weights, labels);
            });
    }
}
