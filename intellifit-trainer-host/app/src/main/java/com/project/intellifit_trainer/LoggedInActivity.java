package com.project.intellifit_trainer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoggedInActivity extends AppCompatActivity {

    TextView welcomeText;
    Button startWorkout, createWorkout, myWorkouts, workoutHistory, learnExercises, myProfile, support, logOut;
    Intent intent;
    FirebaseAuth mAuth;

    @Override
    protected void onResume() {
        super.onResume();
        // Check selected workout whenever the user returns to the activity
        checkSelectedWorkout();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loggedin);

        mAuth = FirebaseAuth.getInstance();
        welcomeText = findViewById(R.id.loggedin_tv_welcomeText);
        startWorkout = findViewById(R.id.loggedin_bt_startworkout);
        createWorkout = findViewById(R.id.loggedin_bt_createworkout);
        myWorkouts = findViewById(R.id.loggedin_bt_myworkouts);
        workoutHistory = findViewById(R.id.loggedin_bt_workoutHistory);
        learnExercises = findViewById(R.id.loggedin_bt_learnExercises);
        myProfile = findViewById(R.id.loggedin_bt_myProfile);
        support = findViewById(R.id.loggedin_bt_support);
        logOut = findViewById(R.id.loggedin_bt_logOut);

        fetchUsername();
        checkSelectedWorkout();

        logOut.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            intent = new Intent(LoggedInActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        support.setOnClickListener(v -> {
            intent = new Intent(LoggedInActivity.this, SupportActivity.class);
            startActivity(intent);
        });

        myProfile.setOnClickListener(v -> {
            intent = new Intent(LoggedInActivity.this, MyProfileActivity.class);
            startActivity(intent);
        });

        learnExercises.setOnClickListener(v -> {
            intent = new Intent(LoggedInActivity.this, LearnExercisesActivity.class);
            startActivity(intent);
        });

        createWorkout.setOnClickListener(v -> {
            intent = new Intent(LoggedInActivity.this, CreateWorkoutActivity.class);
            startActivity(intent);
        });

        myWorkouts.setOnClickListener(v -> {
            intent = new Intent(LoggedInActivity.this, MyWorkoutsActivity.class);
            startActivity(intent);
        });

        startWorkout.setOnClickListener(v -> {
            intent = new Intent(LoggedInActivity.this, StartWorkoutActivity.class);
            startActivity(intent);
        });
    }

    private void fetchUsername() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("users").child(userId);

            databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        String username = dataSnapshot.child("username").getValue(String.class);
                        String welcomeMessage = getString(R.string.welcome_message, username);
                        welcomeText.setText(welcomeMessage);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e("LoggedInActivity", "Error fetching username", databaseError.toException());
                }
            });
        }
    }

    private void checkSelectedWorkout() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String userID = user.getUid();
            DatabaseReference selectedWorkoutRef = FirebaseDatabase.getInstance().getReference("users").child(userID).child("selected_workout");

            selectedWorkoutRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        // If selected_workout exists, enable the button and update its background
                        startWorkout.setEnabled(true);
                        startWorkout.setBackgroundResource(R.drawable.button_background);
                    } else {
                        // If selected_workout does not exist, disable the button and update its background
                        startWorkout.setEnabled(false);
                        startWorkout.setBackgroundResource(R.drawable.button_disabled);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e("LoggedInActivity", "Error checking selected workout", databaseError.toException());
                }
            });
        }
    }
}
