package com.example.myapplication;

public class ExerciseMET {
    public static float getMET(String exerciseName) {
        if (exerciseName == null) return 4.0f;
        String name = exerciseName.toLowerCase();
        
        if (name.contains("run") || name.contains("sprint")) {
            return 9.5f;
        } else if (name.contains("squat") || name.contains("deadlift") || name.contains("press")) {
            return 6.0f;
        } else if (name.contains("push") || name.contains("pull") || name.contains("dip")) {
            return 5.0f;
        } else if (name.contains("curl") || name.contains("row") || name.contains("fly")) {
            return 4.5f;
        } else if (name.contains("plank") || name.contains("crunch") || name.contains("sit")) {
            return 3.8f;
        } else if (name.contains("stretch") || name.contains("yoga") || name.contains("warm")) {
            return 2.5f;
        } else {
            return 4.0f;
        }
    }
}
