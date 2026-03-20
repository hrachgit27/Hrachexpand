package com.example.myapplication;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import android.content.Intent;
import java.util.ArrayList;

public class UserActivity extends AppCompatActivity {

    private int currentIndex = 0;
    private Hrachexpand          currentUser   = new Hrachexpand();
    private ArrayList<Hrachexpand> loadedUsers = new ArrayList<>();
    private ArrayList<Integer>   matches       = new ArrayList<>();

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private InboxFragment     inboxFragment = new InboxFragment();

    // Swipe gesture
    private GestureDetector gestureDetector;
    private static final int SWIPE_THRESHOLD       = 100;
    private static final int SWIPE_VELOCITY_THRESHOLD = 100;

    // Buttons / views
    private Button      likeButton, dislikeButton;
    private ImageButton messageButton, matchButton;
    private ImageView   userImage;
    private TextView    infoText, bioText;
    private TextView    matchBadge;  // optional — shows match count on the matches icon

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.useractivity);

        bindViews();
        setupGestures();

        String username = getIntent().getStringExtra("username");
        String password = getIntent().getStringExtra("password");

        currentUser.login(username, password, this, (success, user) -> {
            if (!success) {
                Toast.makeText(this, "Login error. Please try again.", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            currentUser = user;

            // Pass real ID to InboxFragment before it can open chats
            inboxFragment.setCurrentUserId(currentUser.getId());

            // Load swipe deck
            currentUser.setLoadedUsers(this, () -> {
                loadedUsers = currentUser.getLoadedUsers();
                showCurrentUser();
            });

            // Load matches and update inbox
            currentUser.loadMatches(this, () -> {
                matches = currentUser.getMatches();
                updateMatchBadge();
                inboxFragment.setMatchedUsers(matches);
            });
        });

        // ── Like button ───────────────────────────────────────────────────────
        likeButton.setOnClickListener(v -> handleLike());

        // ── Dislike button ────────────────────────────────────────────────────
        dislikeButton.setOnClickListener(v -> handleDislike());

        // ── Inbox (messages) button ───────────────────────────────────────────
        messageButton.setOnClickListener(v -> showInbox());

        // ── Back-to-swipe button ──────────────────────────────────────────────
        matchButton.setOnClickListener(v -> showSwipeDeck());
    }

    // ── Action handlers ───────────────────────────────────────────────────────

    private void handleLike() {
        if (currentIndex >= loadedUsers.size()) return;
        Hrachexpand liked = loadedUsers.get(currentIndex);

        likeButton.setEnabled(false);
        dislikeButton.setEnabled(false);

        // Animate card off to the right
        animateCard(true, () ->
                currentUser.likeUser(liked, isMatch -> {
                    likeButton.setEnabled(true);
                    dislikeButton.setEnabled(true);

                    if (isMatch) {
                        // Refresh match list so inbox updates
                        currentUser.numMatches(liked.getId());
                        matches = currentUser.getMatches();
                        updateMatchBadge();
                        inboxFragment.setMatchedUsers(matches);

                        // Show the match celebration screen
                        Intent intent = new Intent(UserActivity.this, MatchActivity.class);
                        intent.putExtra("match",     liked.getFilepath());
                        intent.putExtra("matchName", liked.getFirstName());
                        startActivityForResult(intent, 1001);
                    } else {
                        currentIndex++;
                        showCurrentUser();
                    }
                })
        );
    }

    private void handleDislike() {
        if (currentIndex >= loadedUsers.size()) return;
        Hrachexpand disliked = loadedUsers.get(currentIndex);

        likeButton.setEnabled(false);
        dislikeButton.setEnabled(false);

        animateCard(false, () -> {
            currentUser.dislikeUser(disliked, () -> {
                likeButton.setEnabled(true);
                dislikeButton.setEnabled(true);
                currentIndex++;
                showCurrentUser();
            });
        });
    }

    // ── Swipe gesture ─────────────────────────────────────────────────────────

    private void setupGestures() {
        gestureDetector = new GestureDetector(this,
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onFling(MotionEvent e1, MotionEvent e2,
                                           float velocityX, float velocityY) {
                        if (e1 == null || e2 == null) return false;
                        float diffX = e2.getX() - e1.getX();
                        float diffY = e2.getY() - e1.getY();
                        if (Math.abs(diffX) > Math.abs(diffY)
                                && Math.abs(diffX) > SWIPE_THRESHOLD
                                && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                            if (diffX > 0) handleLike();
                            else           handleDislike();
                            return true;
                        }
                        return false;
                    }
                });

        // Attach gesture to the card image so swiping the photo works
        userImage.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return true;
        });
    }

    // ── Card animation ────────────────────────────────────────────────────────

    /**
     * Slides the card out to the right (like) or left (dislike) then resets.
     * @param likeDirection true = right, false = left
     * @param onEnd         called after animation completes
     */
    private void animateCard(boolean likeDirection, Runnable onEnd) {
        float targetX = (likeDirection ? 1 : -1) * userImage.getWidth() * 1.5f;
        ObjectAnimator anim = ObjectAnimator.ofFloat(userImage, "translationX", 0, targetX);
        anim.setDuration(300);
        anim.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) {
                userImage.setTranslationX(0);
                onEnd.run();
            }
        });
        anim.start();
    }

    // ── Display helpers ───────────────────────────────────────────────────────

    private void showCurrentUser() {
        if (currentIndex >= loadedUsers.size()) {
            userImage.setImageResource(R.drawable.ic_no_more_users); // placeholder
            infoText.setText("You've seen everyone!");
            bioText.setText("Check back later for new profiles.");
            likeButton.setEnabled(false);
            dislikeButton.setEnabled(false);
            return;
        }
        likeButton.setEnabled(true);
        dislikeButton.setEnabled(true);

        Hrachexpand shown = loadedUsers.get(currentIndex);

        Glide.with(this)
                .load(shown.getFilepath())
                .apply(RequestOptions.bitmapTransform(new RoundedCorners(32)))
                .placeholder(R.drawable.ic_profile_placeholder)
                .error(R.drawable.ic_profile_placeholder)
                .into(userImage);

        infoText.setText(shown.getFirstName() + ", " + shown.getAge());
        bioText.setText(shown.getBio());
    }

    private void showInbox() {
        setSwipeDeckVisible(false);
        matches = currentUser.getMatches();
        updateMatchBadge();
        inboxFragment.setMatchedUsers(matches);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.inbox_activity, inboxFragment)
                .commit();
    }

    private void showSwipeDeck() {
        androidx.fragment.app.Fragment existing =
                getSupportFragmentManager().findFragmentById(R.id.inbox_activity);
        if (existing != null) {
            getSupportFragmentManager().beginTransaction().remove(existing).commit();
        }
        setSwipeDeckVisible(true);
    }

    /** Toggle swipe-deck card views. */
    private void setSwipeDeckVisible(boolean visible) {
        int vis = visible ? View.VISIBLE : View.GONE;
        userImage.setVisibility(vis);
        infoText.setVisibility(vis);
        bioText.setVisibility(vis);
        findViewById(R.id.buttonRow).setVisibility(vis);
    }

    /** Show the match count as a small badge on the matches icon button. */
    private void updateMatchBadge() {
        if (matchBadge != null) {
            int count = matches.size();
            matchBadge.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
            matchBadge.setText(count > 9 ? "9+" : String.valueOf(count));
        }
    }

    // ── View binding ──────────────────────────────────────────────────────────

    private void bindViews() {
        userImage      = findViewById(R.id.image);
        infoText       = findViewById(R.id.matchNameAge);
        bioText        = findViewById(R.id.matchBio);
        likeButton     = findViewById(R.id.likeButton);
        dislikeButton  = findViewById(R.id.passButton);
        messageButton  = (ImageButton) findViewById(R.id.messagesButton);
        matchButton    = (ImageButton) findViewById(R.id.matchesButton);
        matchBadge     = findViewById(R.id.matchCountBadge); // add this TextView to your layout
    }

    // ── Activity result ───────────────────────────────────────────────────────

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001) {
            currentIndex++;
            showCurrentUser();
        }
    }
}