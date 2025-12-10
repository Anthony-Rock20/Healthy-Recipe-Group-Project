package com.example.peakplatesapp;

import javafx.fxml.FXML;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.DocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class FriendRequestsController {

    @FXML private VBox requestsContainer;
    @FXML private Label requestCountLabel;

    private MainApp mainApp;
    private String userId;

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void loadRequests() {
        if (userId == null) {
            System.err.println("User ID not set");
            return;
        }

        requestsContainer.getChildren().clear();
        Label loading = new Label("Loading requests...");
        requestsContainer.getChildren().add(loading);

        new Thread(() -> {
            try {
                // Load pending friend requests for current user
                QuerySnapshot reqSnapshot = FirestoreContext.getFirestore()
                        .collection("friendRequests")
                        .whereEqualTo("toUserId", userId)
                        .whereEqualTo("status", "pending")
                        .get()
                        .get();

                List<String> requestIds = new ArrayList<>();
                List<String> fromUserIds = new ArrayList<>();
                List<String> fromUsernames = new ArrayList<>();

                for (QueryDocumentSnapshot doc : reqSnapshot.getDocuments()) {
                    String fromId = doc.getString("fromUserId");
                    String fromName = doc.getString("fromUsername");
                    if (fromId != null) {
                        requestIds.add(doc.getId());
                        fromUserIds.add(fromId);
                        fromUsernames.add(fromName != null ? fromName : fromId);
                    }
                }

                Platform.runLater(() -> {
                    requestsContainer.getChildren().clear();
                    if (requestIds.isEmpty()) {
                        requestsContainer.getChildren().add(new Label("No pending friend requests."));
                        requestCountLabel.setText("0 requests");
                    } else {
                        for (int i = 0; i < requestIds.size(); i++) {
                            requestsContainer.getChildren().add(
                                    createRequestCard(requestIds.get(i), fromUserIds.get(i), fromUsernames.get(i))
                            );
                        }
                        requestCountLabel.setText(requestIds.size() + " request" + (requestIds.size() != 1 ? "s" : ""));
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    requestsContainer.getChildren().clear();
                    requestsContainer.getChildren().add(new Label("Error loading requests: " + e.getMessage()));
                });
            }
        }).start();
    }

    private HBox createRequestCard(String requestId, String fromUserId, String fromUsername) {
        HBox card = new HBox(10);
        card.setPadding(new Insets(10));
        card.setStyle("-fx-border-color: #ddd; -fx-border-width: 1; -fx-background-color: #fff9c4;");

        Label userLabel = new Label("👤 " + fromUsername + " sent you a friend request");
        userLabel.setStyle("-fx-font-size: 14;");

        Button acceptButton = new Button("✓ Accept");
        acceptButton.setStyle("-fx-font-size: 12; -fx-padding: 8 15; -fx-background-color: #4CAF50; -fx-text-fill: white;");
        acceptButton.setOnAction(e -> handleAcceptRequest(requestId, fromUserId, fromUsername, card));

        Button declineButton = new Button("✗ Decline");
        declineButton.setStyle("-fx-font-size: 12; -fx-padding: 8 15; -fx-background-color: #f44336; -fx-text-fill: white;");
        declineButton.setOnAction(e -> handleDeclineRequest(requestId, card));

        card.getChildren().addAll(userLabel, acceptButton, declineButton);
        return card;
    }

    private void handleAcceptRequest(String requestId, String fromUserId, String fromUsername, HBox card) {
        new Thread(() -> {
            try {
                // Update friend request status to accepted
                FirestoreContext.getFirestore()
                        .collection("friendRequests")
                        .document(requestId)
                        .update("status", "accepted")
                        .get();

                // Create friendship record
                java.util.Map<String, Object> friendship = new java.util.HashMap<>();
                friendship.put("user1", fromUserId);
                friendship.put("user2", userId);
                friendship.put("createdAt", com.google.cloud.Timestamp.now());

                FirestoreContext.getFirestore()
                        .collection("friendships")
                        .document()
                        .set(friendship)
                        .get();

                Platform.runLater(() -> {
                    showAlert("Friend Added", "You are now friends with " + fromUsername + "!");
                    loadRequests();
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showAlert("Error", "Failed to accept request: " + e.getMessage()));
            }
        }).start();
    }

    private void handleDeclineRequest(String requestId, HBox card) {
        new Thread(() -> {
            try {
                // Update friend request status to declined
                FirestoreContext.getFirestore()
                        .collection("friendRequests")
                        .document(requestId)
                        .update("status", "declined")
                        .get();

                Platform.runLater(() -> {
                    requestsContainer.getChildren().remove(card);
                    showAlert("Request Declined", "Friend request declined.");
                    loadRequests();
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showAlert("Error", "Failed to decline request: " + e.getMessage()));
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
    public void handleViewFriends() {
        try {
            if (mainApp != null && userId != null) {
                mainApp.switchToFriends(userId);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Cannot navigate to friends.");
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
