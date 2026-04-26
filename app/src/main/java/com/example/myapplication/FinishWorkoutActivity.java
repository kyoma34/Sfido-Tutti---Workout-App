package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class FinishWorkoutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_finish_workout);

        TextView tvStatDuration = findViewById(R.id.tvStatDuration);
        TextView tvStatCalories = findViewById(R.id.tvStatCalories);
        TextView tvStatExercises = findViewById(R.id.tvStatExercises);
        TextView tvCongrats = findViewById(R.id.tvCongrats);
        Button btnDone = findViewById(R.id.btnDone);

        Intent intent = getIntent();
        String duration = intent.getStringExtra("duration");
        int calories = intent.getIntExtra("calories", 0);
        int exerciseCount = intent.getIntExtra("exerciseCount", 0);
        int percentage = intent.getIntExtra("percentage", 0);

        tvStatDuration.setText(duration);
        tvStatCalories.setText(String.valueOf(calories));
        tvStatExercises.setText(String.valueOf(exerciseCount));
        
        if (percentage < 100) {
            tvCongrats.setText(percentage + "% Completed");
        } else {
            tvCongrats.setText("Workout Complete!");
        }

        btnDone.setOnClickListener(v -> {
            Intent mainIntent = new Intent(this, MainActivity.class);
            mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(mainIntent);
            finish();
        });
    }
}