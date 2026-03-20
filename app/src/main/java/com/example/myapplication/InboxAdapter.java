package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class InboxAdapter extends RecyclerView.Adapter<InboxAdapter.ViewHolder> {

    public interface OnUserClickListener {
        void onUserClick(Hrachexpand user);
    }

    private final List<ConversationItem> conversations;
    private final OnUserClickListener    listener;

    private static final SimpleDateFormat TIME_FMT =
            new SimpleDateFormat("h:mm a",  Locale.getDefault());
    private static final SimpleDateFormat DATE_FMT =
            new SimpleDateFormat("MMM d",   Locale.getDefault());

    public InboxAdapter(List<ConversationItem> conversations, OnUserClickListener listener) {
        this.conversations = conversations;
        this.listener      = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_conversation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ConversationItem item = conversations.get(position);
        Hrachexpand      user = item.user;

        // Name + age
        holder.nameText.setText(user.getFirstName() + ", " + user.getAge());

        // Last message preview
        if (item.lastMessage != null && !item.lastMessage.isEmpty()) {
            holder.lastMessageText.setText(item.lastMessage);
            holder.lastMessageText.setAlpha(item.unreadCount > 0 ? 1f : 0.6f);
        } else {
            holder.lastMessageText.setText("Say hello 👋");
            holder.lastMessageText.setAlpha(0.5f);
        }

        // Timestamp
        if (item.lastTimestamp > 0) {
            holder.timeText.setVisibility(View.VISIBLE);
            holder.timeText.setText(formatTime(item.lastTimestamp));
        } else {
            holder.timeText.setVisibility(View.GONE);
        }

        // Unread badge
        if (item.unreadCount > 0) {
            holder.unreadBadge.setVisibility(View.VISIBLE);
            holder.unreadBadge.setText(item.unreadCount > 99 ? "99+" : String.valueOf(item.unreadCount));
        } else {
            holder.unreadBadge.setVisibility(View.GONE);
        }

        // Profile photo — circular crop
        Glide.with(holder.itemView.getContext())
                .load(user.getFilepath())
                .transform(new CircleCrop())
                .placeholder(R.drawable.ic_profile_placeholder)
                .error(R.drawable.ic_profile_placeholder)
                .into(holder.profileImage);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onUserClick(user);
        });
    }

    @Override
    public int getItemCount() { return conversations.size(); }

    // ── Utility ──────────────────────────────────────────────────────────────

    private static String formatTime(long millis) {
        Calendar msgCal = Calendar.getInstance();
        msgCal.setTimeInMillis(millis);
        Calendar today = Calendar.getInstance();
        if (msgCal.get(Calendar.YEAR)       == today.get(Calendar.YEAR)
                && msgCal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
            return TIME_FMT.format(new Date(millis));
        }
        return DATE_FMT.format(new Date(millis));
    }

    // ── ViewHolder ────────────────────────────────────────────────────────────

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView profileImage;
        TextView  nameText;
        TextView  lastMessageText;
        TextView  timeText;
        TextView  unreadBadge;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage    = itemView.findViewById(R.id.userImage);
            nameText        = itemView.findViewById(R.id.userName);
            lastMessageText = itemView.findViewById(R.id.lastMessage);
            timeText        = itemView.findViewById(R.id.conversationTime);
            unreadBadge     = itemView.findViewById(R.id.unreadBadge);
        }
    }

    // ── Data model for a single inbox row ─────────────────────────────────────

    public static class ConversationItem {
        public final Hrachexpand user;
        public       String      lastMessage;
        public       long        lastTimestamp;
        public       int         unreadCount;

        public ConversationItem(Hrachexpand user) {
            this.user          = user;
            this.lastMessage   = null;
            this.lastTimestamp = 0;
            this.unreadCount   = 0;
        }
    }
}