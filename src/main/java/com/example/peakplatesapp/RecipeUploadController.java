package com.example.peakplatesapp;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.scene.layout.StackPane;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.DocumentReference;
import com.google.api.core.ApiFuture;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class RecipeUploadController {
    @FXML
    private ImageView imagePreview;
    @FXML
    private Label imageLabel;
    @FXML
    private Label selectedImageLabel;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private Label charCountLabel;
    @FXML
    private StackPane imagePreviewPane;

    private MainApp mainApp;
    private String userId;
    private String username;
    private File selectedImageFile;
    private static final int MAX_DESCRIPTION_LENGTH = 2000;

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @FXML
    public void initialize() {
        // Add character count listener
        descriptionArea.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.length() > MAX_DESCRIPTION_LENGTH) {
                descriptionArea.setText(oldVal);
            } else {
                charCountLabel.setText(newVal.length() + " / " + MAX_DESCRIPTION_LENGTH + " characters");
            }
        });
    }

    @FXML
    public void handleSelectImage() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select Recipe Image");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Image Files", "*.jpg", "*.jpeg", "*.png", "*.gif"),
                    new FileChooser.ExtensionFilter("All Files", "*.*")
            );

            // Get the current stage from the button or any node in the scene
            Stage stage = null;
            if (imagePreviewPane != null && imagePreviewPane.getScene() != null) {
                Window window = imagePreviewPane.getScene().getWindow();
                if (window instanceof Stage) {
                    stage = (Stage) window;
                }
            }

            if (stage != null) {
                File selectedFile = fileChooser.showOpenDialog(stage);
                if (selectedFile != null) {
                    selectedImageFile = selectedFile;
                    displayImage(selectedFile);
                    selectedImageLabel.setText("Selected: " + selectedFile.getName());
                }
            } else {
                System.err.println("Stage not found");
                showAlert(Alert.AlertType.ERROR, "Error", "Could not open file dialog");
            }
        } catch (Exception e) {
            System.err.println("Error selecting image: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to select image: " + e.getMessage());
        }
    }

    private void displayImage(File imageFile) {
        try {
            Image image = new Image(new FileInputStream(imageFile));
            imagePreview.setImage(image);
            imageLabel.setText("");
            imageLabel.setVisible(false);
        } catch (IOException e) {
            System.err.println("Error loading image: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load image: " + e.getMessage());
        }
    }

    @FXML
    public void handleUpload() {
        String description = descriptionArea.getText().trim();

        // Validation
        if (selectedImageFile == null) {
            showAlert(Alert.AlertType.WARNING, "Missing Image", "Please select an image for your recipe.");
            return;
        }

        if (description.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Missing Description", "Please add a description for your recipe.");
            return;
        }

        // Upload recipe
        uploadRecipe(description);
    }

    private void uploadRecipe(String description) {
        try {
            showAlert(Alert.AlertType.INFORMATION, "Uploading", "Uploading your recipe...");

            // Read image file
            byte[] imageBytes = Files.readAllBytes(selectedImageFile.toPath());

            // Create recipe object
            Recipe recipe = new Recipe(userId, username, description, selectedImageFile.getName());
            recipe.setImageData(imageBytes);
            recipe.setId(UUID.randomUUID().toString());

            // Upload to Firestore
            Firestore db = FirestoreContext.getFirestore();
            DocumentReference docRef = db.collection("recipes").document(recipe.getId());

            ApiFuture<?> writeResult = docRef.set(recipe);

            // Wait for completion
            writeResult.get();

            System.out.println("Recipe uploaded successfully with ID: " + recipe.getId());
            showAlert(Alert.AlertType.INFORMATION, "Success", "Recipe uploaded successfully!");

            // Clear form and go back to dashboard
            clearForm();
            if (mainApp != null) {
                mainApp.switchToView("Dashboard.fxml");
            }

        } catch (IOException e) {
            System.err.println("Error reading image file: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to read image file: " + e.getMessage());
        } catch (ExecutionException e) {
            System.err.println("Error uploading recipe: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Upload Error", "Failed to upload recipe: " + e.getCause().getMessage());
        } catch (InterruptedException e) {
            System.err.println("Upload interrupted: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Upload was interrupted: " + e.getMessage());
        }
    }

    private void clearForm() {
        selectedImageFile = null;
        imagePreview.setImage(null);
        imageLabel.setText("Click button below to select an image");
        imageLabel.setVisible(true);
        selectedImageLabel.setText("No image selected");
        descriptionArea.clear();
        charCountLabel.setText("0 / " + MAX_DESCRIPTION_LENGTH + " characters");
    }

    @FXML
    public void handleCancel() {
        if (mainApp != null) {
            try {
                mainApp.switchToView("Dashboard.fxml");
            } catch (IOException e) {
                System.err.println("Error navigating back to dashboard: " + e.getMessage());
            }
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
