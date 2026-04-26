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

import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private LinearLayout programContainer;
    private TextView tvCurrentDate, tvDayLabel;
    private Calendar currentCalendar;

    // Day names matching what's stored in Firestore
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
            loadProgramsForDay();
        });

        findViewById(R.id.btnNextDay).setOnClickListener(v -> {
            currentCalendar.add(Calendar.DAY_OF_YEAR, 1);
            updateDateDisplay();
            loadProgramsForDay();
        });

        // Bottom Navigation
        findViewById(R.id.navAddProgram).setOnClickListener(v ->
                startActivity(new Intent(this, AddProgramActivity.class))
        );

        findViewById(R.id.navProfile).setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class))
        );

        updateDateDisplay();
        loadProgramsForDay();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProgramsForDay();
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
        int dow = currentCalendar.get(Calendar.DAY_OF_WEEK); // 1=Sun ... 7=Sat
        return FULL_DAYS[dow - 1];
    }

    private void loadProgramsForDay() {
        if (mAuth.getCurrentUser() == null) return;

        programContainer.removeAllViews();
        String dayName = getSelectedDayName();
        String userId = mAuth.getCurrentUser().getUid();

        // Show loading
        TextView tvLoading = new TextView(this);
        tvLoading.setText("Loading your programs…");
        tvLoading.setTextColor(0xFF9CA3AF);
        tvLoading.setTextSize(14f);
        tvLoading.setPadding(0, 24, 0, 0);
        programContainer.addView(tvLoading);

        // Filter by userId AND dayName
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
                    programContainer.removeAllViews();
                    TextView tvErr = new TextView(this);
                    tvErr.setText("⚠️ Failed to load: " + e.getMessage());
                    tvErr.setTextColor(0xFFFF6B6B);
                    tvErr.setTextSize(13f);
                    programContainer.addView(tvErr);
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

        TextView emoji = new TextView(this);
        emoji.setText("🏖️");
        emoji.setTextSize(40f);
        emoji.setGravity(Gravity.CENTER);

        TextView msg = new TextView(this);
        msg.setText("No programs for " + dayName);
        msg.setTextColor(0xFF9CA3AF);
        msg.setTextSize(15f);
        msg.setGravity(Gravity.CENTER);

        TextView hint = new TextView(this);
        hint.setText("Tap + Add Program to create one");
        hint.setTextColor(0xFF4B5563);
        hint.setTextSize(13f);
        hint.setGravity(Gravity.CENTER);
        hint.setPadding(0, 8, 0, 0);

        empty.addView(emoji);
        empty.addView(msg);
        empty.addView(hint);
        programContainer.addView(empty);
    }

    @SuppressWarnings("unchecked")
    private void addProgramCard(DocumentSnapshot doc) {
        String name = doc.getString("name");
        String programId = doc.getId();
        List<Map<String, Object>> exercises = (List<Map<String, Object>>) doc.get("exercises");
        int exCount = exercises != null ? exercises.size() : 0;

        // Inflate card
        View card = LayoutInflater.from(this).inflate(R.layout.item_program_card, programContainer, false);

        TextView tvName = card.findViewById(R.id.tvProgramName);
        TextView tvExCount = card.findViewById(R.id.tvExerciseCount);
        Button btnStart = card.findViewById(R.id.btnStartWorkout);
        LinearLayout exerciseList = card.findViewById(R.id.exerciseList);
        ImageView btnDelete = card.findViewById(R.id.btnDeleteProgram);

        tvName.setText(name != null ? name : "Unnamed Program");
        tvExCount.setText(exCount + " exercise" + (exCount != 1 ? "s" : ""));

        if (exercises != null) {
            for (Map<String, Object> ex : exercises) {
                TextView tvEx = new TextView(this);
                String exName = (String) ex.get("name");
                Object sets = ex.get("sets");
                Object reps = ex.get("reps");
                Object restSets = ex.get("restBetweenSets");
                String detail = "• " + exName + "  —  " + sets + " × " + reps + " reps  |  rest: " + restSets + "s";
                tvEx.setText(detail);
                tvEx.setTextColor(0xFF9CA3AF);
                tvEx.setTextSize(12f);
                tvEx.setPadding(0, 4, 0, 4);
                exerciseList.addView(tvEx);
            }
        }

        btnStart.setOnClickListener(v ->
                Toast.makeText(this, "Starting: " + name, Toast.LENGTH_SHORT).show()
        );

        // Delete Logic
        if (btnDelete != null) {
            btnDelete.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("Delete Program")
                        .setMessage("Are you sure you want to delete '" + name + "'?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            db.collection("programs").document(programId)
                                    .delete()
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(MainActivity.this, "Program deleted", Toast.LENGTH_SHORT).show();
                                        loadProgramsForDay(); // Refresh list
                                    })
                                    .addOnFailureListener(e -> Toast.makeText(MainActivity.this, "Error deleting: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        programContainer.addView(card);
    }
}
