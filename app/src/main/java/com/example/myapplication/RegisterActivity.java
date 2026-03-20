package com.example.myapplication;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class RegisterActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;

    private ImageView imageView;
    private Button    registerButton;
    private Uri       imageUri       = null;
    private String    uploadedUrl    = null; // only set once Firebase gives us the https:// URL
    private boolean   uploading      = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.registration);

        EditText usernameInput   = findViewById(R.id.usernameInput);
        EditText passwordInput   = findViewById(R.id.passwordInput);
        EditText confirmPwdInput = findViewById(R.id.confirmPasswordInput);
        EditText firstNameInput  = findViewById(R.id.firstNameInput);
        EditText lastNameInput   = findViewById(R.id.lastNameInput);
        EditText ageInput        = findViewById(R.id.ageInput);
        EditText bioInput        = findViewById(R.id.bioInput);
        EditText prefInput       = findViewById(R.id.prefInput);
        imageView                = findViewById(R.id.imageView);
        Button browseButton      = findViewById(R.id.browseButton);
        registerButton           = findViewById(R.id.registerButton);

        // ── Photo picker ──────────────────────────────────────────────────────
        browseButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        });

        // ── Register ──────────────────────────────────────────────────────────
        registerButton.setOnClickListener(v -> {
            String username  = usernameInput.getText().toString().trim();
            String password  = passwordInput.getText().toString().trim();
            String confirmPw = confirmPwdInput != null
                               ? confirmPwdInput.getText().toString().trim() : password;
            String firstName = firstNameInput.getText().toString().trim();
            String lastName  = lastNameInput.getText().toString().trim();
            String ageStr    = ageInput.getText().toString().trim();
            String bio       = bioInput.getText().toString().trim();
            String pref      = prefInput.getText().toString().trim();

            // ── Validation ────────────────────────────────────────────────────
            if (TextUtils.isEmpty(username)) {
                usernameInput.setError("Username is required"); return;
            }
            if (username.length() < 3) {
                usernameInput.setError("At least 3 characters"); return;
            }
            if (TextUtils.isEmpty(password)) {
                passwordInput.setError("Password is required"); return;
            }
            if (password.length() < 6) {
                passwordInput.setError("At least 6 characters"); return;
            }
            if (!password.equals(confirmPw)) {
                if (confirmPwdInput != null) confirmPwdInput.setError("Passwords don't match");
                else Toast.makeText(this, "Passwords don't match", Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(firstName)) {
                firstNameInput.setError("First name is required"); return;
            }
            if (TextUtils.isEmpty(ageStr)) {
                ageInput.setError("Age is required"); return;
            }
            int age;
            try {
                age = Integer.parseInt(ageStr);
            } catch (NumberFormatException e) {
                ageInput.setError("Enter a valid age"); return;
            }
            if (age < 18 || age > 100) {
                ageInput.setError("Must be 18–100"); return;
            }
            if (TextUtils.isEmpty(bio)) {
                bioInput.setError("Add a short bio"); return;
            }

            // If the user picked a photo but it hasn't finished uploading yet, wait.
            if (imageUri != null && uploadedUrl == null) {
                Toast.makeText(this, "Please wait, photo is still uploading…", Toast.LENGTH_SHORT).show();
                return;
            }

            // uploadedUrl is either the real https:// Firebase URL, or "" if no photo chosen.
            String finalPath = uploadedUrl != null ? uploadedUrl : "";

            // Create and save the user — filepath is already the https:// URL.
            Hrachexpand newuser = new Hrachexpand(
                    username, password, firstName, lastName, age, pref, bio, finalPath);

            Toast.makeText(this, "Welcome, " + firstName + "! Please log in.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(RegisterActivity.this, MainActivity.class));
            finish();
        });
    }

    // ── Image picker result ───────────────────────────────────────────────────

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_IMAGE_REQUEST
                || resultCode != RESULT_OK
                || data == null
                || data.getData() == null) return;

        imageUri    = data.getData();
        uploadedUrl = null;           // reset — previous URL no longer valid for this pick
        uploading   = true;

        imageView.setImageURI(imageUri);  // show preview immediately
        registerButton.setEnabled(false); // block register until upload completes
        registerButton.setText("Uploading photo…");

        StorageReference fileRef = FirebaseStorage.getInstance()
                .getReference()
                .child("images/user_" + System.currentTimeMillis() + ".jpg");

        fileRef.putFile(imageUri)
               .addOnSuccessListener(task ->
                   fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                       // This is the real https://firebasestorage.googleapis.com/... URL
                       uploadedUrl = uri.toString();
                       uploading   = false;

                       registerButton.setEnabled(true);
                       registerButton.setText("Create Account");
                       Toast.makeText(this, "Photo ready ✓", Toast.LENGTH_SHORT).show();
                   }))
               .addOnFailureListener(e -> {
                   uploading = false;
                   imageUri  = null;   // treat as no photo so registration can still proceed
                   registerButton.setEnabled(true);
                   registerButton.setText("Create Account");
                   Toast.makeText(this, "Photo upload failed — you can register without one.",
                           Toast.LENGTH_LONG).show();
               });
    }
}
