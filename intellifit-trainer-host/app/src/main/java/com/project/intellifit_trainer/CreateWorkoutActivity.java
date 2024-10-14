package com.project.intellifit_trainer;


import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;

import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreateWorkoutActivity extends AppCompatActivity {
    private WorkoutAdapter adapter;
    private List<WorkoutExercise> workoutExercisesList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_createworkout);

        RecyclerView rvExercises = findViewById(R.id.rvExercises);
        rvExercises.setLayoutManager(new LinearLayoutManager(this));

        workoutExercisesList = new ArrayList<>();
        initializeWorkoutExercises();

        // Use lambda expression for the OnItemClickListener
        adapter = new WorkoutAdapter(workoutExercisesList, workoutExercise -> {
            // Handle exercise item click if needed
        });
        rvExercises.setAdapter(adapter);

        Button btnSaveWorkout = findViewById(R.id.btnSaveWorkout);
        // Use lambda expression for the OnClickListener
        btnSaveWorkout.setOnClickListener(view -> showWorkoutNameDialog());
    }

    private void initializeWorkoutExercises() {
        workoutExercisesList.add(new WorkoutExercise("Push Up", "Chest, Triceps", R.drawable.push_up_image));
        workoutExercisesList.add(new WorkoutExercise("Dumbbell Curl", "Biceps, Forearms", R.drawable.dumbbell_curl_image));
        workoutExercisesList.add(new WorkoutExercise("High Knees", "Quads, Calves", R.drawable.high_knees_image));
        workoutExercisesList.add(new WorkoutExercise("Cable Triceps", "Triceps, Shoulders", R.drawable.cable_triceps_image));
        workoutExercisesList.add(new WorkoutExercise("Mountain Climbers", "Core, Shoulders", R.drawable.mountain_climbers_image));
        workoutExercisesList.add(new WorkoutExercise("Lunge", "Quads, Glutes", R.drawable.lunge_image));
        workoutExercisesList.add(new WorkoutExercise("Pull Up", "Back, Biceps", R.drawable.pull_up_image));
        workoutExercisesList.add(new WorkoutExercise("Squat", "Quads, Hamstrings", R.drawable.squat_image));
        workoutExercisesList.add(new WorkoutExercise("Jumping Rope", "Calves, Forearms", R.drawable.jumping_rope_image));
        workoutExercisesList.add(new WorkoutExercise("Jumping Jack", "Total Body", R.drawable.jumping_jack_image));
    }

    private void saveWorkout(String workoutName) {
        List<WorkoutExercise> exercisesToSave = adapter.getAddedExercises(); // Get added exercises locally

        if (exercisesToSave.isEmpty()) {
            Toast.makeText(this, "No exercises selected.\nPlease add exercises before saving.", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String userID = user != null ? user.getUid() : null; // Handle potential null userID

        if (userID == null) {
            Toast.makeText(this, "User ID is null. Please log in again.", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference("users").child(userID).child("workouts");
        String workoutId = databaseRef.push().getKey(); // This could be null if the push fails

        // Check if workoutId is null before proceeding
        if (workoutId == null) {
            Toast.makeText(this, "Failed to create workout ID. Please try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Call the extracted method to get workout data
        Map<String, Object> workoutData = createWorkoutData(workoutName, exercisesToSave);

        databaseRef.child(workoutId).setValue(workoutData)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Workout saved successfully!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to save workout.", Toast.LENGTH_SHORT).show());

        Intent intent = new Intent(CreateWorkoutActivity.this, CreateWorkoutActivity.class);
        startActivity(intent);
        finish();
    }

    private Map<String, Object> createWorkoutData(String workoutName, List<WorkoutExercise> exercisesToSave) {
        Map<String, Object> workoutData = new HashMap<>();
        workoutData.put("workoutName", workoutName);

        List<Map<String, Object>> exercisesData = new ArrayList<>();
        for (WorkoutExercise exercise : exercisesToSave) {
            Map<String, Object> exerciseData = new HashMap<>();
            exerciseData.put("name", exercise.getName());
            exerciseData.put("repCount", exercise.getRepCount());
            exerciseData.put("setCount", exercise.getSetCount());
            exercisesData.add(exerciseData);
        }
        workoutData.put("exercises", exercisesData);

        return workoutData;
    }

    private void showWorkoutNameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Name Your Workout");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        // Use lambda expression for the DialogInterface.OnClickListener
        builder.setPositiveButton("Save", (dialog, which) -> {
            String workoutName = input.getText().toString();
            saveWorkout(workoutName);
        });

        // Use lambda expression for the DialogInterface.OnClickListener
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }
}
