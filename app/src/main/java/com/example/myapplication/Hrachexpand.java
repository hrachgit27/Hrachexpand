package com.example.myapplication;

import java.util.*;

import android.content.Context;
import android.widget.Toast;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.DocumentSnapshot;

public class Hrachexpand {
    private int    age;
    private int    id;
    private String preference;
    private String username;
    private String password;
    private String firstName, lastName;
    private String bio;
    private String filepath;

    // Runtime-only (not persisted to Firestore)
    transient ArrayList<Hrachexpand> loadedUsers = new ArrayList<>();

    private ArrayList<Integer> likes    = new ArrayList<>();
    private ArrayList<Integer> dislikes = new ArrayList<>();
    private ArrayList<Integer> matches  = new ArrayList<>();

    private transient FirebaseFirestore db = FirebaseFirestore.getInstance();

    // ─── Constructors ────────────────────────────────────────────────────────

    public Hrachexpand() {
        id         = 0;
        age        = 0;
        preference = "";
        bio        = "";
        firstName  = "";
        lastName   = "";
        username   = "";
        password   = "";
    }

    /**
     * @param u    username
     * @param pa   password
     * @param f    firstName
     * @param l    lastName
     * @param a    age
     * @param pre  preference (e.g. "Women", "Men", "Everyone")
     * @param b    bio
     * @param path profile image URL/path
     */
    public Hrachexpand(String u, String pa, String f, String l, int a,
                       String pre, String b, String path) {
        username   = u;
        password   = pa;
        firstName  = f;
        lastName   = l;
        age        = a;
        preference = pre;
        bio        = b;
        filepath   = path;
        id         = (int) (System.currentTimeMillis() % 1000000);
        addUser();
    }

    // ─── Setters ─────────────────────────────────────────────────────────────

    public void setUsername(String a)   { username   = a; }
    public void setPassword(String a)   { password   = a; }
    public void setFirstName(String a)  { firstName  = a; }
    public void setLastName(String a)   { lastName   = a; }
    public void setPreference(String a) { preference = a; }
    public void setBio(String a)        { bio        = a; }
    public void setFilepath(String a)   { filepath   = a; }
    public void setAge(int a)           { age        = a; }
    public void setId(int id)           { this.id    = id; }

    public void setLikes(ArrayList<Integer> likes)      { this.likes    = likes;   }
    public void setDislikes(ArrayList<Integer> dislikes){ this.dislikes = dislikes; }
    public void setMatches(ArrayList<Integer> matches)  { this.matches  = matches; }

    // Firestore field-name aliases kept for backward compatibility
    /** @deprecated use {@link #setPreference(String)} */
    public void setPrefrence(String a)                      { preference = a; }
    /** @deprecated use {@link #setMatches(ArrayList)} */
    public void setMatchedUsers(ArrayList<Integer> m)       { this.matches = m; }

    // ─── Getters ─────────────────────────────────────────────────────────────

    public String  getPreference()  { return preference; }
    /** @deprecated */
    public String  getPrefrence()   { return preference; }
    public String  getUsername()    { return username;   }
    public String  getPassword()    { return password;   }
    public int     getAge()         { return age;        }
    public String  getBio()         { return bio;        }
    public String  getFilepath()    { return filepath;   }
    public String  getFirstName()   { return firstName;  }
    public String  getLastName()    { return lastName;   }
    public int     getId()          { return id;         }

    public ArrayList<Hrachexpand> getLoadedUsers() { return loadedUsers; }
    public ArrayList<Integer>     getLikes()        { return likes;    }
    public ArrayList<Integer>     getDislikes()     { return dislikes; }
    public ArrayList<Integer>     getMatches()      { return matches;  }

    // ─── Firestore helpers ────────────────────────────────────────────────────

    /** Write a single field on this user's Firestore document. */
    public void updateField(String field, Object value) {
        db.collection("users")
                .document(String.valueOf(id))
                .update(field, value);
    }

    /** Persist the entire object (used on registration). */
    public void addUser() {
        db.collection("users").document(String.valueOf(id)).set(this);
    }

    /** Overwrite the entire document (used when updating profile). */
    public void saveToFirebase(Context context) {
        db.collection("users").document(String.valueOf(this.id)).set(this)
                .addOnSuccessListener(aVoid -> {
                    if (context != null)
                        Toast.makeText(context, "Profile saved", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    if (context != null)
                        Toast.makeText(context, "Save failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // ─── Auth ─────────────────────────────────────────────────────────────────

    public interface LoginCallback {
        void onLoginResult(boolean success, Hrachexpand user);
    }

    public void login(String username, String password, Context context, LoginCallback callback) {
        db.collection("users").whereEqualTo("username", username).get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            Hrachexpand user = doc.toObject(Hrachexpand.class);
                            if (user != null && user.getPassword().equals(password)) {
                                callback.onLoginResult(true, user);
                                return;
                            }
                        }
                        if (context != null)
                            Toast.makeText(context, "Incorrect password", Toast.LENGTH_SHORT).show();
                        callback.onLoginResult(false, null);
                    } else {
                        if (context != null)
                            Toast.makeText(context, "No account found", Toast.LENGTH_SHORT).show();
                        callback.onLoginResult(false, null);
                    }
                })
                .addOnFailureListener(e -> {
                    if (context != null)
                        Toast.makeText(context, "Network error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    callback.onLoginResult(false, null);
                });
    }

    // ─── Swipe deck ──────────────────────────────────────────────────────────

    /**
     * Loads all users the current user has NOT yet liked or disliked.
     * Optionally filtered by preference (e.g. "Men", "Women", "Everyone").
     */
    public void setLoadedUsers(Context context, Runnable onComplete) {
        db.collection("users").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    loadedUsers.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Hrachexpand user = document.toObject(Hrachexpand.class);
                        if (user == null) continue;
                        if (user.getId() == this.id) continue;
                        if (likes.contains(user.getId()))    continue;
                        if (dislikes.contains(user.getId())) continue;
                        loadedUsers.add(user);
                    }
                    if (onComplete != null) onComplete.run();
                })
                .addOnFailureListener(e -> {
                    if (context != null)
                        Toast.makeText(context, "Error loading profiles: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // ─── Like / Match (atomic) ────────────────────────────────────────────────

    public interface MatchCallback {
        /** Called after the like is persisted. @param isMatch true if both users now like each other. */
        void onResult(boolean isMatch);
    }

    /**
     * Likes {@code other}, persists it, then checks whether it created a match.
     * If it's a match, both users' match arrays are updated atomically via batch.
     */
    public void likeUser(Hrachexpand other, MatchCallback callback) {
        numLikes(other.getId());

        db.collection("users")
                .document(String.valueOf(this.id))
                .update("likes", likes)
                .addOnSuccessListener(aVoid -> {
                    // Check if other user already liked us
                    db.collection("users")
                            .document(String.valueOf(other.getId()))
                            .get()
                            .addOnSuccessListener(doc -> {
                                Hrachexpand freshOther = doc.toObject(Hrachexpand.class);
                                boolean isMatch = freshOther != null
                                        && freshOther.getLikes().contains(this.id);

                                if (isMatch) {
                                    numMatches(other.getId());
                                    // Use a batch so both match arrays update atomically
                                    db.runBatch(batch -> {
                                        batch.update(
                                                db.collection("users").document(String.valueOf(this.id)),
                                                "matches", matches
                                        );
                                        // Add current user to other's matches array in Firestore
                                        batch.update(
                                                db.collection("users").document(String.valueOf(other.getId())),
                                                "matches", FieldValue.arrayUnion(this.id)
                                        );
                                    }).addOnCompleteListener(t -> callback.onResult(true));
                                } else {
                                    callback.onResult(false);
                                }
                            })
                            .addOnFailureListener(e -> callback.onResult(false));
                })
                .addOnFailureListener(e -> callback.onResult(false));
    }

    /** Dislike a user and persist it. */
    public void dislikeUser(Hrachexpand other, Runnable onComplete) {
        numDislikes(other.getId());
        db.collection("users")
                .document(String.valueOf(this.id))
                .update("dislikes", dislikes)
                .addOnCompleteListener(t -> { if (onComplete != null) onComplete.run(); });
    }

    // ─── Match list ───────────────────────────────────────────────────────────

    public void loadMatches(Context context, Runnable onComplete) {
        db.collection("users")
                .document(String.valueOf(this.id))
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    matches.clear();
                    if (documentSnapshot.exists()) {
                        List<Long> ids = (List<Long>) documentSnapshot.get("matches");
                        if (ids != null) {
                            for (Long mid : ids) matches.add(mid.intValue());
                        }
                    }
                    if (onComplete != null) onComplete.run();
                })
                .addOnFailureListener(e -> {
                    if (context != null)
                        Toast.makeText(context, "Error loading matches: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // ─── In-memory helpers ────────────────────────────────────────────────────

    /** @deprecated use {@link #likeUser(Hrachexpand, MatchCallback)} */
    public void numLikes(int otherUserId) {
        if (!likes.contains(otherUserId)) likes.add(otherUserId);
    }

    /** @deprecated use {@link #dislikeUser(Hrachexpand, Runnable)} */
    public void numDislikes(int otherUserId) {
        if (!dislikes.contains(otherUserId)) dislikes.add(otherUserId);
    }

    public void numMatches(int userId) {
        if (!matches.contains(userId)) matches.add(userId);
    }

    /** Returns true if both users have liked each other (in-memory check). */
    public boolean matchMaker(Hrachexpand other) {
        return this.likes.contains(other.getId()) && other.getLikes().contains(this.id);
    }

    // ─── Messaging ────────────────────────────────────────────────────────────

    /** Sends a message and returns the new document ID via callback. */
    public void sendMessage(int receiverId, String messageText, OnMessageSent callback) {
        Map<String, Object> msgData = new HashMap<>();
        msgData.put("senderId",    this.id);
        msgData.put("receiverId",  receiverId);
        msgData.put("messageText", messageText);
        msgData.put("timestamp",   System.currentTimeMillis());
        msgData.put("isRead",      false);

        db.collection("messages")
                .add(msgData)
                .addOnSuccessListener(ref -> { if (callback != null) callback.onSent(ref.getId()); })
                .addOnFailureListener(e  -> { if (callback != null) callback.onSent(null); });
    }

    /** Overload without callback for backward compatibility. */
    public void sendMessage(int receiverId, String messageText) {
        sendMessage(receiverId, messageText, null);
    }

    public interface OnMessageSent {
        void onSent(String messageId); // null on failure
    }

    /**
     * Attaches a real-time snapshot listener for the conversation between
     * this user and {@code otherUserId}.
     *
     * IMPORTANT: the Firestore composite index
     *   messages / senderId ASC + receiverId ASC + timestamp ASC
     * must exist (create it once in the Firebase console or via the CLI).
     *
     * @return the {@link ListenerRegistration} — call {@code remove()} on it
     *         in {@code onStop()} / {@code onDestroy()} to avoid leaks.
     */
    public ListenerRegistration listenToChat(int otherUserId, OnMessagesLoaded callback) {
        // We'll do a combined query trick: store a "chatKey" field on each message
        // that is the sorted pair of IDs joined with "_".
        // But since existing data doesn't have that, we use a client-side filter.
        return db.collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null || querySnapshot == null) return;
                    ArrayList<Message> chat = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Message m = doc.toObject(Message.class);
                        if (m == null) continue;
                        boolean outgoing = m.getSenderId() == this.id && m.getReceiverId() == otherUserId;
                        boolean incoming = m.getSenderId() == otherUserId && m.getReceiverId() == this.id;
                        if (outgoing || incoming) chat.add(m);
                    }
                    callback.onMessagesLoaded(chat);

                    // Mark incoming unread messages as read
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Message m = doc.toObject(Message.class);
                        if (m != null && m.getSenderId() == otherUserId
                                && m.getReceiverId() == this.id && !m.isRead()) {
                            doc.getReference().update("isRead", true);
                        }
                    }
                });
    }

    /**
     * One-shot fetch of the conversation (used by InboxFragment for last-message previews).
     */
    public void loadChatWith(int otherUserId, OnMessagesLoaded callback) {
        ArrayList<Message> chat = new ArrayList<>();
        db.collection("messages")
                .whereEqualTo("senderId",   this.id)
                .whereEqualTo("receiverId", otherUserId)
                .get()
                .addOnSuccessListener(snap1 -> {
                    for (DocumentSnapshot doc : snap1) {
                        Message m = doc.toObject(Message.class);
                        if (m != null) chat.add(m);
                    }
                    db.collection("messages")
                            .whereEqualTo("senderId",   otherUserId)
                            .whereEqualTo("receiverId", this.id)
                            .get()
                            .addOnSuccessListener(snap2 -> {
                                for (DocumentSnapshot doc : snap2) {
                                    Message m = doc.toObject(Message.class);
                                    if (m != null) chat.add(m);
                                }
                                chat.sort(Comparator.comparingLong(Message::getTimestamp));
                                callback.onMessagesLoaded(chat);
                            })
                            .addOnFailureListener(e -> {
                                chat.sort(Comparator.comparingLong(Message::getTimestamp));
                                callback.onMessagesLoaded(chat);
                            });
                })
                .addOnFailureListener(e -> callback.onMessagesLoaded(new ArrayList<>()));
    }

    /**
     * Returns the number of unread messages sent TO this user FROM {@code otherUserId}.
     * Results via callback so the caller can update a badge.
     */
    public void getUnreadCount(int otherUserId, OnUnreadCount callback) {
        db.collection("messages")
                .whereEqualTo("senderId",   otherUserId)
                .whereEqualTo("receiverId", this.id)
                .whereEqualTo("isRead",     false)
                .get()
                .addOnSuccessListener(snap -> callback.onCount(snap.size()))
                .addOnFailureListener(e    -> callback.onCount(0));
    }

    public interface OnUnreadCount {
        void onCount(int count);
    }

    public interface OnMessagesLoaded {
        void onMessagesLoaded(ArrayList<Message> messages);
    }

    // ─── toString ─────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return firstName + " " + lastName + ", age " + age + "\n" + bio;
    }
}