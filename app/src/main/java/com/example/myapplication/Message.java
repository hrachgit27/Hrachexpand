package com.example.myapplication;

public class Message {
    private int senderId;
    private int receiverId;
    private String messageText;
    private long timestamp;

    public Message() {}  // Needed for Firestore

    public Message(int senderId, int receiverId, String messageText, long timestamp) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.messageText = messageText;
        this.timestamp = timestamp;
    }

    public int getSenderId() { return senderId; }
    public int getReceiverId() { return receiverId; }
    public String getMessageText() { return messageText; }
    public long getTimestamp() { return timestamp; }
}
