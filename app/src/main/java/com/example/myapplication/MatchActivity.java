package com.example.myapplication;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.Button;
import com.bumptech.glide.Glide;

public class MatchActivity extends AppCompatActivity {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.match_layout);

        ImageView matchImageView = findViewById(R.id.image);
        String matchImagePath = getIntent().getStringExtra("match");
        int currentIndex = getIntent().getIntExtra("index", 0);

        Glide.with(this)
                .load(matchImagePath)
                .into(matchImageView);

        Button nextB = findViewById(R.id.nextB);

        nextB.setOnClickListener(v -> {
            setResult(RESULT_OK);
            finish();
        });
    }
}
