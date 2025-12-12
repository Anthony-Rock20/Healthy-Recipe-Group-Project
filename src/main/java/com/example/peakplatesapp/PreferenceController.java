package com.example.peakplatesapp;


import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class PreferenceController {

    @FXML private TextField caloriesField;
    @FXML private TextField proteinField;
    @FXML private TextField carbsField;
    @FXML private TextField fatsField;
    @FXML private Button saveButton;

    private MainApp mainApp;
    private String userId;

    public void setMainApp(MainApp mainApp) { this.mainApp = mainApp; }
    public void setUserId(String userId) { this.userId = userId; }

    @FXML
    private void initialize() {
        // optionally add input validation here
    }

    public void loadCurrentGoals(Map<String, Object> goalsMap) {
        if (goalsMap == null) return;
        if (goalsMap.get("calories") != null) caloriesField.setText(String.valueOf(((Number)goalsMap.get("calories")).intValue()));
        if (goalsMap.get("protein") != null) proteinField.setText(String.valueOf(((Number)goalsMap.get("protein")).intValue()));
        if (goalsMap.get("carbs") != null) carbsField.setText(String.valueOf(((Number)goalsMap.get("carbs")).intValue()));
        if (goalsMap.get("fats") != null) fatsField.setText(String.valueOf(((Number)goalsMap.get("fats")).intValue()));
    }

    @FXML
    private void handleSave() {
        try {
            int calories = Integer.parseInt(caloriesField.getText().trim());
            int protein = Integer.parseInt(proteinField.getText().trim());
            int carbs = Integer.parseInt(carbsField.getText().trim());
            int fats = Integer.parseInt(fatsField.getText().trim());

            Map<String, Object> goals = new HashMap<>();
            goals.put("calories", calories);
            goals.put("protein", protein);
            goals.put("carbs", carbs);
            goals.put("fats", fats);

            // Update only the goals sub-map
            FirestoreContext.getFirestore()
                    .collection("users")
                    .document(userId)
                    .update("goals", goals)
                    .get();

            // After saving, go back to home and force reload so Home picks up new goals
            if (mainApp != null) mainApp.switchToHome(userId);

        } catch (NumberFormatException nfe) {
            // show validation alert
            System.err.println("Enter valid integers for goals");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    private void handleBack() throws IOException {
        if (mainApp != null && userId != null) {
            try {
                mainApp.switchToHome(userId);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            System.err.println("Error: mainApp or userId not set in PreferenceController.");
        }
    }

}