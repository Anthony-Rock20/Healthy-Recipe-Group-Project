package com.example.peakplatesapp;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.WriteResult;
import com.google.cloud.firestore.SetOptions;
import com.google.api.core.ApiFuture;
import java.util.HashMap;
import java.util.Map;

public class AccessDataController {

    @FXML private TextField nameField;
    @FXML private TextField ageField;
    @FXML private TextField heightField;
    @FXML private TextField weightField;
    @FXML private TextField genderField;

    private Firestore db;

    public void initialize() {
        db = FirestoreContext.getFirestore();
    }

    @FXML
    private void handleWrite() {
        try {
            String name = nameField.getText();
            String gender = genderField.getText();
            int age = Integer.parseInt(ageField.getText());
            int height = Integer.parseInt(heightField.getText());
            int weight = Integer.parseInt(weightField.getText());

            User user = new User(name, age, height, weight, gender);
            Map<String, Object> data = new HashMap<>();
            data.put("name", user.getName());
            data.put("gender", user.getGender());
            data.put("age", user.getAge());
            data.put("height", user.getHeight());
            data.put("weight", user.getWeight());

            DocumentReference docRef = db.collection("users").document();
            docRef.set(data);

            showAlert(AlertType.INFORMATION, "Success", "User data saved successfully!");
            clearFields();
        } catch (NumberFormatException e) {
            showAlert(AlertType.ERROR, "Error", "Please enter valid numbers for age, height, and weight");
        } catch (Exception e) {
            showAlert(AlertType.ERROR, "Error", "Failed to save data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showAlert(AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void clearFields() {
        nameField.clear();
        ageField.clear();
        heightField.clear();
        weightField.clear();
        genderField.clear();
    }
}