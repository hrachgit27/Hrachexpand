package com.example.myapplication;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView      recyclerView;
    private EditText          messageInput;
    private ImageButton       sendButton;
    private MessageAdapter    adapter;
    private TextView          typingIndicator;

    private final ArrayList<Message> chatMessages = new ArrayList<>();

    private Hrachexpand currentUser;
    private int         receiverId;
    private String      receiverName;

    // Real-time listener — must be removed in onStop to avoid leaks
    private ListenerRegistration chatListener;

    // Typing-indicator debounce
    private final android.os.Handler typingHandler = new android.os.Handler();
    private Runnable stopTypingRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        recyclerView    = findViewById(R.id.messagesRecyclerView);
        messageInput    = findViewById(R.id.messageInput);
        sendButton      = findViewById(R.id.sendButton);
        typingIndicator = findViewById(R.id.typingIndicator);

        receiverId    = getIntent().getIntExtra("receiverId",    -1);
        int currentUserId = getIntent().getIntExtra("currentUserId", -1);
        receiverName  = getIntent().getStringExtra("receiverName");
        if (receiverName == null) receiverName = "Chat";

        if (receiverId == -1 || currentUserId == -1) {
            Toast.makeText(this, "Unable to open chat.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // ── Toolbar ──────────────────────────────────────────────────────────
        Toolbar toolbar = findViewById(R.id.chatToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(receiverName);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // ── Set up user & adapter ─────────────────────────────────────────────
        currentUser = new Hrachexpand();
        currentUser.setId(currentUserId);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);          // newest messages at bottom
        recyclerView.setLayoutManager(layoutManager);

        adapter = new MessageAdapter(chatMessages, currentUserId);
        recyclerView.setAdapter(adapter);

        // ── Real-time chat listener ───────────────────────────────────────────
        attachChatListener();

        // ── Send button ───────────────────────────────────────────────────────
        sendButton.setOnClickListener(v -> sendMessage());

        // Allow sending with the keyboard's send action
        messageInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });

        // ── Typing indicator ──────────────────────────────────────────────────
        // Writes a "typing" flag to the receiver's document so the other side
        // can show a "Name is typing…" indicator via their own snapshot listener.
        stopTypingRunnable = () ->
            currentUser.updateField("typingTo", -1);

        messageInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    currentUser.updateField("typingTo", receiverId);
                    typingHandler.removeCallbacks(stopTypingRunnable);
                    typingHandler.postDelayed(stopTypingRunnable, 3000);
                } else {
                    typingHandler.removeCallbacks(stopTypingRunnable);
                    currentUser.updateField("typingTo", -1);
                }
            }
        });

        // ── Listen for receiver typing ────────────────────────────────────────
        attachTypingListener(currentUserId);
    }

    // ── Attach / detach listeners ─────────────────────────────────────────────

    private void attachChatListener() {
        chatListener = currentUser.listenToChat(receiverId, messages -> {
            chatMessages.clear();
            chatMessages.addAll(messages);
            adapter.updateMessages();
            if (!chatMessages.isEmpty()) {
                recyclerView.smoothScrollToPosition(chatMessages.size() - 1);
            }
        });
    }

    /** Watches the receiver's "typingTo" field to show/hide the indicator. */
    private void attachTypingListener(int myId) {
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("users")
            .document(String.valueOf(receiverId))
            .addSnapshotListener((snap, e) -> {
                if (snap == null) return;
                Long typingTo = snap.getLong("typingTo");
                boolean theyAreTypingToMe = typingTo != null && typingTo.intValue() == myId;
                if (typingIndicator != null) {
                    typingIndicator.setVisibility(theyAreTypingToMe ? View.VISIBLE : View.GONE);
                    typingIndicator.setText(receiverName + " is typing…");
                }
            });
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Remove listener to avoid memory leaks and ghost updates
        if (chatListener != null) {
            chatListener.remove();
            chatListener = null;
        }
        // Clear typing flag when leaving the screen
        typingHandler.removeCallbacks(stopTypingRunnable);
        if (currentUser != null) currentUser.updateField("typingTo", -1);
    }

    // ── Send ─────────────────────────────────────────────────────────────────

    private void sendMessage() {
        String text = messageInput.getText().toString().trim();
        if (text.isEmpty()) return;

        sendButton.setEnabled(false);
        messageInput.setText("");
        typingHandler.removeCallbacks(stopTypingRunnable);
        currentUser.updateField("typingTo", -1);

        currentUser.sendMessage(receiverId, text, msgId -> {
            sendButton.setEnabled(true);
            if (msgId == null) {
                Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show();
            }
            // The real-time listener will automatically update the RecyclerView
        });
    }
}
