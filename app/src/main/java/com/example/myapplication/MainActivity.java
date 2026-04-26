package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private LinearLayout programContainer;
    private TextView tvCurrentDate, tvDayLabel;
    private Calendar currentCalendar;
    private Set<String> completedProgramIds = new HashSet<>();

    private static final String[] FULL_DAYS = {
            "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        androidx.activity.EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentCalendar = Calendar.getInstance();

        tvCurrentDate = findViewById(R.id.tvCurrentDate);
        tvDayLabel = findViewById(R.id.tvDayLabel);
        programContainer = findViewById(R.id.programContainer);

        findViewById(R.id.btnPrevDay).setOnClickListener(v -> {
            currentCalendar.add(Calendar.DAY_OF_YEAR, -1);
            updateDateDisplay();
            loadHistoryAndPrograms();
        });

        findViewById(R.id.btnNextDay).setOnClickListener(v -> {
            currentCalendar.add(Calendar.DAY_OF_YEAR, 1);
            updateDateDisplay();
            loadHistoryAndPrograms();
        });

        findViewById(R.id.navAddProgram).setOnClickListener(v ->
                startActivity(new Intent(this, AddProgramActivity.class))
        );

        findViewById(R.id.navProfile).setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class))
        );

        updateDateDisplay();
        loadHistoryAndPrograms();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadHistoryAndPrograms();
    }

    private void updateDateDisplay() {
        SimpleDateFormat dateFmt = new SimpleDateFormat("EEE, MMM d", Locale.getDefault());
        tvCurrentDate.setText(dateFmt.format(currentCalendar.getTime()));

        boolean isToday = isSameDay(currentCalendar, Calendar.getInstance());
        tvDayLabel.setText(isToday ? "Today" : "");
        tvDayLabel.setVisibility(isToday ? View.VISIBLE : View.GONE);
    }

    private boolean isSameDay(Calendar c1, Calendar c2) {
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)
                && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR);
    }

    private String getSelectedDayName() {
        int dow = currentCalendar.get(Calendar.DAY_OF_WEEK);
        return FULL_DAYS[dow - 1];
    }

    private void loadHistoryAndPrograms() {
        if (mAuth.getCurrentUser() == null) return;
        String userId = mAuth.getCurrentUser().getUid();

        // Simplified query to avoid index errors: only filter by userId
        // We will filter by date and completion in Java code
        db.collection("workout_history")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    completedProgramIds.clear();
                    
                    long startMillis = getStartOfDayMillis(currentCalendar);
                    long endMillis = getEndOfDayMillis(currentCalendar);

                    for (DocumentSnapshot doc : querySnapshot) {
                        Boolean completed = doc.getBoolean("completed");
                        Date date = doc.getDate("date");
                        String progId = doc.getString("programId");

                        if (Boolean.TRUE.equals(completed) && date != null && progId != null) {
                            long time = date.getTime();
                            if (time >= startMillis && time <= endMillis) {
                                completedProgramIds.add(progId);
                            }
                        }
                    }
                    loadProgramsForDay();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "History Sync Error", Toast.LENGTH_SHORT).show();
                    loadProgramsForDay();
                });
    }

    private long getStartOfDayMillis(Calendar cal) {
        Calendar c = (Calendar) cal.clone();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private long getEndOfDayMillis(Calendar cal) {
        Calendar c = (Calendar) cal.clone();
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        c.set(Calendar.MILLISECOND, 999);
        return c.getTimeInMillis();
    }

    private void loadProgramsForDay() {
        programContainer.removeAllViews();
        String dayName = getSelectedDayName();
        String userId = mAuth.getCurrentUser().getUid();

        db.collection("programs")
                .whereEqualTo("userId", userId)
                .whereArrayContains("days", dayName)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    programContainer.removeAllViews();
                    if (querySnapshot.isEmpty()) {
                        showEmptyState(dayName);
                        return;
                    }
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        addProgramCard(doc);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Programs failed to load", Toast.LENGTH_SHORT).show();
                });
    }

    private void showEmptyState(String dayName) {
        LinearLayout empty = new LinearLayout(this);
        empty.setOrientation(LinearLayout.VERTICAL);
        empty.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 48, 0, 0);
        empty.setLayoutParams(lp);

        TextView msg = new TextView(this);
        msg.setText("No programs for " + dayName);
        msg.setTextColor(0xFF9CA3AF);
        msg.setTextSize(15f);
        empty.addView(msg);
        programContainer.addView(empty);
    }

    @SuppressWarnings("unchecked")
    private void addProgramCard(DocumentSnapshot doc) {
        String name = doc.getString("name");
        String programId = doc.getId();
        List<Map<String, Object>> exercises = (List<Map<String, Object>>) doc.get("exercises");
        int exCount = exercises != null ? exercises.size() : 0;

        View card = LayoutInflater.from(this).inflate(R.layout.item_program_card, programContainer, false);
        boolean isCompleted = completedProgramIds.contains(programId);

        TextView tvName = card.findViewById(R.id.tvProgramName);
        TextView tvExCount = card.findViewById(R.id.tvExerciseCount);
        Button btnStart = card.findViewById(R.id.btnStartWorkout);
        ImageView btnDelete = card.findViewById(R.id.btnDeleteProgram);
        LinearLayout exerciseList = card.findViewById(R.id.exerciseList);

        tvName.setText(name != null ? name : "Unnamed");
        tvExCount.setText(exCount + " exercise" + (exCount != 1 ? "s" : ""));

        // Add exercises list to UI
        if (exercises != null) {
            for (Map<String, Object> ex : exercises) {
                TextView tvEx = new TextView(this);
                String exName = (String) ex.get("name");
                Object sets = ex.get("sets");
                Object reps = ex.get("reps");
                tvEx.setText("• " + exName + " (" + sets + "x" + reps + ")");
                tvEx.setTextColor(0xFF9CA3AF);
                tvEx.setTextSize(13);
                tvEx.setPadding(0, 2, 0, 2);
                exerciseList.addView(tvEx);
            }
        }
        
        if (isCompleted) {
            card.setAlpha(0.5f);
            btnStart.setText("COMPLETED ✅");
            btnStart.setEnabled(false);
            btnStart.setBackgroundTintList(null); 
        }

        btnStart.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, WorkoutActivity.class);
            intent.putExtra("exercises", (Serializable) exercises);
            intent.putExtra("programId", programId);
            intent.putExtra("programName", name);
            startActivity(intent);
        });

        btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Delete Program")
                    .setMessage("Delete '" + name + "'?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        db.collection("programs").document(programId).delete()
                                .addOnSuccessListener(aVoid -> loadHistoryAndPrograms());
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        programContainer.addView(card);
    }
}
