package com.example.peakplatesapp;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;

    public void handleLogin(ActionEvent event) {
        String email = emailField.getText();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showAlert("Please enter both email and password.");
            return;
        }

        // Mock login validation
        if (email.equals("test@domain.com") && password.equals("1234")) {
            showAlert("Login successful!");
        } else {
            showAlert("Invalid email or password.");
        }
    }

    public void openSignup(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/signup.fxml"));
            Scene scene = new Scene(loader.load());
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String message) {
        new Alert(Alert.AlertType.INFORMATION, message).showAndWait();
    }
}
