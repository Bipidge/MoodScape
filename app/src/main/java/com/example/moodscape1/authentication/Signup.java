package com.example.moodscape1.authentication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns; // Import Patterns for email validation
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar; // Import ProgressBar
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.moodscape1.MainActivity; // Navigate to MainActivity
import com.example.moodscape1.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore; // Import Firestore

import java.util.Date; // For timestamp
import java.util.HashMap;
import java.util.Map;

import com.example.moodscape1.R;

public class Signup extends AppCompatActivity {

    private static final String TAG = "SignUpActivity";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db; // Firestore instance
    private EditText editTextEmail, editTextPassword;
    private Button buttonSignUp, buttonGoToSignIn;
    private ProgressBar progressBarSignUp; // Optional ProgressBar

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup); // Use the sign-up layout

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance(); // Initialize Firestore

        // Use IDs from activity_signup.xml
        editTextEmail = findViewById(R.id.TextEmailAddress); // Inside TextInputLayout
        editTextPassword = findViewById(R.id.editTextTextPassword); // Inside TextInputLayout
        buttonSignUp = findViewById(R.id.signUpButton);
        buttonGoToSignIn = findViewById(R.id.signInButton); // The "Already have an account?" button
        // Initialize ProgressBar - Make sure you add this ID to activity_signup.xml
        // progressBarSignUp = findViewById(R.id.progressBarSignUp);

        // Apply window insets padding (using the R.id.main from activity_signup.xml)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Set onClickListeners programmatically
        buttonSignUp.setOnClickListener(v -> signupButtonClicked());
        buttonGoToSignIn.setOnClickListener(v -> goToSignInClicked());
    }

    // Method for signup button click
    private void signupButtonClicked() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        // --- Input Validation ---
        if (email.isEmpty()) {
            editTextEmail.setError("Email is required");
            editTextEmail.requestFocus();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editTextEmail.setError("Please enter a valid email");
            editTextEmail.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            editTextPassword.setError("Password is required");
            editTextPassword.requestFocus();
            return;
        }
        if (password.length() < 6) {
            editTextPassword.setError("Password should be at least 6 characters");
            editTextPassword.requestFocus();
            return;
        }
        // --- End Validation ---

        // Show progress bar (if added)
        // if (progressBarSignUp != null) progressBarSignUp.setVisibility(View.VISIBLE);
        // Disable buttons
        buttonSignUp.setEnabled(false);
        buttonGoToSignIn.setEnabled(false);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> { // Using lambda
                    if (task.isSuccessful()) {
                        Log.d(TAG, "createUserWithEmail:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Create user document in Firestore AFTER auth is successful
                            createUserDocument(user, email); // This will handle navigation on success
                        } else {
                            // Should not happen if task is successful, but handle defensively
                            Log.e(TAG, "User is null after successful creation.");
                            Toast.makeText(Signup.this, "Signup successful, but failed to get user data.", Toast.LENGTH_SHORT).show();
                            // Hide progress bar and re-enable buttons here too if Firestore step is skipped
                            // if (progressBarSignUp != null) progressBarSignUp.setVisibility(View.GONE);
                            buttonSignUp.setEnabled(true);
                            buttonGoToSignIn.setEnabled(true);
                            navigateToSignIn(); // Go back to sign in as something is wrong
                        }
                    } else {
                        Log.w(TAG, "createUserWithEmail:failure", task.getException());
                        Toast.makeText(Signup.this, "Authentication failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                        // Hide progress bar and re-enable buttons on failure
                        // if (progressBarSignUp != null) progressBarSignUp.setVisibility(View.GONE);
                        buttonSignUp.setEnabled(true);
                        buttonGoToSignIn.setEnabled(true);
                    }
                });
    }

    // Method to create a user document in Firestore
    private void createUserDocument(FirebaseUser firebaseUser, String email) {
        String userId = firebaseUser.getUid();
        // Create a new user map
        Map<String, Object> user = new HashMap<>();
        user.put("uid", userId);
        user.put("email", email);
        user.put("createdAt", new Date()); // Use server timestamp ideally, but Date is simpler for now
        // Add any other default fields you want for a new user
        // user.put("displayName", "New User");

        // Add a new document in collection "users" with ID = user's UID
        db.collection("users").document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> { // Using lambda
                    Log.d(TAG, "User document successfully created for UID: " + userId);
                    Toast.makeText(Signup.this, "Account created successfully!", Toast.LENGTH_SHORT).show();
                    // Navigate to Main App only AFTER Firestore document is created successfully
                    navigateToMainApp(); // Navigation happens here on full success
                })
                .addOnFailureListener(e -> { // Using lambda
                    Log.w(TAG, "Error creating user document", e);
                    // Decide how to handle this: Let user in anyway? Or force retry?
                    // Option: Let user in, but warn them.
                    Toast.makeText(Signup.this, "Account created, but failed to save profile data.", Toast.LENGTH_LONG).show();
                    // Still navigate to main app even if Firestore fails (or handle differently)
                    navigateToMainApp();
                    // Ensure buttons are re-enabled and progress bar hidden even on failure here
                    // if (progressBarSignUp != null) progressBarSignUp.setVisibility(View.GONE);
                    // buttonSignUp.setEnabled(true); // Already done in navigateToMainApp via finish()
                    // buttonGoToSignIn.setEnabled(true);
                });
    }

    // Method for navigating back to SignIn activity
    private void goToSignInClicked() {
        finish(); // Just finish Signup activity to go back to SignIn
    }

    // Navigate to the main part of the app (MainActivity)
    private void navigateToMainApp() {
        Intent intent = new Intent(Signup.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish(); // Finish Signup activity
    }

    // Navigate back to SignIn (used in error cases)
    private void navigateToSignIn() {
        Intent intent = new Intent(Signup.this, SignIn.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish(); // Finish Signup activity
    }
}