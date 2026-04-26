package com.example.myapplication;

import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.*;

public class WorkoutActivity extends AppCompatActivity {

    private enum State { READY, EXERCISING, RESTING_SET, RESTING_EXERCISE }
    private State currentState = State.READY;

    private List<Map<String, Object>> exercises;
    private int currentExerciseIndex = 0;
    private int currentSet = 1;
    private int setsCompletedTotal = 0;
    private int totalExercisesFullyCompleted = 0;
    private long workoutStartTime;
    private long exerciseStartTime;
    private float userWeightKg = 70f;
    
    private TextView tvExerciseName, tvSetInfo, tvRepsInfo, tvTimer, tvTimerLabel, tvWorkoutTitle;
    private Button btnAction;
    private CountDownTimer countDownTimer;
    private Ringtone ringtone;
    private FirebaseFirestore db;
    private String programId, programName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout);

        db = FirebaseFirestore.getInstance();

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            db.collection("users").document(uid).get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Long weight = documentSnapshot.getLong("weight");
                    if (weight != null && weight > 0) {
                        userWeightKg = weight.floatValue();
                    }
                }
            });
        }
        
        Intent intent = getIntent();
        exercises = (List<Map<String, Object>>) intent.getSerializableExtra("exercises");
        programId = intent.getStringExtra("programId");
        programName = intent.getStringExtra("programName");

        tvWorkoutTitle = findViewById(R.id.tvWorkoutTitle);
        tvExerciseName = findViewById(R.id.tvExerciseName);
        tvSetInfo = findViewById(R.id.tvSetInfo);
        tvRepsInfo = findViewById(R.id.tvRepsInfo);
        tvTimer = findViewById(R.id.tvTimer);
        tvTimerLabel = findViewById(R.id.tvTimerLabel);
        btnAction = findViewById(R.id.btnAction);

        if (programName != null) {
            tvWorkoutTitle.setText(programName.toUpperCase());
        }

        workoutStartTime = System.currentTimeMillis();
        updateUI();

        btnAction.setOnClickListener(v -> handleAction());
        findViewById(R.id.btnAbort).setOnClickListener(v -> showAbortDialog());
    }

    private void handleAction() {
        stopAlarm();
        switch (currentState) {
            case READY:
                startExercise();
                break;
            case EXERCISING:
                finishSet();
                break;
            case RESTING_SET:
            case RESTING_EXERCISE:
                stopTimer();
                startExercise();
                break;
        }
    }

    private void startExercise() {
        currentState = State.EXERCISING;
        exerciseStartTime = System.currentTimeMillis();
        btnAction.setText("FINISH SET");
        tvTimerLabel.setText("DURATION");
        startDurationTimer();
        updateUI();
    }

    private void finishSet() {
        stopTimer();
        setsCompletedTotal++;
        
        Map<String, Object> currentEx = exercises.get(currentExerciseIndex);
        int totalSetsInCurrentEx = getIntFromMap(currentEx, "sets", 1);

        if (currentSet < totalSetsInCurrentEx) {
            currentSet++;
            int restTime = getIntFromMap(currentEx, "restBetweenSets", 60);
            startRestTimer(restTime, State.RESTING_SET);
        } else {
            totalExercisesFullyCompleted++;
            if (currentExerciseIndex < exercises.size() - 1) {
                currentExerciseIndex++;
                currentSet = 1;
                int restExTime = getIntFromMap(currentEx, "restBetweenExercises", 90);
                startRestTimer(restExTime, State.RESTING_EXERCISE);
            } else {
                finishWorkout(true);
            }
        }
    }

    private void startDurationTimer() {
        if (countDownTimer != null) countDownTimer.cancel();
        countDownTimer = new CountDownTimer(3600000, 1000) {
            public void onTick(long millisUntilFinished) {
                long seconds = (System.currentTimeMillis() - exerciseStartTime) / 1000;
                tvTimer.setText(formatTime(seconds));
            }
            public void onFinish() {}
        }.start();
    }

    private void startRestTimer(int seconds, State nextState) {
        currentState = nextState;
        tvTimerLabel.setText("REST TIME");
        btnAction.setText("SKIP REST");
        updateUI();

        if (countDownTimer != null) countDownTimer.cancel();
        countDownTimer = new CountDownTimer(seconds * 1000, 1000) {
            public void onTick(long millisUntilFinished) {
                tvTimer.setText(formatTime(millisUntilFinished / 1000));
            }
            public void onFinish() {
                playAlarm();
                btnAction.setText("START NEXT SET");
            }
        }.start();
    }

    private void stopTimer() {
        if (countDownTimer != null) countDownTimer.cancel();
    }

    private void updateUI() {
        Map<String, Object> ex = exercises.get(currentExerciseIndex);
        tvExerciseName.setText((String) ex.get("name"));
        tvSetInfo.setText("Set " + currentSet + " of " + getIntFromMap(ex, "sets", 1));
        tvRepsInfo.setText("Target: " + getIntFromMap(ex, "reps", 10) + " Reps");
    }

    private String formatTime(long totalSeconds) {
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    private void playAlarm() {
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (ringtone == null) {
                ringtone = RingtoneManager.getRingtone(getApplicationContext(), notification);
            }
            if (!ringtone.isPlaying()) {
                ringtone.play();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void stopAlarm() {
        if (ringtone != null && ringtone.isPlaying()) {
            ringtone.stop();
        }
    }

    private void finishWorkout(boolean completed) {
        stopAlarm();
        stopTimer();
        long totalDuration = (System.currentTimeMillis() - workoutStartTime) / 1000;
        int calories = calculateCalories(totalDuration);
        
        // Calculate completion percentage based on total sets
        int totalSetsInProgram = 0;
        for (Map<String, Object> ex : exercises) {
            totalSetsInProgram += getIntFromMap(ex, "sets", 1);
        }
        int percentage = (int) ((setsCompletedTotal / (float) totalSetsInProgram) * 100);

        Map<String, Object> progress = new HashMap<>();
        progress.put("userId", FirebaseAuth.getInstance().getUid());
        progress.put("programId", programId);
        progress.put("programName", programName);
        progress.put("durationSeconds", totalDuration);
        progress.put("calories", calories);
        progress.put("date", new Date());
        progress.put("completed", completed);
        progress.put("percentage", percentage);
        progress.put("exerciseCount", totalExercisesFullyCompleted);

        db.collection("workout_history").add(progress);

        Intent intent = new Intent(this, FinishWorkoutActivity.class);
        intent.putExtra("duration", formatTime(totalDuration));
        intent.putExtra("calories", calories);
        intent.putExtra("exerciseCount", totalExercisesFullyCompleted);
        intent.putExtra("percentage", percentage);
        startActivity(intent);
        finish();
    }

    private int calculateCalories(long totalSeconds) {
        float totalCalories = 0;
        if (exercises == null) return 1;

        for (Map<String, Object> ex : exercises) {
            String name = (String) ex.get("name");
            int sets = getIntFromMap(ex, "sets", 3);
            int reps = getIntFromMap(ex, "reps", 10);
            int restBetweenSets = getIntFromMap(ex, "restBetweenSets", 60);
            int restBetweenExercises = getIntFromMap(ex, "restBetweenExercises", 90);

            float activeSeconds = sets * reps * 3f;
            float restSeconds = (sets - 1) * restBetweenSets + restBetweenExercises;

            float met = ExerciseMET.getMET(name);

            float activeCalories = met * userWeightKg * (activeSeconds / 3600f);
            float restCalories = 1.5f * userWeightKg * (restSeconds / 3600f);

            totalCalories += (activeCalories + restCalories);
        }

        return Math.max(1, Math.round(totalCalories));
    }

    private void showAbortDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Abort Workout")
                .setMessage("Are you sure? Your progress will be saved.")
                .setPositiveButton("Abort", (d, w) -> {
                    finishWorkout(false);
                })
                .setNegativeButton("Keep going", null)
                .show();
    }

    private int getIntFromMap(Map<String, Object> map, String key, int defaultVal) {
        Object val = map.get(key);
        if (val instanceof Long) return ((Long) val).intValue();
        if (val instanceof Integer) return (Integer) val;
        return defaultVal;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTimer();
        stopAlarm();
    }
}
