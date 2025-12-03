package com.example.peakplatesapp;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.*;
import javafx.scene.*;
import javafx.application.Platform;
import java.io.IOException;

// Firebase imports
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
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

    private MainApp mainApp; // Add reference to MainApp

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    @FXML
    public void handleSignup(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert("Please enter both username and password.");
            return;
        }


        String email = username.contains("@") ? username : username + "@example.com";

        try {
            System.out.println("Attempting to register user: " + email);


            FirebaseAuth auth = FirestoreContext.getAuth();
            if (auth == null) {
                throw new RuntimeException("FirebaseAuth initialization failed");
            }

            Firestore db = FirestoreContext.getFirestore();
            if (db == null) {
                throw new RuntimeException("Firestore initialization failed");
            }


            System.out.println("Creating user in Firebase Auth...");
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
                        System.out.println("User created in Firebase Auth with ID: " + userRecord.getUid());


                        System.out.println("Storing user data in Firestore...");
                        Map<String, Object> data = new HashMap<>();
                        data.put("username", username);
                        data.put("email", email);
                        data.put("password",password);
                        data.put("displayName", username);
                        data.put("uid", userRecord.getUid());
                        data.put("createdAt", com.google.cloud.Timestamp.now());


                        DocumentReference docRef = db.collection("users").document(userRecord.getUid());
                        docRef.set(data);
                        System.out.println("User data stored in Firestore successfully");

                        showAlert("Registration Successful!\nUser ID: " + userRecord.getUid() +
                                "\nYou can now login with your credentials.");

                        // Clear fields
                        usernameField.clear();
                        passwordField.clear();



                    } catch (InterruptedException | ExecutionException e) {
                        System.err.println("Error in user creation: " + e.getMessage());
                        showAlert("Registration Error: " + e.getCause().getMessage());
                    } catch (Exception e) {
                        System.err.println("Unexpected error: " + e.getMessage());
                        showAlert("Registration Error: " + e.getMessage());
                    }
                });
            }, Runnable::run);

        } catch (Exception e) {
            System.err.println("Registration error: " + e.getMessage());
            e.printStackTrace();
            showAlert("Registration Error: " + e.getMessage());
        }
    }

    @FXML
    public void goBack(ActionEvent event) {
        try {
            if (mainApp != null) {
                mainApp.switchToView("Login.fxml");
            } else {

                FXMLLoader loader = new FXMLLoader(getClass().getResource("Login.fxml"));
                Scene scene = new Scene(loader.load());
                Stage stage = (Stage) usernameField.getScene().getWindow();
                stage.setScene(scene);
            }
        } catch (IOException e) {
            System.err.println("Error navigating back: " + e.getMessage());
            showAlert("Error: Cannot navigate to login page.");
        }
    }


    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(message);
        alert.setHeaderText(null);
        alert.showAndWait();
    }


    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}