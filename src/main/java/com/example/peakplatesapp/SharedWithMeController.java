package com.example.peakplatesapp;

import javafx.fxml.FXML;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.QueryDocumentSnapshot;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SharedWithMeController {

    @FXML private VBox sharedContainer;
    @FXML private Label sharedCountLabel;

    private MainApp mainApp;
    private String userId;

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void loadSharedRecipes() {
        if (userId == null) {
            System.err.println("User ID not set");
            return;
        }

        sharedContainer.getChildren().clear();
        Label loading = new Label("Loading shared recipes...");
        sharedContainer.getChildren().add(loading);

        new Thread(() -> {
            try {
                // Get all recipes shared with this user
                QuerySnapshot shareSnapshot = FirestoreContext.getFirestore()
                        .collection("sharedRecipes")
                        .whereEqualTo("sharedWith", userId)
                        .get()
                        .get();

                List<String> recipeIds = new ArrayList<>();
                List<String> sharedByUsers = new ArrayList<>();

                for (QueryDocumentSnapshot doc : shareSnapshot.getDocuments()) {
                    String recipeId = doc.getString("recipeId");
                    String sharedByUsername = doc.getString("sharedByUsername");
                    if (recipeId != null) {
                        recipeIds.add(recipeId);
                        sharedByUsers.add(sharedByUsername != null ? sharedByUsername : "Unknown");
                    }
                }

                Platform.runLater(() -> {
                    sharedContainer.getChildren().clear();
                    if (recipeIds.isEmpty()) {
                        sharedContainer.getChildren().add(new Label("No recipes shared with you yet!"));
                        sharedCountLabel.setText("0 recipes");
                    } else {
                        for (int i = 0; i < recipeIds.size(); i++) {
                            loadAndDisplayRecipe(recipeIds.get(i), sharedByUsers.get(i));
                        }
                        sharedCountLabel.setText(recipeIds.size() + " recipe" + (recipeIds.size() != 1 ? "s" : ""));
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    sharedContainer.getChildren().clear();
                    sharedContainer.getChildren().add(new Label("Error loading shared recipes: " + e.getMessage()));
                });
            }
        }).start();
    }

    private void loadAndDisplayRecipe(String recipeId, String sharedByUsername) {
        new Thread(() -> {
            try {
                Recipe recipe = FirestoreContext.getFirestore()
                        .collection("recipes")
                        .document(recipeId)
                        .get()
                        .get()
                        .toObject(Recipe.class);

                if (recipe != null) {
                    recipe.setId(recipeId);
                    Platform.runLater(() -> {
                        sharedContainer.getChildren().add(createSharedRecipeCard(recipe, sharedByUsername));
                    });
                }
            } catch (Exception e) {
                System.err.println("Error loading recipe: " + e.getMessage());
            }
        }).start();
    }

    private VBox createSharedRecipeCard(Recipe recipe, String sharedByUsername) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(10));
        card.setStyle("-fx-background-color: #ffffff; -fx-border-color: #cccccc; -fx-border-radius: 8; -fx-background-radius: 8;");

        // Header with recipe title and shared by info
        Label titleLabel = new Label(recipe.getTitle() != null ? recipe.getTitle() : "Untitled Recipe");
        titleLabel.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");

        Label sharedByLabel = new Label("📨 Shared by: " + sharedByUsername);
        sharedByLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #666;");

        card.getChildren().addAll(titleLabel, sharedByLabel);

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

        // Description preview
        Label descriptionLabel = new Label(recipe.getDescription());
        descriptionLabel.setWrapText(true);
        descriptionLabel.setStyle("-fx-font-size: 12;");
        card.getChildren().add(descriptionLabel);

        // View button
        javafx.scene.control.Button viewButton = new javafx.scene.control.Button("📖 View");
        viewButton.setStyle("-fx-font-size: 12; -fx-padding: 8 15;");
        viewButton.setOnAction(e -> showRecipeDetails(recipe));
        card.getChildren().add(viewButton);

        return card;
    }

    private void showRecipeDetails(Recipe recipe) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(recipe.getTitle());
        alert.setHeaderText("By: " + recipe.getUsername());
        alert.setContentText(recipe.getDescription());
        alert.getDialogPane().setPrefWidth(400);
        alert.showAndWait();
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
    public void handleUploadRecipe() {
        try {
            if (mainApp != null && userId != null) {
                mainApp.switchToUploadRecipe(userId, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Cannot navigate to upload.");
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
