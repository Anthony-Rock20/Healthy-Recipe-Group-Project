package com.example.peakplatesapp;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.CheckBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.FlowPane;
import javafx.geometry.Insets;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.DocumentSnapshot;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

public class DashboardController {

    @FXML private Label welcomeLabel;
    @FXML private VBox recipesContainer;
    @FXML private TextField searchField;
    @FXML private FlowPane filterTagsContainer;

    private MainApp mainApp;
    private String userId;
    private String username;
    private List<Recipe> allRecipes = new ArrayList<>();
    private Set<String> selectedTags = new HashSet<>();

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

    // LOAD RECIPES FROM FIRESTORE
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

                allRecipes.clear();
                Set<String> allTags = new HashSet<>();
                for (QueryDocumentSnapshot doc : snapshots.getDocuments()) {
                    Recipe recipe = doc.toObject(Recipe.class);
                    recipe.setId(doc.getId());
                    allRecipes.add(recipe);
                    if (recipe.getTags() != null) {
                        allTags.addAll(recipe.getTags());
                    }
                }

                Platform.runLater(() -> {
                    // Populate filter tags
                    populateFilterTags(new ArrayList<>(allTags));
                    // Display all recipes
                    displayRecipes(allRecipes);
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
        displayRecipes(allRecipes);
    }

    private void applyFilters() {
        String searchQuery = searchField.getText() != null ? searchField.getText().trim().toLowerCase() : "";
        List<Recipe> filtered = new ArrayList<>();

        for (Recipe recipe : allRecipes) {
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

        displayRecipes(filtered);
    }

    private void displayRecipes(List<Recipe> recipes) {
        recipesContainer.getChildren().clear();
        if (recipes.isEmpty()) {
            recipesContainer.getChildren().add(new Label("No recipes found."));
        } else {
            for (Recipe r : recipes) {
                recipesContainer.getChildren().add(createRecipeCard(r));
            }
        }
    }


    // CREATES RECIPE CARD
    private VBox createRecipeCard(Recipe recipe) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(10));
        card.setStyle("-fx-background-color: #ffffff; -fx-border-color: #cccccc; -fx-border-radius: 8; -fx-background-radius: 8;");

        // Title
        Label titleLabel = new Label(recipe.getTitle() != null ? recipe.getTitle() : "Untitled Recipe");
        titleLabel.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");
        card.getChildren().add(titleLabel);

        // By: username
        Label byLabel = new Label("By: " + (recipe.getUsername() != null ? recipe.getUsername() : "Unknown"));
        byLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #666;");
        card.getChildren().add(byLabel);

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

        // Actions: View / Like / Favorite / Share (labeled)
        HBox actions = new HBox(10);

        Button viewButton = new Button("📖 View");
        viewButton.setOnAction(e -> showRecipeDetails(recipe));

        Button likeButton = new Button("❤️ Like (" + recipe.getLikes() + ")");
        likeButton.setOnAction(e -> handleLike(recipe));

        Button favButton = new Button("⭐ Favorite (" + recipe.getFavorites() + ")");
        favButton.setOnAction(e -> handleFavorite(recipe));

        Button shareButton = new Button("📤 Share (" + recipe.getShares() + ")");
        shareButton.setOnAction(e -> handleShare(recipe));

        actions.getChildren().addAll(viewButton, likeButton, favButton, shareButton);
        card.getChildren().add(actions);

        return card;
    }

    // LIKE RECIPE
    private void handleLike(Recipe recipe) {
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
                    .update("likes", recipe.getLikes(),
                            "likedByUsers", liked)
                    .get();

            loadRecipes();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // FAVORITE RECIPE
    private void handleFavorite(Recipe recipe) {
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
                    .update("favorites", recipe.getFavorites(),
                            "favoriteByUsers", favs)
                    .get();

            loadRecipes();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // SHARE RECIPE
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

    // VIEW RECIPE DETAILS
    private void showRecipeDetails(Recipe recipe) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(recipe.getTitle());
        alert.setHeaderText(null);

        // Build content: title first, then image, then username, then description
        javafx.scene.layout.VBox content = new javafx.scene.layout.VBox(10);
        content.setPrefWidth(420);

        // Add recipe title at the top of content
        Label titleLabel = new Label(recipe.getTitle() != null ? recipe.getTitle() : "Untitled Recipe");
        titleLabel.setStyle("-fx-font-size: 20; -fx-font-weight: bold;");
        content.getChildren().add(titleLabel);

        Image img = null;
        if (recipe.getImageData() != null) {
            try {
                byte[] bytes = recipe.getImageData().toBytes();
                if (bytes != null && bytes.length > 0) {
                    img = createImageFromBytes(bytes);
                }
            } catch (Exception e) {
                System.err.println("Image decode failed for recipe " + recipe.getId() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        if (img != null) {
            ImageView imageView = new ImageView(img);
            imageView.setFitWidth(380);
            imageView.setPreserveRatio(true);
            content.getChildren().add(imageView);
        }

        Label byLabel = new Label("By: " + (recipe.getUsername() != null ? recipe.getUsername() : "Unknown"));
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

    // NAVIGATION BUTTONS
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

    @FXML
    public void handleViewFriends() {
        if (mainApp != null && userId != null) {
            try {
                mainApp.switchToFriends(userId);
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Navigation error", "Cannot open Friends.");
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

    // Helper: try to create a JavaFX Image from bytes; if direct decoding fails, write a temp file and load via URI
    private Image createImageFromBytes(byte[] bytes) {
        try {
            java.io.InputStream bis = new java.io.BufferedInputStream(new ByteArrayInputStream(bytes));
            Image img = new Image(bis);
            // If image failed to load (width=0 and error), fallback to temp file
            if (img.isError() || img.getWidth() <= 0) {
                try {
                    java.nio.file.Path tmp = java.nio.file.Files.createTempFile("peakplates-img-", ".jpg");
                    java.nio.file.Files.write(tmp, bytes);
                    tmp.toFile().deleteOnExit();
                    Image img2 = new Image(tmp.toUri().toString());
                    if (!img2.isError() && img2.getWidth() > 0) return img2;
                } catch (Exception ex) {
                    System.err.println("Fallback temp-file image load failed: " + ex.getMessage());
                }
                return null;
            }
            return img;
        } catch (Exception e) {
            System.err.println("createImageFromBytes error: " + e.getMessage());
            return null;
        }
    }
}
