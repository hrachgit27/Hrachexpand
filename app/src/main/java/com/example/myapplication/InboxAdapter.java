package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;

import java.util.List;

public class InboxAdapter extends RecyclerView.Adapter<InboxAdapter.ViewHolder> {

    public interface OnUserClickListener {
        void onUserClick(Hrachexpand user);
    }

    private List<Hrachexpand> matchedUsers;
    private OnUserClickListener listener;

    public InboxAdapter(List<Hrachexpand> matchedUsers, OnUserClickListener listener) {
        this.matchedUsers = matchedUsers;
        this.listener = listener;
    }

    @NonNull
    @Override
    public InboxAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_conversation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InboxAdapter.ViewHolder holder, int position) {
        Hrachexpand user = matchedUsers.get(position);
        holder.nameText.setText(user.getFirstName() + ", " + user.getAge());
        holder.lastMessageText.setText("Start chatting...");

        Glide.with(holder.itemView.getContext())
                .load(user.getFilepath())
                .into(holder.profileImage);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onUserClick(user);
            }
        });
    }

    @Override
    public int getItemCount() {
        return matchedUsers.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView profileImage;
        TextView nameText;
        TextView lastMessageText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.userImage);
            nameText = itemView.findViewById(R.id.userName);
            lastMessageText = itemView.findViewById(R.id.lastMessage);
        }
    }
}