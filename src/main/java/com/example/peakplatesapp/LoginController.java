package com.example.peakplatesapp;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.Firestore;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;

import java.io.IOException;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;

    private MainApp mainApp;

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    @FXML
    public void handleLogin(ActionEvent event) {
        String username = emailField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert("Please enter both username and password.");
            return;
        }

        // SAME AS YOURS — do not change this
        String loginEmail = username.contains("@") ? username : username + "@example.com";

        System.out.println("Attempting login with email: " + loginEmail);

        tryFirestoreLogin(loginEmail, password);
    }

    private void tryFirestoreLogin(String email, String password) {
        try {
            System.out.println("=== DEBUG START ===");
            System.out.println("Looking for email: " + email);

            Firestore db = FirestoreContext.getFirestore(); // SAME

            // SAME FIRESTORE DEBUG INFO
            System.out.println("Querying ALL users to see what's in database...");
            QuerySnapshot allUsers = db.collection("users").get().get();
            System.out.println("Total users in database: " + allUsers.size());

            for (QueryDocumentSnapshot doc : allUsers) {
                System.out.println("--- Document ID: " + doc.getId() + " ---");
                System.out.println("Data: " + doc.getData());
                if (doc.contains("email")) {
                    System.out.println("Email in DB: " + doc.getString("email"));
                }
                if (doc.contains("password")) {
                    String storedPass = doc.getString("password");
                    System.out.println("Password in DB: " + (storedPass != null ? "[EXISTS]" : "null"));
                }
                if (doc.contains("uid")) {
                    System.out.println("UID in DB: " + doc.getString("uid"));
                }
            }

            // SAME QUERY
            System.out.println("\nNow querying for email: " + email);
            QuerySnapshot snap = db.collection("users")
                    .whereEqualTo("email", email)
                    .get()
                    .get();

            System.out.println("Query found " + snap.size() + " documents");

            boolean authenticated = false;
            String uid = null;

            // SAME PASSWORD CHECKING
            for (QueryDocumentSnapshot doc : snap.getDocuments()) {
                System.out.println("Checking document: " + doc.getId());
                String storedPassword = doc.getString("password");
                System.out.println("Stored password: " + storedPassword);
                System.out.println("Input password: " + password);

                if (storedPassword != null && storedPassword.equals(password)) {
                    authenticated = true;
                    uid = doc.getString("uid");
                    System.out.println("✅ Password match! UID: " + uid);
                    break;
                } else if (storedPassword == null) {
                    System.out.println("❌ No password field in document");
                } else {
                    System.out.println("❌ Password mismatch");
                }
            }

            // SAME AUTH LOGIC
            if (authenticated && uid != null) {
                System.out.println("Authentication successful! Navigating...");

                try {
                    FirebaseAuth auth = FirestoreContext.getAuth();
                    UserRecord userRecord = auth.getUser(uid);
                    System.out.println("✅ Firebase Auth verification: " + userRecord.getEmail());

                    showAlert("Login successful!");

                    if (mainApp != null) {
                        mainApp.switchToDashboard(uid);
                    } else {
                        System.err.println("❌ mainApp is null!");
                        showAlert("Application error: Cannot navigate.");
                    }

                } catch (FirebaseAuthException e) {
                    System.err.println("❌ Firebase Auth verification failed: " + e.getMessage());
                    showAlert("Login error: User not found in authentication system.");
                }

            } else {
                System.out.println("❌ Authentication failed");
                showAlert("Invalid username or password.");
            }

        } catch (Exception e) {
            System.err.println("❌ Firestore login error: " + e.getMessage());
            e.printStackTrace();
            showAlert("Login failed: " + e.getMessage());
        }
    }

    @FXML
    public void openSignup(ActionEvent event) {
        try {
            if (mainApp != null) {
                mainApp.switchToView("Signup");
                return;
            }

            // Fallback navigation
            Stage stage = (Stage) emailField.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Signup"));
            Scene scene = new Scene(loader.load());
            stage.setScene(scene);

        } catch (IOException e) {
            showAlert("Error opening signup page: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
