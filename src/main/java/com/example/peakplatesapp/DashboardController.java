package com.example.peakplatesapp;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.DocumentSnapshot;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

public class DashboardController {

    @FXML private Label welcomeLabel;
    @FXML private VBox recipesContainer;

    private MainApp mainApp;
    private String userId;
    private String username;

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void loadUserData() {
        if (userId == null) {
            System.err.println("User ID not set!");
            return;
        }

        try {
            DocumentSnapshot doc = FirestoreContext.getFirestore()
                    .collection("users")
                    .document(userId)
                    .get()
                    .get();

            if (doc.exists()) {
                if (username == null) {
                    username = doc.getString("username");
                }
                if (username != null) {
                    welcomeLabel.setText("Welcome, " + username + "!");
                }
            }

            loadRecipes();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --------------------------
    // LOAD RECIPES FROM FIRESTORE
    // --------------------------
    private void loadRecipes() {
        recipesContainer.getChildren().clear();
        Label loading = new Label("Loading recipes...");
        recipesContainer.getChildren().add(loading);

        new Thread(() -> {
            try {
                QuerySnapshot snapshots = FirestoreContext.getFirestore()
                        .collection("recipes")
                        .get()
                        .get();

                List<Recipe> recipes = new ArrayList<>();
                for (QueryDocumentSnapshot doc : snapshots.getDocuments()) {
                    Recipe recipe = doc.toObject(Recipe.class);
                    recipe.setId(doc.getId());
                    recipes.add(recipe);
                }

                Platform.runLater(() -> {
                    recipesContainer.getChildren().clear();
                    if (recipes.isEmpty()) {
                        recipesContainer.getChildren().add(new Label("No recipes uploaded yet."));
                    } else {
                        for (Recipe r : recipes) {
                            recipesContainer.getChildren().add(createRecipeCard(r));
                        }
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    recipesContainer.getChildren().clear();
                    recipesContainer.getChildren().add(new Label("Error loading recipes."));
                });
            }
        }).start();
    }

    // --------------------------
    // CREATE RECIPE CARD
    // --------------------------
    private VBox createRecipeCard(Recipe recipe) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(10));
        card.setStyle("-fx-background-color: #ffffff; -fx-border-color: #cccccc; -fx-border-radius: 8; -fx-background-radius: 8;");

        // Title
        Label titleLabel = new Label(recipe.getTitle() != null ? recipe.getTitle() : "Untitled Recipe");
        titleLabel.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");
        card.getChildren().add(titleLabel);

        // Image
        if (recipe.getImageData() != null) {
            try {
                byte[] bytes = recipe.getImageData().toBytes();
                if (bytes.length > 0) {
                    Image img = new Image(new ByteArrayInputStream(bytes));
                    ImageView imageView = new ImageView(img);
                    imageView.setFitWidth(350);
                    imageView.setPreserveRatio(true);
                    imageView.setStyle("-fx-cursor: hand;");
                    imageView.setOnMouseClicked(e -> showRecipeDetails(recipe));
                    card.getChildren().add(imageView);
                }
            } catch (Exception e) {
                System.err.println("Image decode failed: " + e.getMessage());
            }
        }

        // Actions: View / Like / Favorite / Share
        HBox actions = new HBox(10);

        Button viewButton = new Button("📖 View");
        viewButton.setOnAction(e -> showRecipeDetails(recipe));

        Button likeButton = new Button("❤️ " + recipe.getLikes());
        likeButton.setOnAction(e -> handleLike(recipe));

        Button favButton = new Button("⭐ " + recipe.getFavorites());
        favButton.setOnAction(e -> handleFavorite(recipe));

        Button shareButton = new Button("📤 " + recipe.getShares());
        shareButton.setOnAction(e -> handleShare(recipe));

        actions.getChildren().addAll(viewButton, likeButton, favButton, shareButton);
        card.getChildren().add(actions);

        return card;
    }

    // --------------------------
    // LIKE RECIPE
    // --------------------------
    private void handleLike(Recipe recipe) {
        try {
            if (!recipe.getLikedByUsers().contains(userId)) {
                recipe.getLikedByUsers().add(userId);
                recipe.setLikes(recipe.getLikes() + 1);

                FirestoreContext.getFirestore()
                        .collection("recipes")
                        .document(recipe.getId())
                        .update("likes", recipe.getLikes(),
                                "likedByUsers", recipe.getLikedByUsers())
                        .get();

                loadRecipes();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --------------------------
    // FAVORITE RECIPE
    // --------------------------
    private void handleFavorite(Recipe recipe) {
        try {
            if (!recipe.getFavoriteByUsers().contains(userId)) {
                recipe.getFavoriteByUsers().add(userId);
                recipe.setFavorites(recipe.getFavorites() + 1);

                FirestoreContext.getFirestore()
                        .collection("recipes")
                        .document(recipe.getId())
                        .update("favorites", recipe.getFavorites(),
                                "favoriteByUsers", recipe.getFavoriteByUsers())
                        .get();

                loadRecipes();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --------------------------
    // SHARE RECIPE
    // --------------------------
    private void handleShare(Recipe recipe) {
        try {
            if (mainApp != null && userId != null) {
                mainApp.switchToShareRecipe(userId, recipe);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Navigation error", "Cannot open share page.");
        }
    }

    // --------------------------
    // VIEW RECIPE DETAILS
    // --------------------------
    private void showRecipeDetails(Recipe recipe) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(recipe.getTitle());
        alert.setHeaderText(recipe.getUsername());
        alert.setContentText(recipe.getDescription());
        alert.getDialogPane().setPrefWidth(400);
        alert.showAndWait();
    }

    // --------------------------
    // NAVIGATION BUTTONS
    // --------------------------
    @FXML
    public void handleUploadRecipe() {
        try {
            if (mainApp == null) {
                System.err.println("handleUploadRecipe: mainApp is null");
                showAlert("Navigation error", "Internal error: application controller missing.");
                return;
            }
            if (userId == null || userId.isEmpty()) {
                System.err.println("handleUploadRecipe: userId is null or empty (username=" + username + ")");
                showAlert("Navigation error", "Please log in before uploading a recipe.");
                return;
            }

            System.out.println("handleUploadRecipe: navigating to UploadRecipe for userId=" + userId + " username=" + username);
            mainApp.switchToUploadRecipe(userId, username);
        } catch (Exception e) {
            System.err.println("handleUploadRecipe: navigation failed: " + e.getMessage());
            e.printStackTrace();
            showAlert("Navigation error", "Cannot open Upload Recipe: " + e.getMessage());
        }
    }

    @FXML
    public void handleLogout() {
        if (mainApp != null) {
            try {
                mainApp.switchToView("Login");
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Navigation error", "Cannot logout.");
            }
        }
    }

    @FXML
    public void handleViewFavorites() {
        if (mainApp != null && userId != null) {
            try {
                mainApp.switchToFavorites(userId);
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Navigation error", "Cannot open Favorites.");
            }
        }
    }

    @FXML
    public void handleViewSharedWithMe() {
        if (mainApp != null && userId != null) {
            try {
                mainApp.switchToSharedWithMe(userId);
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Navigation error", "Cannot open Shared with Me.");
            }
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
