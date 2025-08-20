package com.example.myapplication;

import java.util.*;

import android.content.Context;
import android.widget.Toast;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.DocumentSnapshot;

public class Hrachexpand {
    private int age;
    private int id;
    private String prefrence;
    private String username;
    private String password;
    private String FirstName, LastName;
    private String bio;
    private String filepath;
    ArrayList<Hrachexpand> loadedUsers = new ArrayList<>();
    private ArrayList<Integer> Likes = new ArrayList<Integer>();
    private ArrayList<Integer> Dislikes = new ArrayList<Integer>();
    private ArrayList<Integer> Matches = new ArrayList<Integer>();
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    public Hrachexpand()
    {
        id = 0;
        age = 0;
        prefrence = "";
        bio = "";
        FirstName = "";
        LastName = "";
        username = "";
        password = "";
    }

    public Hrachexpand(String u, String pa, String f, String l, int a, String pre, String b, String path)
    {
        username = u;
        password = pa;
        FirstName = f;
        LastName = l;
        age = a;
        prefrence = pre;
        bio = b;
        filepath = path;
        id = (int) (System.currentTimeMillis() % 1000000);
        addUser();
    }

    public void changeAge(int a)
    {
        age = a;
    }

    public void changePref(String p)
    {
        prefrence = p;
    }

    public void setUsername(String a) {username = a;}

    public void setPassword(String a) {password = a;}

    public void setFirstName(String a) {FirstName = a;}

    public void setLastName(String a) {LastName = a;}

    public void setPrefrence(String a) {prefrence = a;}

    public void setBio(String a) {bio = a;}

    public void setFilepath(String a) {filepath = a;}

    public void setAge(int a) {age = a;}

    public void setId(int id) {
        this.id = id;
    }

    public void setLikes(ArrayList<Integer> likes) {
        this.Likes = likes;
    }

    public void setDislikes(ArrayList<Integer> dislikes) {
        this.Dislikes = dislikes;
    }

    public void setMatchedUsers(ArrayList<Integer> matches) {
        this.Matches = matches;
    }

    public void loadMatches(Context context, Runnable onComplete) {
        db.collection("users")
                .document(String.valueOf(this.id))
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Matches.clear();
                    if (documentSnapshot.exists()) {
                        // Grab the "matches" array from Firestore
                        List<Long> matchIds = (List<Long>) documentSnapshot.get("matches");
                        if (matchIds != null) {
                            for (Long matchId : matchIds) {
                                Matches.add(matchId.intValue());
                            }
                        }
                    }
                    if (onComplete != null) {
                        onComplete.run();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Error loading users: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    public void numLikes(int otheruserid)
    {
        if (!Likes.contains(otheruserid))
            Likes.add(otheruserid);
    }

    public void numDislikes(int otheruserid)
    {
        if (!Dislikes.contains(otheruserid))
            Dislikes.add(otheruserid);
    }

    public void numMatches(int user)
    {
        if (!Matches.contains(user))
            Matches.add(user);
    }

    public void setLoadedUsers(Context context, Runnable onComplete) {
        db.collection("users").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    loadedUsers.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Hrachexpand user = document.toObject(Hrachexpand.class);
                        if (user != null && user.getId() != this.id && !getLikes().contains(user.getId())) {
                            loadedUsers.add(user);
                        }
                    }
                    if (onComplete != null) {
                        onComplete.run();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Error loading users: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    public String getPrefrence()
    {
        return prefrence;
    }

    public String getUsername(){
        return username;
    }

    public String getPassword(){
        return password;
    }

    public int getAge()
    {
        return age;
    }

    public String getBio()
    {
        return bio;
    }

    public String getFilepath() {
        return filepath;
    }

    public String getFirstName(){
        return FirstName;
    }

    public String getLastName(){
        return LastName;
    }

    public int getId()
    {
        return id;
    }

    public ArrayList<Hrachexpand> getLoadedUsers() {
        return loadedUsers;
    }

    public ArrayList<Integer> getLikes() {
        return Likes;
    }

    public ArrayList<Integer> getDislikes() {
        return Dislikes;
    }

    public ArrayList<Integer> getMatches() {
        return Matches;
    }

    public boolean matchMaker(Hrachexpand other)
    {
        return this.Likes.contains(other.getId()) && other.Likes.contains(this.getId());
    }

    public String toString() {
        String result = this.getFirstName() + "\n" +
                this.getLastName() + "\n" +
                "Age: " + this.getAge() + "\n" +
                "Prefrence: " + this.getPrefrence() + "\n" +
                this.getBio() + "\n";

        return result;
    }

    public void addUser() {
        db.collection("users").document(String.valueOf(id)).set(this);
    }

    public interface LoginCallback {
        void onLoginResult(boolean success, Hrachexpand user);
    }

    public void login(String username, String password, Context context, LoginCallback callback) {
        db.collection("users").whereEqualTo("username", username).get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                            Hrachexpand user = document.toObject(Hrachexpand.class);
                            if (user != null && user.getPassword().equals(password)) {
                                callback.onLoginResult(true, user);
                                return;
                            }
                        }
                        Toast.makeText(context, "Invalid password", Toast.LENGTH_SHORT).show();
                        callback.onLoginResult(false, null);
                    } else {
                        Toast.makeText(context, "User not found", Toast.LENGTH_SHORT).show();
                        callback.onLoginResult(false, null);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Database error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    callback.onLoginResult(false, null);
                });
    }

    public void saveToFirebase(Context context) {
        db.document(String.valueOf(this.id)).set(this)
                .addOnSuccessListener(aVoid -> {
                    if (context != null) {
                        Toast.makeText(context, "User saved successfully", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (context != null) {
                        Toast.makeText(context, "Error saving user: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public void sendMessage(int receiverId, String messageText) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> msgData = new HashMap<>();
        msgData.put("senderId", this.id);
        msgData.put("receiverId", receiverId);
        msgData.put("messageText", messageText);
        msgData.put("timestamp", System.currentTimeMillis());

        db.collection("messages").add(msgData);
    }

    public void loadChatWith(int otherUserId, OnMessagesLoaded callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("messages")
                .whereIn("senderId", Arrays.asList(this.id, otherUserId))
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    ArrayList<Message> chat = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot) {
                        Message m = doc.toObject(Message.class);
                        if ((m.getSenderId() == this.id && m.getReceiverId() == otherUserId)
                                || (m.getSenderId() == otherUserId && m.getReceiverId() == this.id)) {
                            chat.add(m);
                        }
                    }
                    // Sort by timestamp
                    chat.sort(Comparator.comparingLong(Message::getTimestamp));
                    callback.onMessagesLoaded(chat);
                });
    }

    public interface OnMessagesLoaded {
        void onMessagesLoaded(ArrayList<Message> messages);
    }
}