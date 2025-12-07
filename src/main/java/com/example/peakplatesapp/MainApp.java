package com.example.peakplatesapp;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import javafx.scene.control.Alert;

public class MainApp extends Application {

    private static Scene scene;
    private Stage stage;

    @Override
    public void start(Stage stage) throws IOException {
        this.stage = stage;

        // Load Login screen at startup and wire controller
        FXMLLoader loader = new FXMLLoader(
                MainApp.class.getResource("/com/example/peakplatesapp/Login.fxml")
        );
        Parent root = loader.load();
        // If controller supports setMainApp, provide reference
        Object controller = loader.getController();
        if (controller != null) {
            if (controller instanceof LoginController) {
                ((LoginController) controller).setMainApp(this);
            } else if (controller instanceof SignupController) {
                ((SignupController) controller).setMainApp(this);
            }
        }

        scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("Peak Plates - Login");
        stage.show();
        
            // If credentials look missing, show a non-blocking informational alert
            try {
                String credsCheck = FirestoreContext.credentialsHealthCheck();
                if (credsCheck != null) {
                    Alert info = new Alert(Alert.AlertType.WARNING);
                    info.setTitle("Firebase Configuration");
                    info.setHeaderText("Firebase credentials issue detected");
                    info.setContentText(credsCheck + "\nPlace your service account JSON at src/main/resources/com/example/peakplatesapp/key.json or follow README_INTELLIJ.md");
                    info.show();
                }
            } catch (Exception ex) {
                System.err.println("Error checking Firebase credentials: " + ex.getMessage());
            }
    }

    // ---------------------------
    // FXML Loader Helper
    // ---------------------------
    private Parent loadFXML(String fxml) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                MainApp.class.getResource("/com/example/peakplatesapp/" + fxml + ".fxml")
        );
        return loader.load();
    }

    // ---------------------------
    // Scene Switching
    // ---------------------------

    public void switchToView(String fxml) throws IOException {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/com/example/peakplatesapp/" + fxml + ".fxml"));
        Parent root = loader.load();

        Object controller = loader.getController();
        if (controller != null) {
            if (controller instanceof LoginController) {
                ((LoginController) controller).setMainApp(this);
            } else if (controller instanceof SignupController) {
                ((SignupController) controller).setMainApp(this);
            } else if (controller instanceof DashboardController) {
                ((DashboardController) controller).setMainApp(this);
            } else if (controller instanceof RecipeUploadController) {
                ((RecipeUploadController) controller).setMainApp(this);
            } else if (controller instanceof FavoritesController) {
                ((FavoritesController) controller).setMainApp(this);
            }
        }

        scene.setRoot(root);
    }

    // ---------------------------
    // Login → Dashboard navigation
    // ---------------------------
    public void switchToDashboard(String userId, String username) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                MainApp.class.getResource("/com/example/peakplatesapp/Dashboard.fxml")
        );
        Parent root = loader.load();

        DashboardController controller = loader.getController();
        controller.setMainApp(this);
        controller.setUserId(userId);
        controller.loadUserData();

        scene.setRoot(root);
    }

    // ---------------------------
    // Dashboard → Upload Recipe
    // ---------------------------
    public void switchToUploadRecipe(String userId, String username) throws IOException {

        FXMLLoader loader = new FXMLLoader(
                MainApp.class.getResource("/com/example/peakplatesapp/UploadRecipe.fxml")
        );

        Parent root = loader.load();

        RecipeUploadController controller = loader.getController();
        controller.setMainApp(this);
        controller.setUserId(userId);
        controller.setUsername(username);

        scene.setRoot(root);
    }

    /**
     * Backwards-compatible switchToDashboard overload used by older callers.
     */
    public void switchToDashboard(String userId) throws IOException {
        switchToDashboard(userId, null);
    }

    // ---------------------------
    // Dashboard → Favorites
    // ---------------------------
    public void switchToFavorites(String userId) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                MainApp.class.getResource("/com/example/peakplatesapp/Favorites.fxml")
        );
        Parent root = loader.load();

        FavoritesController controller = loader.getController();
        controller.setMainApp(this);
        controller.setUserId(userId);
        controller.loadFavorites();

        scene.setRoot(root);
    }

    // ---------------------------
    // Share Recipe Page
    // ---------------------------
    public void switchToShareRecipe(String userId, Recipe recipe) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                MainApp.class.getResource("/com/example/peakplatesapp/ShareRecipe.fxml")
        );
        Parent root = loader.load();

        ShareRecipeController controller = loader.getController();
        controller.setMainApp(this);
        controller.setUserId(userId);
        controller.setRecipeId(recipe.getId());
        controller.setRecipe(recipe);

        scene.setRoot(root);
    }

    // ---------------------------
    // Shared with Me Page
    // ---------------------------
    public void switchToSharedWithMe(String userId) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                MainApp.class.getResource("/com/example/peakplatesapp/SharedWithMe.fxml")
        );
        Parent root = loader.load();

        SharedWithMeController controller = loader.getController();
        controller.setMainApp(this);
        controller.setUserId(userId);
        controller.loadSharedRecipes();

        scene.setRoot(root);
    }

    public Stage getStage() {
        return stage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
