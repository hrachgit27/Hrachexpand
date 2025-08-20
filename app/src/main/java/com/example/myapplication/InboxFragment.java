package com.example.myapplication;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class InboxFragment extends Fragment {

    private RecyclerView recyclerView;
    private InboxAdapter adapter;
    // Keep ONE mutable list that the adapter holds a reference to
    private final List<Hrachexpand> matchedUsers = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.inbox_activity, container, false);

        recyclerView = view.findViewById(R.id.inboxRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new InboxAdapter(matchedUsers, user -> {
            // handle click
        });
        recyclerView.setAdapter(adapter);

        return view;
    }

    // Call this when you load/refresh matches
    public void setMatchedUsers(List<Integer> userIds) {
        matchedUsers.clear();  // keep the same list reference

        if (userIds == null || userIds.isEmpty()) {
            if (adapter != null) adapter.notifyDataSetChanged();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        for (Integer id : userIds) {
            db.collection("users")
                    .document(String.valueOf(id))
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Hrachexpand user = documentSnapshot.toObject(Hrachexpand.class);
                            if (user != null) {
                                matchedUsers.add(user);
                                if (adapter != null) adapter.notifyDataSetChanged();
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Firestore", "Failed to load user with id " + id, e);
                    });
        }
    }
}