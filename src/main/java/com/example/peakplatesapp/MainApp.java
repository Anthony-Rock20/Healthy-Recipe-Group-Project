package com.example.peakplatesapp;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.Alert;
import java.io.IOException;

public class MainApp extends Application {
    private static Scene scene;
    private Stage stage;

    @Override
    public void start(Stage stage) throws IOException {
        System.out.println("Starting Peak Plates application...");
        this.stage = stage;

        try {
            System.out.println("Initializing Firebase services...");

            FirestoreContext.getFirestore();
            System.out.println("Firebase services initialized successfully");


            FXMLLoader loader = new FXMLLoader(getClass().getResource("Login.fxml"));
            Parent root = loader.load();
            scene = new Scene(root);


            try {
                scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());
            } catch (NullPointerException e) {
                System.out.println("No CSS file found, continuing without styles...");
            }


            LoginController controller = loader.getController();
            if (controller != null) {

                controller.setMainApp(this);
            }

            stage.setTitle("Peak Plates");
            stage.setScene(scene);
            stage.setResizable(false);
            stage.show();

        } catch (Exception e) {
            System.err.println("Failed to start application: " + e.getMessage());
            e.printStackTrace();


            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Startup Error");
            alert.setHeaderText("Failed to start application");
            alert.setContentText("Error: " + e.getMessage());
            alert.showAndWait();

            System.exit(1);
        }
    }


    public void switchToView(String fxmlFile) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
        Parent root = loader.load();
        scene.setRoot(root);

        Object controller = loader.getController();
        if (controller != null && controller instanceof LoginController) {
            ((LoginController) controller).setMainApp(this);
        } else if (controller != null && controller instanceof SignupController) {
            ((SignupController) controller).setMainApp(this);
        } else if (controller != null && controller instanceof DashboardController) {
            ((DashboardController) controller).setMainApp(this);
        }

        scene.setRoot(root);
        stage.sizeToScene();

        stage.sizeToScene();
    }
    public void switchToDashboard(String userId) throws IOException {
        System.out.println("Switching to dashboard for user: " + userId);


        FXMLLoader loader = new FXMLLoader(getClass().getResource("Dashboard.fxml"));
        Parent root = loader.load();


        DashboardController dashboardController = loader.getController();

        if (dashboardController != null) {

            dashboardController.setMainApp(this);


            dashboardController.setUserId(userId);


            dashboardController.loadUserData();
        }


        scene.setRoot(root);


        stage.setTitle("Peak Plates - Dashboard");
    }


    public void showSettingsView() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("AccessData.fxml"));
        Parent root = loader.load();


        AccessDataController controller = loader.getController();
        if (controller != null) {

        }

        scene.setRoot(root);
    }


    public static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MainApp.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }


    public Stage getStage() {
        return stage;
    }


    public static void main(String[] args) {
        launch();
    }
}