package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;

public class MatchActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.match_layout);

        String matchImagePath = getIntent().getStringExtra("match");
        String matchName      = getIntent().getStringExtra("matchName");
        int    receiverId     = getIntent().getIntExtra("receiverId",    -1);
        int    currentUserId  = getIntent().getIntExtra("currentUserId", -1);

        ImageView matchImageView = findViewById(R.id.image);
        TextView  titleText      = findViewById(R.id.matchTitle);      // add to layout
        TextView  subtitleText   = findViewById(R.id.matchSubtitle);   // add to layout
        Button    chatButton     = findViewById(R.id.chatButton);       // "Send a Message"
        Button    continueButton = findViewById(R.id.nextB);            // "Keep Swiping"

        // ── Photo ─────────────────────────────────────────────────────────────
        Glide.with(this)
                .load(matchImagePath)
                .transform(new CircleCrop())
                .placeholder(R.drawable.ic_profile_placeholder)
                .into(matchImageView);

        // ── Text ──────────────────────────────────────────────────────────────
        if (titleText != null)
            titleText.setText("It's a Match! 🎉");
        if (subtitleText != null && matchName != null)
            subtitleText.setText("You and " + matchName + " liked each other.");

        // ── Entrance animation ────────────────────────────────────────────────
        if (matchImageView != null)
            matchImageView.startAnimation(
                    AnimationUtils.loadAnimation(this, android.R.anim.fade_in));
        if (titleText != null)
            titleText.startAnimation(
                    AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left));

        // ── Buttons ───────────────────────────────────────────────────────────
        // "Send a Message" — opens the chat directly
        if (chatButton != null) {
            chatButton.setOnClickListener(v -> {
                if (receiverId != -1 && currentUserId != -1) {
                    Intent intent = new Intent(this, ChatActivity.class);
                    intent.putExtra("receiverId",    receiverId);
                    intent.putExtra("currentUserId", currentUserId);
                    intent.putExtra("receiverName",  matchName);
                    startActivity(intent);
                }
                setResult(RESULT_OK);
                finish();
            });
        }

        // "Keep Swiping" — return to swipe deck
        continueButton.setOnClickListener(v -> {
            setResult(RESULT_OK);
            finish();
        });
    }
}