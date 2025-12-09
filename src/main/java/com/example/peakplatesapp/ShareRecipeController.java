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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ShareRecipeController {

    @FXML private VBox usersContainer;
    @FXML private Label recipeNameLabel;

    private MainApp mainApp;
    private String userId;
    private String recipeId;
    private String recipeName;
    private Recipe recipe;

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setRecipeId(String recipeId) {
        this.recipeId = recipeId;
    }

    public void setRecipe(Recipe recipe) {
        this.recipe = recipe;
        this.recipeName = recipe != null ? recipe.getTitle() : "Unknown";
        if (recipeNameLabel != null) {
            recipeNameLabel.setText("Recipe: " + recipeName);
        }
        loadUsers();
    }

    private void loadUsers() {
        usersContainer.getChildren().clear();
        Label loading = new Label("Loading users...");
        usersContainer.getChildren().add(loading);

        new Thread(() -> {
            try {
                QuerySnapshot usersSnapshot = FirestoreContext.getFirestore()
                        .collection("users")
                        .get()
                        .get();

                List<String> userIds = new ArrayList<>();
                List<String> usernames = new ArrayList<>();

                for (QueryDocumentSnapshot doc : usersSnapshot.getDocuments()) {
                    String uid = doc.getId();
                    String username = doc.getString("username");
                    
                    // Skip current user
                    if (!uid.equals(userId)) {
                        userIds.add(uid);
                        usernames.add(username != null ? username : uid);
                    }
                }

                Platform.runLater(() -> {
                    usersContainer.getChildren().clear();
                    if (userIds.isEmpty()) {
                        usersContainer.getChildren().add(new Label("No other users available."));
                    } else {
                        for (int i = 0; i < userIds.size(); i++) {
                            String uid = userIds.get(i);
                            String username = usernames.get(i);
                            usersContainer.getChildren().add(createUserShareCard(uid, username));
                        }
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

    private HBox createUserShareCard(String userId, String username) {
        HBox card = new HBox(10);
        card.setPadding(new Insets(10));
        card.setStyle("-fx-border-color: #ddd; -fx-border-width: 1; -fx-background-color: #f9f9f9;");

        Label userLabel = new Label("👤 " + username);
        userLabel.setStyle("-fx-font-size: 14;");

        Button shareButton = new Button("📤 Share");
        shareButton.setStyle("-fx-font-size: 12; -fx-padding: 8 15; -fx-background-color: #2196F3; -fx-text-fill: white;");
        shareButton.setOnAction(e -> handleShareWithUser(userId, username));

        card.getChildren().addAll(userLabel, shareButton);
        return card;
    }

    private void handleShareWithUser(String targetUserId, String targetUsername) {
        new Thread(() -> {
            try {
                if (recipe == null) {
                    Platform.runLater(() -> showAlert("Error", "Recipe not loaded."));
                    return;
                }

                // Add user to sharedWith list
                List<String> sharedWith = recipe.getSharedWith();
                if (!sharedWith.contains(targetUserId)) {
                    sharedWith.add(targetUserId);
                    recipe.setSharedWith(sharedWith);

                    // Update recipe in Firestore
                    FirestoreContext.getFirestore()
                            .collection("recipes")
                            .document(recipeId)
                            .update("sharedWith", sharedWith)
                            .get();

                    // Store share record with timestamp for "Shared with Me" tracking
                    String shareId = FirestoreContext.getFirestore().collection("sharedRecipes").document().getId();
                    java.util.Map<String, Object> shareData = new java.util.HashMap<>();
                    shareData.put("recipeId", recipeId);
                    shareData.put("recipeName", recipeName);
                    shareData.put("sharedBy", userId);
                    shareData.put("sharedByUsername", recipe.getUsername());
                    shareData.put("sharedWith", targetUserId);
                    shareData.put("sharedAt", com.google.cloud.Timestamp.now());

                    FirestoreContext.getFirestore()
                            .collection("sharedRecipes")
                            .document(shareId)
                            .set(shareData)
                            .get();

                    Platform.runLater(() -> {
                        showAlert("Success", "Recipe shared with " + targetUsername + "!");
                        loadUsers();
                    });
                } else {
                    Platform.runLater(() -> showAlert("Info", "Recipe already shared with " + targetUsername + "."));
                }
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showAlert("Error", "Failed to share: " + e.getMessage()));
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
