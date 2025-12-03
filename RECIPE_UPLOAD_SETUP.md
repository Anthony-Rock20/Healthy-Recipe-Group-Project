# Recipe Upload Feature - Quick Start Guide

## What's New

Your Peak Plates app now has a complete recipe sharing system! Here's what was added:

### 🎯 Main Features

1. **Recipe Upload Page** - Accessible from the dashboard with a green upload button
2. **Image Selection** - Browse your computer and select recipe photos
3. **Recipe Description** - Add detailed descriptions (up to 2000 characters)
4. **Recipe Display** - All recipes appear as cards on the dashboard
5. **Interaction Buttons** - Like, Favorite, and Share recipes

---

## 📁 Files Added/Modified

### New Files:
- `src/main/java/.../Recipe.java` - Recipe data model
- `src/main/java/.../RecipeUploadController.java` - Upload functionality
- `src/main/resources/.../UploadRecipe.fxml` - Upload page UI

### Updated Files:
- `src/main/java/.../DashboardController.java` - Recipe display & interactions
- `src/main/resources/.../Dashboard.fxml` - New UI with upload button
- `src/main/java/.../MainApp.java` - Navigation method added

---

## 🚀 How to Use

### Uploading a Recipe:
```
1. Log in to Peak Plates
2. Click "📤 Upload Recipe" button (top right of dashboard)
3. Click "🖼️ Choose Image" to select a photo
4. Enter recipe details (ingredients, instructions, etc.)
5. Click "📤 Upload Recipe"
6. Recipe appears on your dashboard!
```

### Interacting with Recipes:
- **❤️ Like Button** - Like recipes (prevents duplicate likes per user)
- **⭐ Favorite Button** - Mark as favorite (prevents duplicates)
- **📤 Share Button** - Share the recipe and increment share counter

---

## 💾 Database Structure

Recipes are stored in Firestore with:
- Image data (binary)
- Description
- Creator information
- Like/Favorite/Share counts
- Timestamps

---

## ✅ Testing

The project compiles without errors and builds successfully with all new components integrated.

### To test:
```bash
./mvnw clean javafx:run
```

---

## 📝 Key Implementation Details

- **Image Storage**: Images are converted to byte arrays in Firestore
- **Duplicate Prevention**: Users can't like/favorite the same recipe twice
- **Real-time Updates**: Dashboard refreshes when actions are performed
- **Error Handling**: User-friendly alerts for all operations
- **Responsive UI**: Works well on different screen sizes

---

## 🔧 Technical Stack

- **Frontend**: JavaFX
- **Database**: Firebase Firestore
- **Build**: Maven
- **Java Version**: 23

---

## 📚 Additional Documentation

See `RECIPE_FEATURE_GUIDE.md` for comprehensive technical documentation.
