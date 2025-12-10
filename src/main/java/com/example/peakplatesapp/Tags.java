package com.example.peakplatesapp;

import java.util.Arrays;
import java.util.List;

/**
 * Centralized tag management for recipe filtering and categorization.
 */
public class Tags {
    // Predefined list of available tags
    public static final List<String> AVAILABLE_TAGS = Arrays.asList(
        // New tags
        "Weight Loss",
        "Muscle Gain",
        "Diabetic Friendly",
        "Vegetarian",
        "Dairy Free",
        "Anti-Inflammatory",
        "Nut Free",
        "Soy Free",
        "High Antioxidants",
        "Immunity Boosting",
        // Original tags
        "Whole Grains",
        "Greens",
        "High Protein",
        "High Fiber",
        "Low Calories",
        "Vegan",
        "Gluten Free",
        "Heart Healthy",
        "Low Sugar",
        "Low Sodium",
        "Gut Friendly",
        "No Added Sugar"
    );

    // Private constructor to prevent instantiation
    private Tags() {
    }

    /**
     * Returns the list of available tags for recipe filtering.
     * @return list of tag strings
     */
    public static List<String> getAvailableTags() {
        return AVAILABLE_TAGS;
    }
}
