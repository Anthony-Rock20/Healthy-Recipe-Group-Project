package com.example.peakplatesapp;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import java.nio.file.Files;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.UUID;

public class RecipeUploadController {

    @FXML private TextField titleField;
    @FXML private TextArea descriptionField;
    @FXML private Button chooseImageButton;
    @FXML private ImageView imagePreview;
    @FXML private Label selectedImageLabel;

    private File selectedImageFile;

    private MainApp mainApp;
    private String userId;
    private String username;

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    public void setUserId(String id) {
        this.userId = id;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    // --------------------------
    // CHOOSE IMAGE
    // --------------------------
    @FXML
    private void handleChooseImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Recipe Image");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));

        File file = chooser.showOpenDialog(chooseImageButton.getScene().getWindow());
        if (file != null) {
            selectedImageFile = file;
            chooseImageButton.setText("Image Selected ✔");
            try {
                if (selectedImageLabel != null) selectedImageLabel.setText(file.getName());
                if (imagePreview != null) {
                    Image img = new Image(new java.io.FileInputStream(file));
                    imagePreview.setImage(img);
                }
            } catch (Exception e) {
                System.err.println("Failed to load preview: " + e.getMessage());
            }
        }
    }

    // Bridge method for FXML which references handleSelectImage
    @FXML
    private void handleSelectImage() {
        handleChooseImage();
    }

    @FXML
    private void handleCancel() {
        try {
            if (mainApp != null) {
                mainApp.switchToDashboard(userId, username);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to cancel and return to dashboard.");
        }
    }

    @FXML
    private void handleViewFavorites() {
        try {
            if (mainApp != null && userId != null) {
                mainApp.switchToFavorites(userId);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to navigate to favorites.");
        }
    }

    @FXML
    private void handleLogout() {
        try {
            if (mainApp != null) {
                mainApp.switchToView("Login");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to logout.");
        }
    }

    @FXML
    private void handleViewSharedWithMe() {
        try {
            if (mainApp != null && userId != null) {
                mainApp.switchToSharedWithMe(userId);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to navigate to shared with me.");
        }
    }

    // --------------------------
    // UPLOAD RECIPE TO FIRESTORE & STORAGE
    // --------------------------
    @FXML
    private void handleUpload() {
        String title = titleField.getText() == null ? "" : titleField.getText().trim();
        String description = descriptionField.getText() == null ? "" : descriptionField.getText().trim();

        if (title.isEmpty() || description.isEmpty() || selectedImageFile == null) {
            showError("Please fill all fields and choose an image.");
            return;
        }

        Recipe recipe = new Recipe();
        recipe.setUserId(userId);
        recipe.setUsername(username);
        recipe.setTitle(title);
        recipe.setDescription(description);

        // Generate Firestore document ID
        Firestore db = FirestoreContext.getFirestore();
        DocumentReference ref = db.collection("recipes").document();
        recipe.setId(ref.getId());

        // Show a non-blocking progress alert
        Alert progressAlert = new Alert(Alert.AlertType.INFORMATION);
        progressAlert.setTitle("Uploading");
        progressAlert.setHeaderText(null);
        progressAlert.setContentText("Uploading your recipe...");
        try {
            if (mainApp != null && mainApp.getStage() != null) progressAlert.initOwner(mainApp.getStage());
        } catch (Exception ignored) {}
        progressAlert.show();

        // Upload image data and metadata in background thread
        new Thread(() -> {
            try {
                // Read image bytes and attach to recipe as Firestore Blob
                byte[] imageBytes = Files.readAllBytes(selectedImageFile.toPath());
                recipe.setImageData(com.google.cloud.firestore.Blob.fromBytes(imageBytes));

                // Save recipe metadata (including image bytes) in Firestore
                ref.set(recipe).get();

                Platform.runLater(() -> {
                    try { progressAlert.close(); } catch (Exception ignored) {}
                    showInfo("Recipe uploaded successfully!");
                    try {
                        mainApp.switchToDashboard(userId, username);
                    } catch (Exception e) {
                        e.printStackTrace();
                        showError("Upload succeeded but failed to navigate to dashboard.");
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    try { progressAlert.close(); } catch (Exception ignored) {}
                    showError("Failed to upload recipe: " + e.getMessage());
                });
            }
        }, "recipe-upload-thread").start();
    }

    // --------------------------
    // HELPER ALERT METHODS
    // --------------------------
    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg);
        a.showAndWait();
    }

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg);
        a.showAndWait();
    }
}
