package com.example.peakplatesapp;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.geometry.Insets;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class FavoritesController {
    @FXML
    private VBox favoritesContainer;
    @FXML
    private Label favoriteCountLabel;
    
    private MainApp mainApp;
    private String userId;

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @FXML
    public void handleUploadRecipe() {
        try {
            if (mainApp != null && userId != null) {
                mainApp.switchToUploadRecipe(userId, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Navigation error", "Cannot open Upload Recipe.");
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
            showAlert("Navigation error", "Cannot logout.");
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
            showAlert("Navigation error", "Cannot navigate to shared with me.");
        }
    }

    public void loadFavorites() {
        if (userId == null) {
            System.err.println("User ID not set");
            return;
        }

        try {
            QuerySnapshot recipesSnapshot = FirestoreContext.getFirestore()
                    .collection("recipes")
                    .get()
                    .get();

            favoritesContainer.getChildren().clear();
            int favoriteCount = 0;

            for (QueryDocumentSnapshot doc : recipesSnapshot.getDocuments()) {
                Recipe recipe = doc.toObject(Recipe.class);
                recipe.setId(doc.getId());

                // Check if current user has favorited this recipe
                if (recipe.getFavoriteByUsers() != null && recipe.getFavoriteByUsers().contains(userId)) {
                    favoritesContainer.getChildren().add(createFavoriteRecipeCard(recipe));
                    favoriteCount++;
                }
            }

            if (favoriteCount == 0) {
                Label emptyLabel = new Label("You haven't favorited any recipes yet!");
                emptyLabel.setStyle("-fx-font-size: 16; -fx-text-fill: #999;");
                favoritesContainer.getChildren().add(emptyLabel);
            }

            favoriteCountLabel.setText(favoriteCount + " favorite" + (favoriteCount != 1 ? "s" : ""));
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Error loading favorites: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private VBox createFavoriteRecipeCard(Recipe recipe) {
        VBox card = new VBox(10);
        card.setStyle("-fx-border-color: #ddd; -fx-border-width: 1; -fx-border-radius: 5; -fx-background-color: #fff;");
        card.setPadding(new Insets(15));

        // Header with username
        HBox header = new HBox(10);
        Label userLabel = new Label("👤 " + recipe.getUsername());
        userLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");
        header.getChildren().add(userLabel);
        card.getChildren().add(header);

        // Image
        if (recipe.getImageData() != null) {
            try {
                byte[] bytes = recipe.getImageData().toBytes();
                if (bytes.length > 0) {
                    Image img = new Image(new java.io.ByteArrayInputStream(bytes));
                    ImageView imageView = new ImageView(img);
                    imageView.setFitHeight(250);
                    imageView.setFitWidth(400);
                    imageView.setPreserveRatio(true);
                    card.getChildren().add(imageView);
                }
            } catch (Exception e) {
                System.err.println("Error loading recipe image: " + e.getMessage());
            }
        }

        // Description
        Label descriptionLabel = new Label(recipe.getDescription());
        descriptionLabel.setWrapText(true);
        descriptionLabel.setStyle("-fx-font-size: 12; -fx-text-alignment: LEFT;");
        card.getChildren().add(descriptionLabel);

        // Action buttons
        HBox buttonBox = new HBox(10);
        buttonBox.setStyle("-fx-padding: 10; -fx-spacing: 5;");

        javafx.scene.control.Button likeButton = new javafx.scene.control.Button("❤️ Like (" + recipe.getLikes() + ")");
        likeButton.setStyle("-fx-font-size: 11; -fx-padding: 5 10;");

        javafx.scene.control.Button removeFavoriteButton = new javafx.scene.control.Button("⭐ Remove from Favorites");
        removeFavoriteButton.setStyle("-fx-font-size: 11; -fx-padding: 5 10; -fx-background-color: #FF9800; -fx-text-fill: white;");
        removeFavoriteButton.setOnAction(e -> handleRemoveFavorite(recipe));

        javafx.scene.control.Button shareButton = new javafx.scene.control.Button("📤 Share (" + recipe.getShares() + ")");
        shareButton.setStyle("-fx-font-size: 11; -fx-padding: 5 10;");

        buttonBox.getChildren().addAll(likeButton, removeFavoriteButton, shareButton);
        card.getChildren().add(buttonBox);

        return card;
    }

    private void handleRemoveFavorite(Recipe recipe) {
        try {
            java.util.List<String> favoriteByUsers = recipe.getFavoriteByUsers();
            if (favoriteByUsers != null && favoriteByUsers.contains(userId)) {
                favoriteByUsers.remove(userId);
                recipe.setFavorites(Math.max(0, recipe.getFavorites() - 1));

                FirestoreContext.getFirestore()
                        .collection("recipes")
                        .document(recipe.getId())
                        .update("favorites", recipe.getFavorites(), "favoriteByUsers", favoriteByUsers)
                        .get();

                System.out.println("Recipe removed from favorites");
                loadFavorites(); // Refresh favorites
            }
        } catch (Exception e) {
            System.err.println("Error removing from favorites: " + e.getMessage());
            showAlert("Error", "Failed to remove from favorites: " + e.getMessage());
        }
    }

    @FXML
    public void handleBackToDashboard() {
        try {
            if (mainApp != null) {
                // Use the dashboard navigation that preserves user context
                mainApp.switchToDashboard(userId);
            }
        } catch (IOException e) {
            System.err.println("Error navigating back to dashboard: " + e.getMessage());
        }
    }

    private void showAlert(String title, String content) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
