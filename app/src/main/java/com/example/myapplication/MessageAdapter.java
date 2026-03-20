package com.example.myapplication;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    // View types
    private static final int TYPE_DATE_SEPARATOR = 0;
    private static final int TYPE_MESSAGE        = 1;

    /** A flat list item is either a real Message or a date-separator String. */
    private final ArrayList<Object> items = new ArrayList<>();
    private final ArrayList<Message> messages;
    private final int currentUserId;

    private static final SimpleDateFormat TIME_FMT =
            new SimpleDateFormat("h:mm a", Locale.getDefault());
    private static final SimpleDateFormat DATE_FMT =
            new SimpleDateFormat("EEEE, MMM d", Locale.getDefault());

    public MessageAdapter(ArrayList<Message> messages, int currentUserId) {
        this.messages      = messages;
        this.currentUserId = currentUserId;
        rebuildItems();
    }

    // ── Item list ─────────────────────────────────────────────────────────────

    /** Rebuild the flat display list inserting date-separator strings as needed. */
    private void rebuildItems() {
        items.clear();
        String lastDate = null;
        for (Message m : messages) {
            String dateLabel = dateLabel(m.getTimestamp());
            if (!dateLabel.equals(lastDate)) {
                items.add(dateLabel);   // separator
                lastDate = dateLabel;
            }
            items.add(m);
        }
    }

    /**
     * Call this instead of notifyDataSetChanged() whenever the message list changes.
     * Rebuilds date-separator items then notifies the RecyclerView.
     */
    public void updateMessages() {
        rebuildItems();
        notifyDataSetChanged();
    }

    // ── ViewHolder types ──────────────────────────────────────────────────────

    @Override
    public int getItemViewType(int position) {
        return (items.get(position) instanceof String) ? TYPE_DATE_SEPARATOR : TYPE_MESSAGE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_DATE_SEPARATOR) {
            View v = inf.inflate(R.layout.item_date_separator, parent, false);
            return new DateViewHolder(v);
        } else {
            View v = inf.inflate(R.layout.item_message, parent, false);
            return new MessageViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof DateViewHolder) {
            ((DateViewHolder) holder).dateText.setText((String) items.get(position));
        } else {
            bindMessage((MessageViewHolder) holder, (Message) items.get(position));
        }
    }

    private void bindMessage(MessageViewHolder holder, Message msg) {
        holder.messageText.setText(msg.getMessageText());
        holder.timeText.setText(TIME_FMT.format(new Date(msg.getTimestamp())));

        boolean isMine = msg.getSenderId() == currentUserId;

        // Bubble alignment
        LinearLayout.LayoutParams bubbleParams =
                (LinearLayout.LayoutParams) holder.bubbleContainer.getLayoutParams();
        bubbleParams.gravity = isMine ? Gravity.END : Gravity.START;
        holder.bubbleContainer.setLayoutParams(bubbleParams);

        // Bubble style
        holder.messageText.setBackgroundResource(
                isMine ? R.drawable.message_bg_sent : R.drawable.message_bg_received);

        // Read receipt — show a small "✓✓" only on sent messages
        if (holder.readReceipt != null) {
            if (isMine) {
                holder.readReceipt.setVisibility(View.VISIBLE);
                holder.readReceipt.setText(msg.isRead() ? "✓✓" : "✓");
                holder.readReceipt.setAlpha(msg.isRead() ? 1f : 0.5f);
            } else {
                holder.readReceipt.setVisibility(View.GONE);
            }
        }

        // Time label alignment
        LinearLayout.LayoutParams timeParams =
                (LinearLayout.LayoutParams) holder.timeText.getLayoutParams();
        timeParams.gravity = isMine ? Gravity.END : Gravity.START;
        holder.timeText.setLayoutParams(timeParams);
    }

    @Override
    public int getItemCount() { return items.size(); }

    // ── Utility ───────────────────────────────────────────────────────────────

    private static String dateLabel(long millis) {
        Calendar msgCal = Calendar.getInstance();
        msgCal.setTimeInMillis(millis);
        Calendar today = Calendar.getInstance();

        if (isSameDay(msgCal, today)) return "Today";

        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);
        if (isSameDay(msgCal, yesterday)) return "Yesterday";

        return DATE_FMT.format(new Date(millis));
    }

    private static boolean isSameDay(Calendar a, Calendar b) {
        return a.get(Calendar.YEAR)       == b.get(Calendar.YEAR)
            && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }

    // ── ViewHolders ───────────────────────────────────────────────────────────

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        LinearLayout bubbleContainer;
        TextView     messageText;
        TextView     timeText;
        TextView     readReceipt; // may be null if not in layout

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            bubbleContainer = itemView.findViewById(R.id.bubbleContainer);
            messageText     = itemView.findViewById(R.id.messageText);
            timeText        = itemView.findViewById(R.id.messageTime);
            readReceipt     = itemView.findViewById(R.id.readReceipt); // optional
        }
    }

    static class DateViewHolder extends RecyclerView.ViewHolder {
        TextView dateText;
        DateViewHolder(@NonNull View itemView) {
            super(itemView);
            dateText = itemView.findViewById(R.id.dateSeparatorText);
        }
    }
}
