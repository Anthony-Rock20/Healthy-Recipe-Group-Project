package com.example.peakplatesapp;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.Alert;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.geometry.Insets;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.ArrayList;
import java.util.List;

public class DashboardController {
    @FXML
    private Label welcomeLabel;
    @FXML
    private VBox recipesContainer;
    
    private MainApp mainApp;
    private String userId;
    private String username;

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void loadUserData() {
        if (userId == null) {
            System.err.println("User ID not set");
            return;
        }
        
        try {
            DocumentSnapshot doc = FirestoreContext.getFirestore()
                    .collection("users")
                    .document(userId)
                    .get()
                    .get();
            
            if (doc.exists()) {
                username = doc.getString("username");
                if (welcomeLabel != null && username != null) {
                    welcomeLabel.setText("Welcome, " + username + "!");
                }
                System.out.println("User data loaded successfully");
                
                // Load recipes after user data is loaded
                loadRecipes();
            } else {
                System.err.println("User document not found");
            }
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Error loading user data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadRecipes() {
        try {
            QuerySnapshot recipesSnapshot = FirestoreContext.getFirestore()
                    .collection("recipes")
                    .get()
                    .get();

            recipesContainer.getChildren().clear();

            if (recipesSnapshot.isEmpty()) {
                Label emptyLabel = new Label("No recipes yet. Upload one to get started!");
                emptyLabel.setStyle("-fx-font-size: 16; -fx-text-fill: #999;");
                recipesContainer.getChildren().add(emptyLabel);
            } else {
                for (QueryDocumentSnapshot doc : recipesSnapshot.getDocuments()) {
                    Recipe recipe = doc.toObject(Recipe.class);
                    recipe.setId(doc.getId());
                    recipesContainer.getChildren().add(createRecipeCard(recipe));
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Error loading recipes: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private VBox createRecipeCard(Recipe recipe) {
        VBox card = new VBox(10);
        card.setStyle("-fx-border-color: #ddd; -fx-border-width: 1; -fx-border-radius: 5; -fx-background-color: #fff;");
        card.setPadding(new Insets(15));

        // Header with username, timestamp, and edit/delete buttons
        HBox header = new HBox(10);
        Label userLabel = new Label("👤 " + recipe.getUsername());
        userLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");
        header.getChildren().add(userLabel);
        
        // Add spacer
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().add(spacer);
        
        // Add edit/delete buttons only if current user is the recipe creator
        if (userId != null && userId.equals(recipe.getUserId())) {
            Button editButton = new Button("✏️ Edit");
            editButton.setStyle("-fx-font-size: 10; -fx-padding: 5 10;");
            editButton.setOnAction(e -> handleEditRecipe(recipe));
            
            Button deleteButton = new Button("🗑️ Delete");
            deleteButton.setStyle("-fx-font-size: 10; -fx-padding: 5 10; -fx-background-color: #f44336; -fx-text-fill: white;");
            deleteButton.setOnAction(e -> handleDeleteRecipe(recipe));
            
            header.getChildren().addAll(editButton, deleteButton);
        }
        
        card.getChildren().add(header);

        // Image
        if (recipe.getImageData() != null && recipe.getImageData().length > 0) {
            try {
                Image img = new Image(new java.io.ByteArrayInputStream(recipe.getImageData()));
                ImageView imageView = new ImageView(img);
                imageView.setFitHeight(250);
                imageView.setFitWidth(400);
                imageView.setPreserveRatio(true);
                card.getChildren().add(imageView);
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

        Button likeButton = new Button("❤️ Like (" + recipe.getLikes() + ")");
        likeButton.setStyle("-fx-font-size: 11; -fx-padding: 5 10;");
        likeButton.setOnAction(e -> handleLike(recipe));

        Button favoriteButton = new Button("⭐ Favorite (" + recipe.getFavorites() + ")");
        favoriteButton.setStyle("-fx-font-size: 11; -fx-padding: 5 10;");
        favoriteButton.setOnAction(e -> handleFavorite(recipe));

        Button shareButton = new Button("📤 Share (" + recipe.getShares() + ")");
        shareButton.setStyle("-fx-font-size: 11; -fx-padding: 5 10;");
        shareButton.setOnAction(e -> handleShare(recipe));

        buttonBox.getChildren().addAll(likeButton, favoriteButton, shareButton);
        card.getChildren().add(buttonBox);

        return card;
    }

    private void handleLike(Recipe recipe) {
        try {
            List<String> likedByUsers = recipe.getLikedByUsers();
            if (likedByUsers == null) {
                likedByUsers = new ArrayList<>();
            }

            if (!likedByUsers.contains(userId)) {
                likedByUsers.add(userId);
                recipe.setLikes(recipe.getLikes() + 1);
                recipe.setLikedByUsers(likedByUsers);

                FirestoreContext.getFirestore()
                        .collection("recipes")
                        .document(recipe.getId())
                        .update("likes", recipe.getLikes(), "likedByUsers", likedByUsers)
                        .get();

                System.out.println("Recipe liked successfully");
                loadRecipes(); // Refresh recipes
            } else {
                showAlert("Already Liked", "You already liked this recipe.");
            }
        } catch (Exception e) {
            System.err.println("Error liking recipe: " + e.getMessage());
            showAlert("Error", "Failed to like recipe: " + e.getMessage());
        }
    }

    private void handleFavorite(Recipe recipe) {
        try {
            List<String> favoriteByUsers = recipe.getFavoriteByUsers();
            if (favoriteByUsers == null) {
                favoriteByUsers = new ArrayList<>();
            }

            if (!favoriteByUsers.contains(userId)) {
                favoriteByUsers.add(userId);
                recipe.setFavorites(recipe.getFavorites() + 1);
                recipe.setFavoriteByUsers(favoriteByUsers);

                FirestoreContext.getFirestore()
                        .collection("recipes")
                        .document(recipe.getId())
                        .update("favorites", recipe.getFavorites(), "favoriteByUsers", favoriteByUsers)
                        .get();

                System.out.println("Recipe favorited successfully");
                loadRecipes(); // Refresh recipes
            } else {
                showAlert("Already Favorited", "You already favorited this recipe.");
            }
        } catch (Exception e) {
            System.err.println("Error favoriting recipe: " + e.getMessage());
            showAlert("Error", "Failed to favorite recipe: " + e.getMessage());
        }
    }

    private void handleShare(Recipe recipe) {
        try {
            recipe.setShares(recipe.getShares() + 1);

            FirestoreContext.getFirestore()
                    .collection("recipes")
                    .document(recipe.getId())
                    .update("shares", recipe.getShares())
                    .get();

            showAlert("Shared", "Recipe shared successfully! (Share count: " + recipe.getShares() + ")");
            System.out.println("Recipe shared successfully");
            loadRecipes(); // Refresh recipes
        } catch (Exception e) {
            System.err.println("Error sharing recipe: " + e.getMessage());
            showAlert("Error", "Failed to share recipe: " + e.getMessage());
        }
    }

    @FXML
    public void handleUploadRecipe() {
        try {
            if (mainApp != null) {
                // Create a temporary upload controller to pass data
                mainApp.switchToUploadRecipe(userId, username);
            } else {
                System.err.println("mainApp is null");
            }
        } catch (IOException e) {
            System.err.println("Error navigating to upload: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void handleLogout() {
        try {
            if (mainApp != null) {
                mainApp.switchToView("Login.fxml");
            }
        } catch (IOException e) {
            System.err.println("Error logging out: " + e.getMessage());
        }
    }

    private void handleEditRecipe(Recipe recipe) {
        // For now, show a message. Full edit functionality can be added later
        showAlert("Edit Recipe", "Edit functionality coming soon!\nRecipe ID: " + recipe.getId());
    }

    private void handleDeleteRecipe(Recipe recipe) {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Delete Recipe");
        confirmDialog.setHeaderText("Are you sure?");
        confirmDialog.setContentText("This action cannot be undone.");
        
        java.util.Optional<javafx.scene.control.ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == javafx.scene.control.ButtonType.OK) {
            try {
                FirestoreContext.getFirestore()
                        .collection("recipes")
                        .document(recipe.getId())
                        .delete()
                        .get();
                
                showAlert("Success", "Recipe deleted successfully!");
                loadRecipes(); // Refresh recipes
            } catch (Exception e) {
                System.err.println("Error deleting recipe: " + e.getMessage());
                showAlert("Error", "Failed to delete recipe: " + e.getMessage());
            }
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
