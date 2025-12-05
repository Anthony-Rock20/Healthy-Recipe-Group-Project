package com.example.peakplatesapp;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.*;
import javafx.scene.*;
import javafx.application.Platform;
import java.io.IOException;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.auth.UserRecord.CreateRequest;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.DocumentReference;
import com.google.api.core.ApiFuture;
import java.util.concurrent.ExecutionException;
import java.util.HashMap;
import java.util.Map;

import javafx.fxml.FXMLLoader;

public class SignupController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;

    private MainApp mainApp;

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    @FXML
    public void handleSignup(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String confirm = confirmPasswordField.getText();

        if (username.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            showAlert("Please fill in all fields.");
            return;
        }

        if (!password.equals(confirm)) {
            showAlert("Passwords do not match.");
            return;
        }

        String email = username.contains("@") ? username : username + "@example.com";

        try {
            FirebaseAuth auth = FirestoreContext.getAuth();
            Firestore db = FirestoreContext.getFirestore();

            if (auth == null || db == null) {
                throw new RuntimeException("Firebase initialization failed");
            }

            CreateRequest request = new CreateRequest()
                    .setEmail(email)
                    .setEmailVerified(false)
                    .setPassword(password)
                    .setDisplayName(username);

            ApiFuture<UserRecord> userRecordFuture = auth.createUserAsync(request);

            userRecordFuture.addListener(() -> {
                Platform.runLater(() -> {
                    try {
                        UserRecord userRecord = userRecordFuture.get();

                        Map<String, Object> data = new HashMap<>();
                        data.put("username", username);
                        data.put("email", email);
                        data.put("password", password);
                        data.put("displayName", username);
                        data.put("uid", userRecord.getUid());
                        data.put("createdAt", com.google.cloud.Timestamp.now());

                        DocumentReference docRef =
                                db.collection("users").document(userRecord.getUid());
                        docRef.set(data);

                        showAlert("Registration Successful!\nYou can now login.");

                        usernameField.clear();
                        passwordField.clear();
                        confirmPasswordField.clear();

                    } catch (InterruptedException | ExecutionException e) {
                        showAlert("Registration Error: " + e.getMessage());
                    } catch (Exception e) {
                        showAlert("Registration Error: " + e.getMessage());
                    }
                });
            }, Runnable::run);

        } catch (Exception e) {
            showAlert("Registration Error: " + e.getMessage());
        }
    }

    @FXML
    public void goBack(ActionEvent event) {
        try {
            if (mainApp != null) {
                mainApp.switchToView("Login.fxml");
                return;
            }

            Stage stage = (Stage) usernameField.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Login.fxml"));
            Scene scene = new Scene(loader.load());
            stage.setScene(scene);

        } catch (IOException e) {
            showAlert("Error: Cannot return to login page.");
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
