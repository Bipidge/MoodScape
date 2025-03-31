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
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


public class SignIn extends AppCompatActivity {
    private static final String TAG = "SignInActivity";

    private FirebaseAuth mAuth;
    private EditText editTextEmail, editTextPassword;
    private Button buttonSignIn, buttonGoToSignUp;
    private ProgressBar progressBarSignIn; // Optional ProgressBar

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_in); // Use the sign-in layout

        mAuth = FirebaseAuth.getInstance();

        editTextEmail = findViewById(R.id.TextEmail); // ID from activity_sign_in.xml
        editTextPassword = findViewById(R.id.TextPassword); // ID from activity_sign_in.xml
        buttonSignIn = findViewById(R.id.signInButton);
        buttonGoToSignUp = findViewById(R.id.buttonGoToSignUp);
        // Initialize ProgressBar - Make sure you add this ID to activity_sign_in.xml
        // progressBarSignIn = findViewById(R.id.progressBarSignIn);

        // Apply window insets padding to the root content view
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets; // Return CONSUMED if you handle padding differently
        });

        // Set onClickListeners programmatically
        buttonSignIn.setOnClickListener(v -> signInButtonClicked());
        buttonGoToSignUp.setOnClickListener(v -> goToSignUpClicked());
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Log.d(TAG, "User " + currentUser.getUid() + " already signed in. Navigating to Main.");
            navigateToMainApp();
        } else {
            Log.d(TAG, "No user signed in.");
        }
    }

    // Method for sign-in button click
    private void signInButtonClicked() {
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
        if (password.length() < 6) { // Firebase default minimum
            editTextPassword.setError("Password should be at least 6 characters");
            editTextPassword.requestFocus();
            return;
        }
        // --- End Validation ---

        // Show progress bar (if added)
        // if (progressBarSignIn != null) progressBarSignIn.setVisibility(View.VISIBLE);
        // Disable buttons during login attempt
        buttonSignIn.setEnabled(false);
        buttonGoToSignUp.setEnabled(false);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> { // Using lambda
                    // Hide progress bar (if added)
                    // if (progressBarSignIn != null) progressBarSignIn.setVisibility(View.GONE);
                    // Re-enable buttons
                    buttonSignIn.setEnabled(true);
                    buttonGoToSignUp.setEnabled(true);

                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithEmail:success");
                        Toast.makeText(SignIn.this, "Authentication successful.", Toast.LENGTH_SHORT).show();
                        navigateToMainApp();
                    } else {
                        Log.w(TAG, "signInWithEmail:failure", task.getException());
                        Toast.makeText(SignIn.this, "Authentication failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    // Method for navigating to SignUp activity
    private void goToSignUpClicked() {
        Intent intent = new Intent(this, Signup.class);
        startActivity(intent);
        // Do not finish SignIn, so user can press back from Signup to return here
    }

    // Navigate to the main part of the app (MainActivity)
    private void navigateToMainApp() {
        Intent intent = new Intent(SignIn.this, MainActivity.class);
        // Clear the activity stack so the user can't navigate back to the login screen
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish(); // Call finish to remove SignIn activity from the back stack
    }
}