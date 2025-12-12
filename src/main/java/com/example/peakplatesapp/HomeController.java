package com.example.peakplatesapp;

import com.google.cloud.firestore.DocumentSnapshot;
import javafx.application.Platform;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import javafx.scene.Scene;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class HomeController {

    @FXML private Label welcomeLabel;

    @FXML private ProgressBar calorieBar;
    @FXML private Label calorieLabel;

    @FXML private ProgressBar proteinBar;
    @FXML private Label proteinLabel;

    @FXML private ProgressBar carbBar;
    @FXML private Label carbLabel;

    @FXML private ProgressBar fatBar;
    @FXML private Label fatLabel;

    @FXML private DatePicker datePicker;

    private MainApp mainApp;
    private String userId;
    private String username;

    // Cache of daily logs by date
    private Map<String, Map<String, Object>> dailyLogsCache = new HashMap<>();

    // Properties for goals and consumed values
    private final IntegerProperty goalCalories = new SimpleIntegerProperty(0);
    private final IntegerProperty consumedCalories = new SimpleIntegerProperty(0);

    private final IntegerProperty goalProtein = new SimpleIntegerProperty(0);
    private final IntegerProperty consumedProtein = new SimpleIntegerProperty(0);

    private final IntegerProperty goalCarbs = new SimpleIntegerProperty(0);
    private final IntegerProperty consumedCarbs = new SimpleIntegerProperty(0);

    private final IntegerProperty goalFats = new SimpleIntegerProperty(0);
    private final IntegerProperty consumedFats = new SimpleIntegerProperty(0);

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void initialize() {
        datePicker.setValue(LocalDate.now()); // default today
        datePicker.valueProperty().addListener((obs, oldDate, newDate) -> {
            if (newDate != null) {
                updateConsumedValuesForDate(newDate.toString());
            }
        });

        // Bind progress bars once
        bindProgressBar(calorieBar, calorieLabel, consumedCalories, goalCalories, "kcal");
        bindProgressBar(proteinBar, proteinLabel, consumedProtein, goalProtein, "g");
        bindProgressBar(carbBar, carbLabel, consumedCarbs, goalCarbs, "g");
        bindProgressBar(fatBar, fatLabel, consumedFats, goalFats, "g");
    }

    public void loadUserData() {
        if (userId == null) {
            System.err.println("HomeController: userId not set");
            return;
        }

        new Thread(() -> {
            try {
                DocumentSnapshot doc = FirestoreContext.getFirestore()
                        .collection("users")
                        .document(userId)
                        .get()
                        .get();

                if (!doc.exists()) {
                    Platform.runLater(() -> welcomeLabel.setText("Welcome!"));
                    return;
                }

                // Username
                if (username == null) username = doc.getString("username");
                final String finalUsername = username;

                // Goals
                Map<String, Object> goalsMap = (Map<String, Object>) doc.get("goals");
                int gCal = goalsMap != null && goalsMap.get("calories") != null ? ((Number) goalsMap.get("calories")).intValue() : 0;
                int gProt = goalsMap != null && goalsMap.get("protein") != null ? ((Number) goalsMap.get("protein")).intValue() : 0;
                int gCarb = goalsMap != null && goalsMap.get("carbs") != null ? ((Number) goalsMap.get("carbs")).intValue() : 0;
                int gFat = goalsMap != null && goalsMap.get("fats") != null ? ((Number) goalsMap.get("fats")).intValue() : 0;

                // Daily logs
                Map<String, Object> dailyLogs = (Map<String, Object>) doc.get("daily");
                if (dailyLogs != null) {
                    for (Map.Entry<String, Object> entry : dailyLogs.entrySet()) {
                        String date = entry.getKey();
                        Map<String, Object> values = (Map<String, Object>) entry.getValue();
                        dailyLogsCache.put(date, values);
                    }
                }

                Platform.runLater(() -> {
                    if (finalUsername != null) welcomeLabel.setText("Welcome, " + finalUsername + "!");
                    goalCalories.set(gCal);
                    goalProtein.set(gProt);
                    goalCarbs.set(gCarb);
                    goalFats.set(gFat);

                    // Update progress bars for selected date (today default)
                    String selectedDate = datePicker.getValue() != null ? datePicker.getValue().toString() : LocalDate.now().toString();
                    updateConsumedValuesForDate(selectedDate);
                });

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    private void bindProgressBar(ProgressBar bar, Label label,
                                 IntegerProperty consumedProp, IntegerProperty goalProp,
                                 String unit) {
        DoubleBinding pct = new DoubleBinding() {
            { super.bind(consumedProp, goalProp); }
            @Override
            protected double computeValue() {
                int goal = goalProp.get();
                int consumed = consumedProp.get();
                if (goal <= 0) return 0.0;
                double ratio = (double) consumed / goal;
                return Math.max(0.0, Math.min(ratio, 1.0));
            }
        };

        bar.progressProperty().bind(pct);

        StringBinding sb = new StringBinding() {
            { super.bind(consumedProp, goalProp, pct); }
            @Override
            protected String computeValue() {
                int percent = (int) Math.round(pct.get() * 100.0);
                return String.format("%d / %d %s (%d%%)", consumedProp.get(), goalProp.get(), unit, percent);
            }
        };
        label.textProperty().bind(sb);
    }

    @FXML
    private void handlePreferences() {
        if (mainApp != null && userId != null) {
            try { mainApp.switchToPreferences(userId); }
            catch (Exception e) { e.printStackTrace(); }
        }
    }

    @FXML
    private void openLogMealPopup(ActionEvent event) {
        String selectedDate = datePicker.getValue() != null ? datePicker.getValue().toString() : LocalDate.now().toString();
        openLogMealPopupForDate(selectedDate);
    }

    @FXML
    private void openLogMealPopupForDate(String date) {
        Stage dialog = new Stage();
        dialog.setTitle("Log Meal for " + date);

        VBox root = new VBox(12);
        root.setPadding(new Insets(20));

        TextField calField = new TextField(); calField.setPromptText("Calories");
        TextField protField = new TextField(); protField.setPromptText("Protein (g)");
        TextField carbField = new TextField(); carbField.setPromptText("Carbs (g)");
        TextField fatField = new TextField(); fatField.setPromptText("Fats (g)");

        Label errorLabel = new Label(); errorLabel.setStyle("-fx-text-fill: red;");

        Button submitBtn = new Button("Add Meal"); submitBtn.setPrefWidth(120);

        submitBtn.setOnAction(e -> {
            try {
                int addCal = Integer.parseInt(calField.getText().trim());
                int addProt = Integer.parseInt(protField.getText().trim());
                int addCarb = Integer.parseInt(carbField.getText().trim());
                int addFat = Integer.parseInt(fatField.getText().trim());

                Map<String, Object> dailyConsumed = dailyLogsCache.getOrDefault(date, new HashMap<>());
                dailyConsumed.put("calories", ((Number) dailyConsumed.getOrDefault("calories", 0)).intValue() + addCal);
                dailyConsumed.put("protein", ((Number) dailyConsumed.getOrDefault("protein", 0)).intValue() + addProt);
                dailyConsumed.put("carbs", ((Number) dailyConsumed.getOrDefault("carbs", 0)).intValue() + addCarb);
                dailyConsumed.put("fats", ((Number) dailyConsumed.getOrDefault("fats", 0)).intValue() + addFat);
                dailyLogsCache.put(date, dailyConsumed);

                // Update Firestore
                new Thread(() -> {
                    try {
                        Map<String, Object> updateMap = new HashMap<>();
                        updateMap.put("daily." + date + ".calories", dailyConsumed.get("calories"));
                        updateMap.put("daily." + date + ".protein", dailyConsumed.get("protein"));
                        updateMap.put("daily." + date + ".carbs", dailyConsumed.get("carbs"));
                        updateMap.put("daily." + date + ".fats", dailyConsumed.get("fats"));
                        FirestoreContext.getFirestore()
                                .collection("users")
                                .document(userId)
                                .update(updateMap)
                                .get();
                    } catch (Exception ex) { ex.printStackTrace(); }
                }).start();

                Platform.runLater(() -> updateConsumedValuesForDate(date));
                dialog.close();
            } catch (NumberFormatException ex) {
                errorLabel.setText("Please enter valid numbers.");
            }
        });

        root.getChildren().addAll(new Label("Enter Meal for " + date + ":"), calField, protField, carbField, fatField, errorLabel, submitBtn);

        Scene scene = new Scene(root, 300, 350);
        dialog.setScene(scene);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.showAndWait();
    }

    @FXML
    private void goBack() throws IOException {
        if (mainApp != null && userId != null) mainApp.switchToDashboard(userId);
        else System.err.println("Error: mainApp or userId not set.");
    }

    @FXML
    private void updateConsumedValuesForDate(String date) {
        Map<String, Object> consumed = dailyLogsCache.getOrDefault(date, new HashMap<>());
        consumedCalories.set(((Number) consumed.getOrDefault("calories", 0)).intValue());
        consumedProtein.set(((Number) consumed.getOrDefault("protein", 0)).intValue());
        consumedCarbs.set(((Number) consumed.getOrDefault("carbs", 0)).intValue());
        consumedFats.set(((Number) consumed.getOrDefault("fats", 0)).intValue());
    }
}
