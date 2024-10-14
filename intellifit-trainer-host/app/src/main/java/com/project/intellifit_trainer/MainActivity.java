package com.project.intellifit_trainer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    EditText username, password;
    TextView forgotpw;
    Button login, signup;
    Intent intent;
    String str_username, str_password;
    FirebaseAuth mAuth;

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        updateUI(currentUser);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        clearSelectedWorkout();

        mAuth = FirebaseAuth.getInstance();
        username = findViewById(R.id.main_et_username);
        password = findViewById(R.id.main_et_pw);
        login = findViewById(R.id.main_bt_login);
        signup = findViewById(R.id.main_bt_signup);
        forgotpw = findViewById(R.id.main_tv_forgotpw);

        // Using lambda for forgot password click listener
        forgotpw.setOnClickListener(v -> {
            intent = new Intent(MainActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });

        // Using lambda for sign up click listener
        signup.setOnClickListener(v -> {
            intent = new Intent(MainActivity.this, SignUpActivity.class);
            startActivity(intent);
        });

        // Using lambda for login click listener
        login.setOnClickListener(v -> {
            str_username = username.getText().toString().trim();
            str_password = password.getText().toString().trim();

            if (TextUtils.isEmpty(str_username)) {
                Toast.makeText(MainActivity.this, "Please enter your username.", Toast.LENGTH_SHORT).show();
                username.requestFocus();
                return;
            }
            if (TextUtils.isEmpty(str_password)) {
                Toast.makeText(MainActivity.this, "Please enter your password.", Toast.LENGTH_SHORT).show();
                password.requestFocus();
                return;
            }

            mAuth.signInWithEmailAndPassword(str_username, str_password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            updateVerificationStatus(user);

                            if (user != null && user.isEmailVerified()) {
                                Toast.makeText(MainActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                                intent = new Intent(MainActivity.this, LoggedInActivity.class);
                                startActivity(intent);
                                finish();
                            } else if (user != null) {
                                Toast.makeText(MainActivity.this, "Please verify your email address first.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(MainActivity.this, "Username or password incorrect.", Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }

    private void updateUI(FirebaseUser currentUser) {
        if (currentUser != null) {
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users");
            ref.child(currentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    String username = dataSnapshot.child("username").getValue(String.class);
                    if (username != null) {
                        if (currentUser.isEmailVerified()) {
                            Toast.makeText(MainActivity.this, "Welcome back " + username + "!", Toast.LENGTH_SHORT).show();
                            intent = new Intent(MainActivity.this, LoggedInActivity.class);
                            intent.putExtra("USERNAME", username);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(MainActivity.this, "Welcome back " + username + "!\nPlease verify your email.\n", Toast.LENGTH_LONG).show();
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e("updateUI", "Database error", databaseError.toException());
                }
            });
        }
    }

    private void updateVerificationStatus(FirebaseUser user) {
        if (user != null && user.isEmailVerified()) {
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(user.getUid());
            userRef.child("is_verified").setValue(true)
                    .addOnSuccessListener(aVoid -> Log.d("Firebase", "User verified status updated to true"))
                    .addOnFailureListener(e -> Log.d("Firebase", "Failed to update user verified status: " + e.getMessage()));  // Changed here
        }
    }

    private void clearSelectedWorkout() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String userID = user.getUid();
            DatabaseReference selectedWorkoutRef = FirebaseDatabase.getInstance().getReference("users").child(userID).child("selected_workout");

            selectedWorkoutRef.removeValue().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d("ClearSelectedWorkout", "Selected workout cleared successfully.");
                } else {
                    Log.w("ClearSelectedWorkout", "Failed to clear selected workout.", task.getException());
                }
            });
        }
    }
}
