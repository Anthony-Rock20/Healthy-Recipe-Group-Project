package com.example.peakplatesapp;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.FlowPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.geometry.Insets;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

public class FavoritesController {
    @FXML
    private VBox favoritesContainer;
    @FXML
    private Label favoriteCountLabel;
    @FXML
    private TextField searchField;
    @FXML
    private FlowPane filterTagsContainer;
    
    private MainApp mainApp;
    private String userId;
    private List<Recipe> allFavorites = new ArrayList<>();
    private Set<String> selectedTags = new HashSet<>();

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

    @FXML
    public void handleViewFriends() {
        try {
            if (mainApp != null && userId != null) {
                mainApp.switchToFriends(userId);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Navigation error", "Cannot navigate to friends.");
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

            allFavorites.clear();
            Set<String> allTags = new HashSet<>();
            int favoriteCount = 0;

            for (QueryDocumentSnapshot doc : recipesSnapshot.getDocuments()) {
                Recipe recipe = doc.toObject(Recipe.class);
                recipe.setId(doc.getId());

                // Check if current user has favorited this recipe
                if (recipe.getFavoriteByUsers() != null && recipe.getFavoriteByUsers().contains(userId)) {
                    allFavorites.add(recipe);
                    favoriteCount++;
                    if (recipe.getTags() != null) {
                        allTags.addAll(recipe.getTags());
                    }
                }
            }

            int finalFavCount = favoriteCount;
            Set<String> finalTags = allTags;
            javafx.application.Platform.runLater(() -> {
                populateFilterTags(new ArrayList<>(finalTags));
                displayFavorites(allFavorites);
                favoriteCountLabel.setText(finalFavCount + " favorite" + (finalFavCount != 1 ? "s" : ""));
            });
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Error loading favorites: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void populateFilterTags(List<String> tags) {
        filterTagsContainer.getChildren().clear();
        for (String tag : tags) {
            CheckBox tagCheckBox = new CheckBox(tag);
            tagCheckBox.setStyle("-fx-font-size: 12;");
            tagCheckBox.setOnAction(e -> {
                if (tagCheckBox.isSelected()) {
                    selectedTags.add(tag);
                } else {
                    selectedTags.remove(tag);
                }
                applyFilters();
            });
            filterTagsContainer.getChildren().add(tagCheckBox);
        }
    }

    @FXML
    private void handleSearch() {
        applyFilters();
    }

    @FXML
    private void handleClearSearch() {
        searchField.clear();
        selectedTags.clear();
        for (javafx.scene.Node node : filterTagsContainer.getChildren()) {
            if (node instanceof CheckBox) {
                ((CheckBox) node).setSelected(false);
            }
        }
        displayFavorites(allFavorites);
    }

    private void applyFilters() {
        String searchQuery = searchField.getText() != null ? searchField.getText().trim().toLowerCase() : "";
        List<Recipe> filtered = new ArrayList<>();

        for (Recipe recipe : allFavorites) {
            // Check name match
            boolean nameMatches = recipe.getTitle() != null && recipe.getTitle().toLowerCase().contains(searchQuery);
            
            // Check tag matches
            boolean tagsMatch = true;
            if (!selectedTags.isEmpty()) {
                tagsMatch = recipe.getTags() != null && recipe.getTags().stream().anyMatch(selectedTags::contains);
            }

            // Include if name matches (when searching) or if tags match (when filtering)
            if ((searchQuery.isEmpty() || nameMatches) && tagsMatch) {
                filtered.add(recipe);
            }
        }

        displayFavorites(filtered);
    }

    private void displayFavorites(List<Recipe> recipes) {
        favoritesContainer.getChildren().clear();
        if (recipes.isEmpty()) {
            favoritesContainer.getChildren().add(new Label("No recipes found."));
        } else {
            for (Recipe recipe : recipes) {
                favoritesContainer.getChildren().add(createFavoriteRecipeCard(recipe));
            }
        }
    }
    private VBox createFavoriteRecipeCard(Recipe recipe) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(10));
        card.setStyle("-fx-background-color: #ffffff; -fx-border-color: #cccccc; -fx-border-radius: 8; -fx-background-radius: 8;");

        // Title
        Label titleLabel = new Label(recipe.getTitle() != null ? recipe.getTitle() : "Untitled Recipe");
        titleLabel.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");
        card.getChildren().add(titleLabel);

        // By: username
        Label byLabelCard = new Label("By: " + (recipe.getUsername() != null ? recipe.getUsername() : "Unknown"));
        byLabelCard.setStyle("-fx-font-size: 12; -fx-text-fill: #666;");
        card.getChildren().add(byLabelCard);

        // Tags (display under title)
        if (recipe.getTags() != null && !recipe.getTags().isEmpty()) {
            javafx.scene.layout.FlowPane tagsFlow = new javafx.scene.layout.FlowPane(8, 6);
            for (String t : recipe.getTags()) {
                Label tagLabel = new Label(t);
                tagLabel.setStyle("-fx-background-color: #e0f2f1; -fx-padding: 4 8; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 11;");
                tagsFlow.getChildren().add(tagLabel);
            }
            card.getChildren().add(tagsFlow);
        }

        // Image
        Image img = null;
        if (recipe.getImageData() != null) {
            try {
                byte[] bytes = recipe.getImageData().toBytes();
                if (bytes.length > 0) {
                    img = createImageFromBytes(bytes);
                }
            } catch (Exception e) {
                System.err.println("Image decode failed: " + e.getMessage());
            }
        }

        if (img != null) {
            ImageView imageView = new ImageView(img);
            imageView.setFitWidth(350);
            imageView.setPreserveRatio(true);
            imageView.setStyle("-fx-cursor: hand;");
            imageView.setOnMouseClicked(e -> showRecipeDetails(recipe));
            card.getChildren().add(imageView);
        }

        // Actions: View / Like / Favorite / Share
        HBox actions = new HBox(10);

        javafx.scene.control.Button viewButton = new javafx.scene.control.Button("📖 View");
        viewButton.setOnAction(e -> showRecipeDetails(recipe));

        javafx.scene.control.Button likeButton = new javafx.scene.control.Button("❤️ Like (" + recipe.getLikes() + ")");
        likeButton.setOnAction(e -> handleLikeToggle(recipe));

        javafx.scene.control.Button favButton = new javafx.scene.control.Button("⭐ Favorite (" + recipe.getFavorites() + ")");
        favButton.setOnAction(e -> handleFavoriteToggle(recipe));

        javafx.scene.control.Button shareButton = new javafx.scene.control.Button("📤 Share (" + recipe.getShares() + ")");
        shareButton.setOnAction(e -> handleShare(recipe));

        actions.getChildren().addAll(viewButton, likeButton, favButton, shareButton);
        card.getChildren().add(actions);

        return card;
    }

    private void handleShare(Recipe recipe) {
        try {
            if (mainApp != null && userId != null) {
                mainApp.switchToShareRecipe(userId, recipe);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Navigation error", "Cannot open Share page.");
        }
    }

    private void handleLikeToggle(Recipe recipe) {
        try {
            java.util.List<String> liked = recipe.getLikedByUsers();
            if (liked == null) liked = new java.util.ArrayList<>();
            if (!liked.contains(userId)) {
                liked.add(userId);
                recipe.setLikes(recipe.getLikes() + 1);
            } else {
                liked.remove(userId);
                recipe.setLikes(Math.max(0, recipe.getLikes() - 1));
            }

            recipe.setLikedByUsers(liked);
            FirestoreContext.getFirestore()
                    .collection("recipes")
                    .document(recipe.getId())
                    .update("likes", recipe.getLikes(), "likedByUsers", liked)
                    .get();

            loadFavorites();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to update like: " + e.getMessage());
        }
    }

    private void handleFavoriteToggle(Recipe recipe) {
        try {
            java.util.List<String> favs = recipe.getFavoriteByUsers();
            if (favs == null) favs = new java.util.ArrayList<>();
            if (!favs.contains(userId)) {
                favs.add(userId);
                recipe.setFavorites(recipe.getFavorites() + 1);
            } else {
                favs.remove(userId);
                recipe.setFavorites(Math.max(0, recipe.getFavorites() - 1));
            }

            recipe.setFavoriteByUsers(favs);
            FirestoreContext.getFirestore()
                    .collection("recipes")
                    .document(recipe.getId())
                    .update("favorites", recipe.getFavorites(), "favoriteByUsers", favs)
                    .get();

            loadFavorites();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to update favorite: " + e.getMessage());
        }
    }

    private void showRecipeDetails(Recipe recipe) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle(recipe.getTitle());
        alert.setHeaderText(null);

        javafx.scene.layout.VBox content = new javafx.scene.layout.VBox(10);
        content.setPrefWidth(420);

        Label titleLabel = new Label(recipe.getTitle() != null ? recipe.getTitle() : "Untitled Recipe");
        titleLabel.setStyle("-fx-font-size: 20; -fx-font-weight: bold;");
        content.getChildren().add(titleLabel);

        Image img = null;
        if (recipe.getImageData() != null) {
            try {
                byte[] bytes = recipe.getImageData().toBytes();
                if (bytes.length > 0) {
                    img = createImageFromBytes(bytes);
                }
            } catch (Exception e) {
                System.err.println("Image decode failed: " + e.getMessage());
            }
        }

        if (img != null) {
            ImageView imageView = new ImageView(img);
            imageView.setFitWidth(380);
            imageView.setPreserveRatio(true);
            content.getChildren().add(imageView);
        }

        javafx.scene.control.Label byLabel = new javafx.scene.control.Label("By: " + (recipe.getUsername() != null ? recipe.getUsername() : "Unknown"));
        byLabel.setStyle("-fx-font-weight: bold; -fx-padding: 4 0 0 0;");
        content.getChildren().add(byLabel);

        // Tags
        if (recipe.getTags() != null && !recipe.getTags().isEmpty()) {
            javafx.scene.layout.FlowPane tagsFlow = new javafx.scene.layout.FlowPane(8, 6);
            for (String t : recipe.getTags()) {
                Label tagLabel = new Label(t);
                tagLabel.setStyle("-fx-background-color: #e0f2f1; -fx-padding: 4 8; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 11;");
                tagsFlow.getChildren().add(tagLabel);
            }
            content.getChildren().add(tagsFlow);
        }

        // Ingredients
        if (recipe.getIngredients() != null && !recipe.getIngredients().isEmpty()) {
            Label ingHeader = new Label("Ingredients:");
            ingHeader.setStyle("-fx-font-weight: bold; -fx-padding: 6 0 0 0;");
            javafx.scene.control.Label ing = new javafx.scene.control.Label(recipe.getIngredients());
            ing.setWrapText(true);
            content.getChildren().addAll(ingHeader, ing);
        }

        // Steps
        if (recipe.getSteps() != null && !recipe.getSteps().isEmpty()) {
            Label stepsHeader = new Label("Steps:");
            stepsHeader.setStyle("-fx-font-weight: bold; -fx-padding: 6 0 0 0;");
            javafx.scene.control.Label st = new javafx.scene.control.Label(recipe.getSteps());
            st.setWrapText(true);
            content.getChildren().addAll(stepsHeader, st);
        }

        alert.getDialogPane().setContent(content);
        alert.getDialogPane().setPrefWidth(420);
        alert.showAndWait();
    }

    private Image createImageFromBytes(byte[] bytes) {
        try {
            java.io.InputStream bis = new java.io.BufferedInputStream(new java.io.ByteArrayInputStream(bytes));
            Image img = new Image(bis);
            if (img.isError() || img.getWidth() <= 0) {
                try {
                    java.nio.file.Path tmp = java.nio.file.Files.createTempFile("peakplates-img-", ".jpg");
                    java.nio.file.Files.write(tmp, bytes);
                    tmp.toFile().deleteOnExit();
                    Image img2 = new Image(tmp.toUri().toString());
                    if (!img2.isError() && img2.getWidth() > 0) return img2;
                } catch (Exception ex) {
                    System.err.println("Favorites fallback temp-file image load failed: " + ex.getMessage());
                }
                return null;
            }
            return img;
        } catch (Exception e) {
            System.err.println("Favorites createImageFromBytes error: " + e.getMessage());
            return null;
        }
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
