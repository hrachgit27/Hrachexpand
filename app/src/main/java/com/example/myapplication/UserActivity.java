package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import android.content.Intent;
import java.util.ArrayList;
import java.util.List;

import static kotlinx.coroutines.DelayKt.delay;

public class UserActivity extends AppCompatActivity {

    int currentIndex =  0;
    Hrachexpand currentUser = new Hrachexpand();
    ArrayList<Hrachexpand> loadedUsers = new ArrayList<>();
    ArrayList<Integer> matches = new ArrayList<>();
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    InboxFragment inboxFragment = new InboxFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.useractivity);

        // Get login info
        String username = getIntent().getStringExtra("username");
        String password = getIntent().getStringExtra("password");
        currentUser.login(username,password, this, ((success, user) ->
                {
                if (success) {
                    currentUser = user;
                    currentUser.setLoadedUsers(this, () -> {
                        loadedUsers = currentUser.getLoadedUsers();
                        showCurrentUser();
                    });
                    currentUser.loadMatches(this, () -> {
                        matches = currentUser.getMatches();
                        inboxFragment.setMatchedUsers(matches);
                    });
                } else {
                    Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
                    finish(); // exit activity
                }
            }));


        // Button handlers
        Button likeButton = findViewById(R.id.likeButton);
        Button dislikeButton = findViewById(R.id.passButton);
        ImageButton messageButton = findViewById(R.id.messagesButton);
        ImageButton matchButton = findViewById(R.id.matchesButton);

        likeButton.setOnClickListener(v -> {
            Hrachexpand likedUser = loadedUsers.get(currentIndex);
            currentUser.numLikes(likedUser.getId());
            db.collection("users").document(String.valueOf(currentUser.getId()))
                    .update("likes", currentUser.getLikes());
            if (currentUser.matchMaker(likedUser)) {
                currentUser.numMatches(likedUser.getId());
                db.collection("users").document(String.valueOf(currentUser.getId()))
                        .update("matches", currentUser.getMatches());
                Intent intent = new Intent(UserActivity.this, MatchActivity.class);
                intent.putExtra("match", likedUser.getFilepath());
                startActivityForResult(intent, 1001);
            } else {
                currentIndex++;
                showCurrentUser();
            }
        });

        dislikeButton.setOnClickListener(v -> {
            Hrachexpand dislikedUser = loadedUsers.get(currentIndex);
            currentUser.numDislikes(dislikedUser.getId());
            db.collection("users").document(String.valueOf(currentUser.getId()))
                    .update("dislikes", currentUser.getDislikes());
            currentIndex++;
            showCurrentUser();
        });

        messageButton.setOnClickListener(v -> {
            findViewById(R.id.image).setVisibility(View.GONE);
            findViewById(R.id.matchNameAge).setVisibility(View.GONE);
            findViewById(R.id.matchBio).setVisibility(View.GONE);
            findViewById(R.id.buttonRow).setVisibility(View.GONE);

            matches = currentUser.getMatches();
            inboxFragment.setMatchedUsers(matches);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.inbox_activity, inboxFragment)
                    .commit();
        });

        matchButton.setOnClickListener(v -> {
            if (getSupportFragmentManager().findFragmentById(R.id.inbox_activity) != null) {
                getSupportFragmentManager().beginTransaction()
                        .remove(getSupportFragmentManager().findFragmentById(R.id.inbox_activity))
                        .commit();
            }

            findViewById(R.id.image).setVisibility(View.VISIBLE);
            findViewById(R.id.matchNameAge).setVisibility(View.VISIBLE);
            findViewById(R.id.matchBio).setVisibility(View.VISIBLE);
            findViewById(R.id.buttonRow).setVisibility(View.VISIBLE);
        });
    }

    void showCurrentUser() {
        if (currentIndex >= loadedUsers.size()) {
            Toast.makeText(this, "No more users", Toast.LENGTH_SHORT).show();
            return;
        }
        // Update the ImageView with the user's image
        ImageView userImage = findViewById(R.id.image);
        TextView infoText = findViewById(R.id.matchNameAge);
        TextView bioText = findViewById(R.id.matchBio);

        Glide.with(this)
                .load(loadedUsers.get(currentIndex).getFilepath())
                .into(userImage);
        infoText.setText(loadedUsers.get(currentIndex).getFirstName() + ", " + loadedUsers.get(currentIndex).getAge());
        bioText.setText(loadedUsers.get(currentIndex).getBio());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001) {
            currentIndex++;
            showCurrentUser();
        }
    }
}
