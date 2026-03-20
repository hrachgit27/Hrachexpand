package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class InboxFragment extends Fragment {

    private RecyclerView  recyclerView;
    private InboxAdapter  adapter;
    private TextView      emptyState;

    private final List<InboxAdapter.ConversationItem> conversations = new ArrayList<>();

    // currentUserId is passed in by UserActivity after login
    private int currentUserId = -1;

    /** Must be called by the host Activity before showing this fragment. */
    public void setCurrentUserId(int id) {
        this.currentUserId = id;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.inbox_activity, container, false);

        recyclerView = view.findViewById(R.id.inboxRecyclerView);
        emptyState   = view.findViewById(R.id.emptyStateText); // optional — add to layout

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.addItemDecoration(
                new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));

        adapter = new InboxAdapter(conversations, user -> {
            if (currentUserId == -1) {
                Log.e("InboxFragment", "currentUserId not set");
                return;
            }
            Intent intent = new Intent(requireContext(), ChatActivity.class);
            intent.putExtra("receiverId",    user.getId());
            intent.putExtra("currentUserId", currentUserId);
            intent.putExtra("receiverName",  user.getFirstName());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        return view;
    }

    /**
     * Loads matched users from Firestore, then for each one fetches the last
     * message and unread count so the inbox rows show real preview data.
     */
    public void setMatchedUsers(List<Integer> userIds) {
        conversations.clear();
        if (adapter != null) adapter.notifyDataSetChanged();
        showEmptyState(userIds == null || userIds.isEmpty());

        if (userIds == null || userIds.isEmpty()) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        AtomicInteger remaining = new AtomicInteger(userIds.size());

        for (Integer id : userIds) {
            db.collection("users")
                    .document(String.valueOf(id))
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            Hrachexpand user = doc.toObject(Hrachexpand.class);
                            if (user != null) {
                                InboxAdapter.ConversationItem item =
                                        new InboxAdapter.ConversationItem(user);
                                conversations.add(item);

                                // Load last message preview for this conversation
                                loadLastMessage(item, () -> {
                                    if (adapter != null) adapter.notifyDataSetChanged();
                                    showEmptyState(conversations.isEmpty());
                                });
                            }
                        }
                        if (remaining.decrementAndGet() == 0) {
                            if (adapter != null) adapter.notifyDataSetChanged();
                            showEmptyState(conversations.isEmpty());
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("InboxFragment", "Failed to load user " + id, e);
                        remaining.decrementAndGet();
                    });
        }
    }

    /**
     * Fetches the most-recent message between the current user and the matched
     * user, then updates the {@link InboxAdapter.ConversationItem} in place.
     */
    private void loadLastMessage(InboxAdapter.ConversationItem item, Runnable onDone) {
        if (currentUserId == -1) { onDone.run(); return; }

        Hrachexpand proxy = new Hrachexpand();
        proxy.setId(currentUserId);

        proxy.loadChatWith(item.user.getId(), messages -> {
            if (!messages.isEmpty()) {
                Message last = messages.get(messages.size() - 1);
                item.lastMessage   = last.getMessageText();
                item.lastTimestamp = last.getTimestamp();
            }

            // Count unread messages from this user
            proxy.getUnreadCount(item.user.getId(), count -> {
                item.unreadCount = count;
                onDone.run();
            });
        });
    }

    private void showEmptyState(boolean empty) {
        if (emptyState == null) return;
        emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        emptyState.setText("No matches yet.\nKeep swiping! 💫");
    }
}