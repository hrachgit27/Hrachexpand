package com.example.myapplication;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EditText messageInput;
    private Button sendButton;
    private MessageAdapter adapter;
    private ArrayList<Message> chatMessages = new ArrayList<>();

    private Hrachexpand currentUser;
    private int receiverId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        recyclerView = findViewById(R.id.messagesRecyclerView);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);

        // Get from intent
        receiverId = getIntent().getIntExtra("receiverId", -1);
        int currentUserId = getIntent().getIntExtra("currentUserId", -1);

        // Load your currentUser from somewhere
        currentUser = new Hrachexpand();
        currentUser.setId(currentUserId);

        adapter = new MessageAdapter(chatMessages, currentUserId);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        loadChat();

        sendButton.setOnClickListener(v -> {
            String text = messageInput.getText().toString().trim();
            if (!text.isEmpty()) {
                currentUser.sendMessage(receiverId, text);
                messageInput.setText("");
                // reload chat
                loadChat();
            }
        });
    }

    private void loadChat() {
        currentUser.loadChatWith(receiverId, messages -> {
            chatMessages.clear();
            chatMessages.addAll(messages);
            adapter.notifyDataSetChanged();
            recyclerView.scrollToPosition(chatMessages.size() - 1);
        });
    }
}