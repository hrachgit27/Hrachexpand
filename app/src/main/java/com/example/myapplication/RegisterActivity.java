package com.example.myapplication;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import java.io.InputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import android.util.Log;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private ImageView imageView;
    private Uri imageUri = null;
    private Hrachexpand newuser = new Hrachexpand();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.registration);  // <- see layout below

        EditText usernameInput = findViewById(R.id.usernameInput);
        EditText passwordInput = findViewById(R.id.passwordInput);
        EditText firstNameInput = findViewById(R.id.firstNameInput);
        EditText lastNameInput = findViewById(R.id.lastNameInput);
        EditText ageInput = findViewById(R.id.ageInput);
        EditText bioInput = findViewById(R.id.bioInput);
        EditText prefInput = findViewById(R.id.prefInput);
        imageView = findViewById(R.id.imageView);
        Button browseButton = findViewById(R.id.browseButton);
        Button registerButton = findViewById(R.id.registerButton);

        browseButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        });



        registerButton.setOnClickListener(v -> {
            String username = usernameInput.getText().toString();
            String password = passwordInput.getText().toString();
            String firstName = firstNameInput.getText().toString();
            String lastName = lastNameInput.getText().toString();
            int age = Integer.parseInt(ageInput.getText().toString());
            String bio = bioInput.getText().toString();
            String pref = prefInput.getText().toString();
            String path = imageUri != null ? imageUri.getPath() : "";

            newuser = new Hrachexpand(username, password, firstName, lastName, age, bio, pref, path);
            Toast.makeText(this, "User Registered", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(RegisterActivity.this, MainActivity.class));
            // 👇 Save user and navigate AFTER upload completes
            //uploadImageAndRegister(newuser, imageUri);
        });
    }

    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            imageView.setImageURI(imageUri); // still show preview

            // Upload to Firebase Storage
            StorageReference storageRef = FirebaseStorage.getInstance().getReference();
            String filename = "user_" + System.currentTimeMillis() + ".jpg";
            StorageReference fileRef = storageRef.child("images/" + filename);

            fileRef.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            String downloadUrl = uri.toString();

                            // Save downloadUrl to Firestore under the user document
                            FirebaseFirestore db = FirebaseFirestore.getInstance();
                            String userId = String.valueOf(newuser.getId());
                            db.collection("users").document(userId)
                                    .update("filepath", downloadUrl)
                                    .addOnSuccessListener(aVoid ->
                                            Toast.makeText(this, "Image uploaded & saved", Toast.LENGTH_SHORT).show())
                                    .addOnFailureListener(e ->
                                            Toast.makeText(this, "Firestore error: " + e.getMessage(), Toast.LENGTH_SHORT).show());

                        });
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }
}