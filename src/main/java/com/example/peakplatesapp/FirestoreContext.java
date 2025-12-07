package com.example.peakplatesapp;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.cloud.FirestoreClient;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;


public class FirestoreContext {
    private static Firestore firestoreInstance;
    private static FirebaseAuth firebaseAuthInstance;

    public static Firestore getFirestore() {
        if (firestoreInstance == null) {
            initializeFirebase();
            firestoreInstance = FirestoreClient.getFirestore();
        }
        return firestoreInstance;
    }

    public static FirebaseAuth getAuth() {
        if (firebaseAuthInstance == null) {
            initializeFirebase();
            firebaseAuthInstance = FirebaseAuth.getInstance();
        }
        return firebaseAuthInstance;
    }

    private static synchronized void initializeFirebase() {
        if (FirebaseApp.getApps().isEmpty()) {
            try {
                InputStream stream = null;

                // Try classpath resource first (leading slash and without)
                stream = FirestoreContext.class.getResourceAsStream("/com/example/peakplatesapp/key.json");
                if (stream == null) {
                    stream = FirestoreContext.class.getResourceAsStream("key.json");
                }

                String usedPath = null;
                if (stream != null) {
                    usedPath = "classpath:/com/example/peakplatesapp/key.json";
                } else {
                    // Fallback to file system for development
                    String fsPath = "src/main/resources/com/example/peakplatesapp/key.json";
                    try {
                        stream = new FileInputStream(fsPath);
                        usedPath = fsPath;
                    } catch (IOException ioe) {
                        // Try target/classes (when running from IDE after build)
                        String targetPath = "target/classes/com/example/peakplatesapp/key.json";
                        stream = new FileInputStream(targetPath);
                        usedPath = targetPath;
                    }
                }

                if (stream == null) {
                    throw new IOException("key.json not found in classpath or resource paths");
                }

                System.out.println("Loading Firebase credentials from: " + usedPath);

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(stream))
                        .build();

                FirebaseApp.initializeApp(options);
                System.out.println("Firebase initialized successfully (credentials: " + usedPath + ")");
            } catch (IOException ex) {
                System.err.println("Failed to initialize Firebase: " + ex.getMessage());
                ex.printStackTrace();
                throw new RuntimeException("Firebase initialization failed: " + ex.getMessage(), ex);
            }
        }
    }

    /**
     * Quick check whether credentials file is present and can be parsed.
     * Returns null if OK, otherwise returns a human-readable error message.
     */
    public static String credentialsHealthCheck() {
        try {
            InputStream stream = FirestoreContext.class.getResourceAsStream("/com/example/peakplatesapp/key.json");
            if (stream == null) {
                stream = FirestoreContext.class.getResourceAsStream("key.json");
            }
            if (stream == null) {
                // try filesystem
                String fsPath = "src/main/resources/com/example/peakplatesapp/key.json";
                try {
                    stream = new FileInputStream(fsPath);
                } catch (IOException ioe) {
                    String targetPath = "target/classes/com/example/peakplatesapp/key.json";
                    try {
                        stream = new FileInputStream(targetPath);
                    } catch (IOException ioe2) {
                        return "key.json not found. Place Firebase service account JSON at src/main/resources/com/example/peakplatesapp/key.json";
                    }
                }
            }

            // Try to parse credentials
            try (InputStream in = stream) {
                GoogleCredentials.fromStream(in);
            }
            return null; // OK
        } catch (Exception e) {
            return "Failed to parse key.json: " + e.getMessage();
        }
    }
}
