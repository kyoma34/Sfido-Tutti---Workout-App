package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvProfileName, tvProfileEmail;
    private Button btnLogout;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfileEmail = findViewById(R.id.tvProfileEmail);
        btnLogout = findViewById(R.id.btnLogout);

        if (currentUser != null) {
            tvProfileEmail.setText(currentUser.getEmail());

            // Fetch name from Firestore
            db.collection("users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        tvProfileName.setText(documentSnapshot.getString("name"));
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ProfileActivity.this, "Error fetching profile", Toast.LENGTH_SHORT).show();
                });
        }

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

        findViewById(R.id.navProfile).setOnClickListener(v -> {
            // Already on profile
        });
    }
}