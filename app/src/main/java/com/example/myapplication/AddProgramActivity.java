package com.example.myapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.*;

public class AddProgramActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private LinearLayout exerciseContainer;
    private EditText etProgramName;
    private ChipGroup chipGroupDays;
    private List<ExerciseEntry> exerciseEntries = new ArrayList<>();

    private static final String[] DAYS = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
    private static final String[] DAY_FULL = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_program);

        db = FirebaseFirestore.getInstance();

        etProgramName = findViewById(R.id.etProgramName);
        exerciseContainer = findViewById(R.id.exerciseContainer);
        chipGroupDays = findViewById(R.id.chipGroupDays);
        Button btnAddExercise = findViewById(R.id.btnAddExercise);
        Button btnSaveProgram = findViewById(R.id.btnSaveProgram);
        ImageView btnBack = findViewById(R.id.btnBack);

        // Build day chips
        for (int i = 0; i < DAYS.length; i++) {
            Chip chip = new Chip(this);
            chip.setText(DAYS[i]);
            chip.setTag(DAY_FULL[i]);
            chip.setCheckable(true);
            chip.setChipBackgroundColorResource(R.color.chip_bg_selector);
            chip.setTextColor(getResources().getColorStateList(R.color.chip_text_selector));
            chip.setChipStrokeColorResource(R.color.chip_stroke_selector);
            chip.setChipStrokeWidth(2f);
            chipGroupDays.addView(chip);
        }

        btnBack.setOnClickListener(v -> finish());

        btnAddExercise.setOnClickListener(v -> addExerciseCard());

        btnSaveProgram.setOnClickListener(v -> saveProgram());

        // Add first exercise card by default
        addExerciseCard();
    }

    private void addExerciseCard() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View card = inflater.inflate(R.layout.item_exercise_card, exerciseContainer, false);

        ExerciseEntry entry = new ExerciseEntry();
        entry.view = card;
        entry.etName = card.findViewById(R.id.etExerciseName);
        entry.etSets = card.findViewById(R.id.etSets);
        entry.etReps = card.findViewById(R.id.etReps);
        entry.etRestBetweenSets = card.findViewById(R.id.etRestBetweenSets);
        entry.etRestBetweenExercises = card.findViewById(R.id.etRestBetweenExercises);

        TextView tvIndex = card.findViewById(R.id.tvExerciseIndex);
        tvIndex.setText("Exercise " + (exerciseEntries.size() + 1));

        ImageView btnRemove = card.findViewById(R.id.btnRemoveExercise);
        btnRemove.setOnClickListener(v -> {
            exerciseContainer.removeView(card);
            exerciseEntries.remove(entry);
            reindexExercises();
        });

        exerciseEntries.add(entry);
        exerciseContainer.addView(card);
    }

    private void reindexExercises() {
        for (int i = 0; i < exerciseEntries.size(); i++) {
            TextView tv = exerciseEntries.get(i).view.findViewById(R.id.tvExerciseIndex);
            tv.setText("Exercise " + (i + 1));
        }
    }

    private void saveProgram() {
        String programName = etProgramName.getText().toString().trim();
        if (programName.isEmpty()) {
            etProgramName.setError("Enter a program name");
            etProgramName.requestFocus();
            return;
        }

        List<String> selectedDays = new ArrayList<>();
        for (int i = 0; i < chipGroupDays.getChildCount(); i++) {
            Chip chip = (Chip) chipGroupDays.getChildAt(i);
            if (chip.isChecked()) {
                selectedDays.add((String) chip.getTag());
            }
        }

        if (selectedDays.isEmpty()) {
            Toast.makeText(this, "Please select at least one day", Toast.LENGTH_SHORT).show();
            return;
        }

        if (exerciseEntries.isEmpty()) {
            Toast.makeText(this, "Please add at least one exercise", Toast.LENGTH_SHORT).show();
            return;
        }

        List<Map<String, Object>> exercises = new ArrayList<>();
        for (ExerciseEntry entry : exerciseEntries) {
            String name = entry.etName.getText().toString().trim();
            if (name.isEmpty()) {
                entry.etName.setError("Enter exercise name");
                entry.etName.requestFocus();
                return;
            }
            Map<String, Object> ex = new HashMap<>();
            ex.put("name", name);
            ex.put("sets", parseIntSafe(entry.etSets.getText().toString(), 3));
            ex.put("reps", parseIntSafe(entry.etReps.getText().toString(), 10));
            ex.put("restBetweenSets", parseIntSafe(entry.etRestBetweenSets.getText().toString(), 60));
            ex.put("restBetweenExercises", parseIntSafe(entry.etRestBetweenExercises.getText().toString(), 90));
            exercises.add(ex);
        }

        Map<String, Object> program = new HashMap<>();
        program.put("name", programName);
        program.put("days", selectedDays);
        program.put("exercises", exercises);
        program.put("createdAt", new Date());

        db.collection("programs")
                .add(program)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(this, "Program saved! 💪", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private int parseIntSafe(String s, int defaultVal) {
        try { return Integer.parseInt(s.trim()); }
        catch (Exception e) { return defaultVal; }
    }

    static class ExerciseEntry {
        View view;
        EditText etName, etSets, etReps, etRestBetweenSets, etRestBetweenExercises;
    }
}
