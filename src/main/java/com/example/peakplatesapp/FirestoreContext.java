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

                var stream = FirestoreContext.class.getResourceAsStream("key.json");
                if (stream == null) {
                    // Fallback to file system for development
                    stream = new FileInputStream("src/main/resources/com/example/peakplatesapp/key.json");
                }

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(stream))
                        .build();

                FirebaseApp.initializeApp(options);
                System.out.println("Firebase initialized successfully");
            } catch (IOException ex) {
                System.err.println("Failed to initialize Firebase: " + ex.getMessage());
                ex.printStackTrace();
                System.exit(1);
            }
        }
    }
}
