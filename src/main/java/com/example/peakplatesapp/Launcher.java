package com.example.peakplatesapp;

import java.io.File;
import java.lang.reflect.Method;

public class Launcher {
    public static void main(String[] args) {
        try {
            // Check if JavaFX Application class is available
            Class.forName("javafx.application.Application");

            // Use reflection to call Application.launch(MainApp.class, args)
            Class<?> appClass = Class.forName("javafx.application.Application");
            Method launch = appClass.getMethod("launch", Class.class, String[].class);
            Class<?> mainAppClass = Class.forName("com.example.peakplatesapp.MainApp");
            launch.invoke(null, mainAppClass, (Object) args);

        } catch (ClassNotFoundException e) {
            System.err.println("JavaFX classes not found on the classpath.");
            System.err.println("Two options to run:");
            System.err.println("  1) Configure IntelliJ run configuration with JavaFX SDK or add VM options (recommended). See README_INTELLIJ.md");
            System.err.println("  2) Let the project run via Maven (this will run './mvnw clean javafx:run').");
            System.err.println("Attempting to run via Maven wrapper './mvnw javafx:run'...");

            try {
                ProcessBuilder pb = new ProcessBuilder();
                // Use platform-appropriate mvnw
                String wrapper = "./mvnw";
                if (System.getProperty("os.name").toLowerCase().contains("win")) {
                    wrapper = "mvnw.cmd";
                }
                pb.command(wrapper, "clean", "javafx:run");
                pb.directory(new File(System.getProperty("user.dir")));
                pb.inheritIO();
                Process p = pb.start();
                int code = p.waitFor();
                System.exit(code);
            } catch (Exception ex) {
                ex.printStackTrace();
                System.err.println("Failed to run via Maven.");
                System.exit(1);
            }
        } catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException | java.lang.reflect.InvocationTargetException ex) {
            ex.printStackTrace();
            System.exit(1);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }
}
