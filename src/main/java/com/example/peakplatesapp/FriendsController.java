package com.example.peakplatesapp;

import javafx.fxml.FXML;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class FriendsController {

    @FXML private TextField searchField;
    @FXML private VBox usersContainer;
    @FXML private VBox friendsContainer;
    @FXML private Label usersCountLabel;
    @FXML private Label friendsCountLabel;

    private MainApp mainApp;
    private String userId;
    private List<String> currentFriendIds = new ArrayList<>();

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void loadFriends() {
        if (userId == null) {
            System.err.println("User ID not set");
            return;
        }

        usersContainer.getChildren().clear();
        friendsContainer.getChildren().clear();
        Label loading = new Label("Loading users...");
        usersContainer.getChildren().add(loading);
        friendsContainer.getChildren().add(new Label("Loading friends..."));

        new Thread(() -> {
            try {
                // Load all users (for friend list)
                QuerySnapshot usersSnapshot = FirestoreContext.getFirestore()
                        .collection("users")
                        .get()
                        .get();

                List<String> allUserIds = new ArrayList<>();
                List<String> allUsernames = new ArrayList<>();
                currentFriendIds.clear();
                List<String> friendUserIds = new ArrayList<>();
                List<String> friendUsernames = new ArrayList<>();

                for (QueryDocumentSnapshot doc : usersSnapshot.getDocuments()) {
                    String uid = doc.getId();
                    String username = doc.getString("username");

                    // Skip current user
                    if (!uid.equals(userId)) {
                        allUserIds.add(uid);
                        allUsernames.add(username != null ? username : uid);
                    }
                }

                // Load existing friends
                try {
                    QuerySnapshot friendsSnapshot = FirestoreContext.getFirestore()
                            .collection("friendships")
                            .whereEqualTo("user1", userId)
                            .get()
                            .get();

                    for (QueryDocumentSnapshot doc : friendsSnapshot.getDocuments()) {
                        String user2 = doc.getString("user2");
                        if (user2 != null && !currentFriendIds.contains(user2)) {
                            currentFriendIds.add(user2);
                        }
                    }

                    friendsSnapshot = FirestoreContext.getFirestore()
                            .collection("friendships")
                            .whereEqualTo("user2", userId)
                            .get()
                            .get();

                    for (QueryDocumentSnapshot doc : friendsSnapshot.getDocuments()) {
                        String user1 = doc.getString("user1");
                        if (user1 != null && !currentFriendIds.contains(user1)) {
                            currentFriendIds.add(user1);
                        }
                    }

                    // Get friend usernames
                    for (int i = 0; i < allUserIds.size(); i++) {
                        if (currentFriendIds.contains(allUserIds.get(i))) {
                            friendUserIds.add(allUserIds.get(i));
                            friendUsernames.add(allUsernames.get(i));
                        }
                    }
                } catch (Exception ex) {
                    System.err.println("Error loading friendships: " + ex.getMessage());
                }

                final List<String> finalUserIds = allUserIds;
                final List<String> finalUsernames = allUsernames;
                final List<String> finalFriendUserIds = friendUserIds;
                final List<String> finalFriendUsernames = friendUsernames;

                Platform.runLater(() -> {
                    usersContainer.getChildren().clear();
                    friendsContainer.getChildren().clear();

                    // Display friends section
                    if (finalFriendUserIds.isEmpty()) {
                        friendsContainer.getChildren().add(new Label("You have no friends yet."));
                        friendsCountLabel.setText("0 friends");
                    } else {
                        for (int i = 0; i < finalFriendUserIds.size(); i++) {
                            String uid = finalFriendUserIds.get(i);
                            String username = finalFriendUsernames.get(i);
                            friendsContainer.getChildren().add(createFriendCard(uid, username));
                        }
                        friendsCountLabel.setText(finalFriendUserIds.size() + " friend" + (finalFriendUserIds.size() != 1 ? "s" : ""));
                    }

                    // Display users section
                    if (finalUserIds.isEmpty()) {
                        usersContainer.getChildren().add(new Label("No other users available."));
                        usersCountLabel.setText("0 users");
                    } else {
                        for (int i = 0; i < finalUserIds.size(); i++) {
                            String uid = finalUserIds.get(i);
                            String username = finalUsernames.get(i);
                            usersContainer.getChildren().add(createUserCard(uid, username));
                        }
                        usersCountLabel.setText(finalUserIds.size() + " user" + (finalUserIds.size() != 1 ? "s" : ""));
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    usersContainer.getChildren().clear();
                    usersContainer.getChildren().add(new Label("Error loading users: " + e.getMessage()));
                });
            }
        }).start();
    }

    @FXML
    private void handleSearch() {
        String query = searchField.getText() != null ? searchField.getText().trim().toLowerCase() : "";

        usersContainer.getChildren().clear();
        if (query.isEmpty()) {
            loadFriends();
            return;
        }

        Label searching = new Label("Searching...");
        usersContainer.getChildren().add(searching);

        new Thread(() -> {
            try {
                QuerySnapshot usersSnapshot = FirestoreContext.getFirestore()
                        .collection("users")
                        .get()
                        .get();

                List<String> matchIds = new ArrayList<>();
                List<String> matchNames = new ArrayList<>();

                for (QueryDocumentSnapshot doc : usersSnapshot.getDocuments()) {
                    String uid = doc.getId();
                    String username = doc.getString("username");

                    if (!uid.equals(userId) && username != null && username.toLowerCase().contains(query)) {
                        matchIds.add(uid);
                        matchNames.add(username);
                    }
                }

                final List<String> finalIds = matchIds;
                final List<String> finalNames = matchNames;

                Platform.runLater(() -> {
                    usersContainer.getChildren().clear();
                    if (finalIds.isEmpty()) {
                        usersContainer.getChildren().add(new Label("No users found matching: " + query));
                        usersCountLabel.setText("0 results");
                    } else {
                        for (int i = 0; i < finalIds.size(); i++) {
                            usersContainer.getChildren().add(createUserCard(finalIds.get(i), finalNames.get(i)));
                        }
                        usersCountLabel.setText(finalIds.size() + " result" + (finalIds.size() != 1 ? "s" : ""));
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    usersContainer.getChildren().clear();
                    usersContainer.getChildren().add(new Label("Error searching: " + e.getMessage()));
                });
            }
        }).start();
    }

    private HBox createFriendCard(String friendUserId, String friendUsername) {
        HBox card = new HBox(10);
        card.setPadding(new Insets(10));
        card.setStyle("-fx-border-color: #4CAF50; -fx-border-width: 1; -fx-background-color: #e8f5e9;");

        Label userLabel = new Label("✓ " + friendUsername);
        userLabel.setStyle("-fx-font-size: 14; -fx-text-fill: #2e7d32;");

        Button removeButton = new Button("➖ Remove");
        removeButton.setStyle("-fx-font-size: 12; -fx-padding: 8 15; -fx-background-color: #f44336; -fx-text-fill: white;");
        removeButton.setOnAction(e -> handleRemoveFriend(friendUserId, friendUsername));

        card.getChildren().addAll(userLabel, removeButton);
        return card;
    }

    private void handleRemoveFriend(String friendUserId, String friendUsername) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Remove Friend");
        confirmation.setHeaderText(null);
        confirmation.setContentText("Are you sure you want to remove " + friendUsername + " from your friends?");
        if (confirmation.showAndWait().get() != javafx.scene.control.ButtonType.OK) {
            return;
        }

        new Thread(() -> {
            try {
                // Find and delete friendship records
                QuerySnapshot friendships1 = FirestoreContext.getFirestore()
                        .collection("friendships")
                        .whereEqualTo("user1", userId)
                        .whereEqualTo("user2", friendUserId)
                        .get()
                        .get();

                for (QueryDocumentSnapshot doc : friendships1.getDocuments()) {
                    FirestoreContext.getFirestore()
                            .collection("friendships")
                            .document(doc.getId())
                            .delete()
                            .get();
                }

                QuerySnapshot friendships2 = FirestoreContext.getFirestore()
                        .collection("friendships")
                        .whereEqualTo("user1", friendUserId)
                        .whereEqualTo("user2", userId)
                        .get()
                        .get();

                for (QueryDocumentSnapshot doc : friendships2.getDocuments()) {
                    FirestoreContext.getFirestore()
                            .collection("friendships")
                            .document(doc.getId())
                            .delete()
                            .get();
                }

                Platform.runLater(() -> {
                    showAlert("Friend Removed", friendUsername + " has been removed from your friends.");
                    loadFriends();
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showAlert("Error", "Failed to remove friend: " + e.getMessage()));
            }
        }).start();
    }

    private HBox createUserCard(String targetUserId, String targetUsername) {
        HBox card = new HBox(10);
        card.setPadding(new Insets(10));
        card.setStyle("-fx-border-color: #ddd; -fx-border-width: 1; -fx-background-color: #f9f9f9;");

        Label userLabel = new Label("👤 " + targetUsername);
        userLabel.setStyle("-fx-font-size: 14;");

        boolean isFriend = currentFriendIds.contains(targetUserId);

        if (isFriend) {
            Button friendButton = new Button("✓ Friends");
            friendButton.setStyle("-fx-font-size: 12; -fx-padding: 8 15; -fx-background-color: #4CAF50; -fx-text-fill: white;");
            friendButton.setDisable(true);
            card.getChildren().addAll(userLabel, friendButton);
        } else {
            Button addButton = new Button("➕ Add Friend");
            addButton.setStyle("-fx-font-size: 12; -fx-padding: 8 15; -fx-background-color: #2196F3; -fx-text-fill: white;");
            addButton.setOnAction(e -> handleAddFriend(targetUserId, targetUsername));
            card.getChildren().addAll(userLabel, addButton);
        }

        return card;
    }

    private void handleAddFriend(String targetUserId, String targetUsername) {
        new Thread(() -> {
            try {
                // Check if there's already a pending friend request between these two users
                QuerySnapshot existingRequests = FirestoreContext.getFirestore()
                        .collection("friendRequests")
                        .whereEqualTo("fromUserId", userId)
                        .whereEqualTo("toUserId", targetUserId)
                        .whereEqualTo("status", "pending")
                        .get()
                        .get();

                if (!existingRequests.isEmpty()) {
                    Platform.runLater(() -> showAlert("Already Sent", "You already sent a friend request to " + targetUsername));
                    return;
                }

                // Check if they're already friends
                QuerySnapshot existingFriendship1 = FirestoreContext.getFirestore()
                        .collection("friendships")
                        .whereEqualTo("user1", userId)
                        .whereEqualTo("user2", targetUserId)
                        .get()
                        .get();

                QuerySnapshot existingFriendship2 = FirestoreContext.getFirestore()
                        .collection("friendships")
                        .whereEqualTo("user1", targetUserId)
                        .whereEqualTo("user2", userId)
                        .get()
                        .get();

                if (!existingFriendship1.isEmpty() || !existingFriendship2.isEmpty()) {
                    Platform.runLater(() -> showAlert("Already Friends", "You are already friends with " + targetUsername));
                    return;
                }

                // Create friend request
                FriendRequest req = new FriendRequest(userId, null, targetUserId, targetUsername);
                FirestoreContext.getFirestore()
                        .collection("friendRequests")
                        .document()
                        .set(req)
                        .get();

                Platform.runLater(() -> {
                    showAlert("Friend Request Sent", "Friend request sent to " + targetUsername + "!");
                    loadFriends();
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showAlert("Error", "Failed to send friend request: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    public void handleBackToDashboard() {
        try {
            if (mainApp != null && userId != null) {
                mainApp.switchToDashboard(userId);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Cannot navigate to dashboard.");
        }
    }

    @FXML
    public void handleViewFavorites() {
        try {
            if (mainApp != null && userId != null) {
                mainApp.switchToFavorites(userId);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Cannot navigate to favorites.");
        }
    }

    @FXML
    public void handleViewSharedWithMe() {
        try {
            if (mainApp != null && userId != null) {
                mainApp.switchToSharedWithMe(userId);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Cannot navigate to shared with me.");
        }
    }

    @FXML
    public void handleFriendRequests() {
        try {
            if (mainApp != null && userId != null) {
                mainApp.switchToFriendRequests(userId);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Cannot navigate to friend requests.");
        }
    }

    @FXML
    public void handleLogout() {
        try {
            if (mainApp != null) {
                mainApp.switchToView("Login");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Cannot logout.");
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
