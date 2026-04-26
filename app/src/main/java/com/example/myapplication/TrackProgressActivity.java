package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.SimpleDateFormat;
import java.util.*;

public class TrackProgressActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private LinearLayout historyContainer;
    private TextView tvTotalWorkouts, tvTotalCalories, tvTotalMinutes, tvAvgCompletion, tvStreakCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_progress);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        tvTotalWorkouts  = findViewById(R.id.tvTotalWorkouts);
        tvTotalCalories  = findViewById(R.id.tvTotalCalories);
        tvTotalMinutes   = findViewById(R.id.tvTotalMinutes);
        tvAvgCompletion  = findViewById(R.id.tvAvgCompletion);
        tvStreakCount    = findViewById(R.id.tvStreakCount);
        historyContainer = findViewById(R.id.historyContainer);

        // Nav
        findViewById(R.id.navHome).setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
        findViewById(R.id.navAddProgram).setOnClickListener(v -> {
            startActivity(new Intent(this, AddProgramActivity.class));
            finish();
        });
        findViewById(R.id.navTrack).setOnClickListener(v -> { /* already here */ });
        findViewById(R.id.navProfile).setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
            finish();
        });

        loadStats();
    }

    private void loadStats() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        db.collection("workout_history")
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    int totalWorkouts = 0;
                    long totalCalories = 0;
                    long totalSeconds = 0;
                    long totalPercentage = 0;
                    int completedCount = 0;
                    List<Map<String, Object>> history = new ArrayList<>();
                    Set<String> completedDays = new TreeSet<>();

                    SimpleDateFormat dayFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

                    for (QueryDocumentSnapshot doc : snapshot) {
                        totalWorkouts++;
                        Long cal = doc.getLong("calories");
                        Long dur = doc.getLong("durationSeconds");
                        Long pct = doc.getLong("percentage");
                        Boolean completed = doc.getBoolean("completed");
                        Date date = doc.getDate("date");

                        if (cal != null) totalCalories += cal;
                        if (dur != null) totalSeconds += dur;
                        if (pct != null) totalPercentage += pct;
                        if (Boolean.TRUE.equals(completed)) {
                            completedCount++;
                            if (date != null) completedDays.add(dayFmt.format(date));
                        }

                        Map<String, Object> item = new HashMap<>();
                        item.put("programName", doc.getString("programName"));
                        item.put("calories", cal != null ? cal : 0);
                        item.put("durationSeconds", dur != null ? dur : 0);
                        item.put("percentage", pct != null ? pct : 0);
                        item.put("completed", completed);
                        item.put("date", date);
                        history.add(item);
                    }

                    // Sort history by date descending
                    history.sort((a, b) -> {
                        Date da = (Date) a.get("date");
                        Date db2 = (Date) b.get("date");
                        if (da == null && db2 == null) return 0;
                        if (da == null) return 1;
                        if (db2 == null) return -1;
                        return db2.compareTo(da);
                    });

                    // Calculate streak
                    int streak = calculateStreak(completedDays, dayFmt);

                    // Update stats UI
                    tvTotalWorkouts.setText(String.valueOf(totalWorkouts));
                    tvTotalCalories.setText(String.valueOf(totalCalories));
                    tvTotalMinutes.setText(String.valueOf(totalSeconds / 60));
                    tvAvgCompletion.setText(totalWorkouts > 0
                            ? (totalPercentage / totalWorkouts) + "%"
                            : "0%");
                    tvStreakCount.setText(streak + " days");

                    // Populate history list
                    historyContainer.removeAllViews();
                    if (history.isEmpty()) {
                        showEmpty();
                    } else {
                        for (Map<String, Object> item : history) {
                            addHistoryCard(item);
                        }
                    }
                })
                .addOnFailureListener(e -> showEmpty());
    }

    private int calculateStreak(Set<String> completedDays, SimpleDateFormat fmt) {
        if (completedDays.isEmpty()) return 0;
        List<String> days = new ArrayList<>(completedDays);
        Collections.sort(days, Collections.reverseOrder());

        Calendar cal = Calendar.getInstance();
        String today = fmt.format(cal.getTime());
        cal.add(Calendar.DAY_OF_YEAR, -1);
        String yesterday = fmt.format(cal.getTime());

        // Streak must include today or yesterday
        if (!days.get(0).equals(today) && !days.get(0).equals(yesterday)) return 0;

        int streak = 1;
        for (int i = 0; i < days.size() - 1; i++) {
            try {
                Date d1 = fmt.parse(days.get(i));
                Date d2 = fmt.parse(days.get(i + 1));
                long diff = d1.getTime() - d2.getTime();
                if (diff == 86400000L) {
                    streak++;
                } else {
                    break;
                }
            } catch (Exception e) { break; }
        }
        return streak;
    }

    private void addHistoryCard(Map<String, Object> item) {
        View card = LayoutInflater.from(this).inflate(R.layout.item_history_card, historyContainer, false);

        TextView tvName       = card.findViewById(R.id.tvHistoryName);
        TextView tvDate       = card.findViewById(R.id.tvHistoryDate);
        TextView tvDuration   = card.findViewById(R.id.tvHistoryDuration);
        TextView tvCalories   = card.findViewById(R.id.tvHistoryCalories);
        TextView tvCompletion = card.findViewById(R.id.tvHistoryCompletion);

        String name = (String) item.get("programName");
        tvName.setText(name != null ? name : "Workout");

        Date date = (Date) item.get("date");
        if (date != null) {
            SimpleDateFormat fmt = new SimpleDateFormat("EEE, MMM d · HH:mm", Locale.getDefault());
            tvDate.setText(fmt.format(date));
        }

        long dur = ((Number) item.get("durationSeconds")).longValue();
        tvDuration.setText(dur / 60 + " min");

        long cal = ((Number) item.get("calories")).longValue();
        tvCalories.setText(cal + " kcal");

        long pct = ((Number) item.get("percentage")).longValue();
        Boolean completed = (Boolean) item.get("completed");
        if (Boolean.TRUE.equals(completed) && pct >= 100) {
            tvCompletion.setText("✅ Completed");
            tvCompletion.setTextColor(0xFF4CAF50);
        } else {
            tvCompletion.setText(pct + "% done");
            tvCompletion.setTextColor(0xFFFF9800);
        }

        historyContainer.addView(card);
    }

    private void showEmpty() {
        TextView tv = new TextView(this);
        tv.setText("No workouts yet.\nStart your first one!");
        tv.setTextColor(0xFF9CA3AF);
        tv.setTextSize(15f);
        tv.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 64, 0, 0);
        tv.setLayoutParams(lp);
        historyContainer.addView(tv);
    }
}