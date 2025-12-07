package com.example.peakplatesapp;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import com.google.cloud.firestore.Blob;

public class Recipe implements Serializable {
    private String id;
    private String userId;
    private String username;
    private String description;
    private String title;
    private String imagePath;
    private Blob imageData;
    private long timestamp;
    private int likes;
    private int favorites;
    private int shares;
    private List<String> likedByUsers;
    private List<String> favoriteByUsers;
    private List<String> sharedWith;

    public Recipe() {
        this.likes = 0;
        this.favorites = 0;
        this.shares = 0;
        this.likedByUsers = new ArrayList<>();
        this.favoriteByUsers = new ArrayList<>();
        this.sharedWith = new ArrayList<>();
        this.timestamp = System.currentTimeMillis();
    }

    public Recipe(String userId, String username, String description, String imagePath) {
        this();
        this.userId = userId;
        this.username = username;
        this.description = description;
        this.imagePath = imagePath;
    }

    public Recipe(String userId, String username, String title, String description, String imagePath) {
        this(userId, username, description, imagePath);
        this.title = title;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public Blob getImageData() {
        return imageData;
    }

    public void setImageData(Blob imageData) {
        this.imageData = imageData;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getLikes() {
        return likes;
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }

    public int getFavorites() {
        return favorites;
    }

    public void setFavorites(int favorites) {
        this.favorites = favorites;
    }

    public int getShares() {
        return shares;
    }

    public void setShares(int shares) {
        this.shares = shares;
    }

    public List<String> getLikedByUsers() {
        return likedByUsers;
    }

    public void setLikedByUsers(List<String> likedByUsers) {
        this.likedByUsers = likedByUsers;
    }

    public List<String> getFavoriteByUsers() {
        return favoriteByUsers;
    }

    public void setFavoriteByUsers(List<String> favoriteByUsers) {
        this.favoriteByUsers = favoriteByUsers;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<String> getSharedWith() {
        if (sharedWith == null) {
            sharedWith = new ArrayList<>();
        }
        return sharedWith;
    }

    public void setSharedWith(List<String> sharedWith) {
        this.sharedWith = sharedWith;
    }
}
