# Peak Plates Recipe Upload Feature

## Overview
A complete recipe upload and sharing system has been added to the Peak Plates dashboard. Users can now upload recipes with images, descriptions, and interact with other recipes through like, favorite, and share buttons.

## New Files Created

### 1. **Recipe.java** (`/src/main/java/com/example/peakplatesapp/Recipe.java`)
A data model class that represents a recipe with the following fields:
- `id`: Unique identifier for the recipe
- `userId`: ID of the user who created the recipe
- `username`: Username of the recipe creator
- `description`: Recipe description (ingredients, instructions, tips)
- `imagePath`: Path to the recipe image file
- `imageData`: Byte array containing the image data
- `timestamp`: Creation timestamp
- `likes`: Number of likes
- `favorites`: Number of times favorited
- `shares`: Number of times shared
- `likedByUsers`: List of user IDs who liked the recipe
- `favoriteByUsers`: List of user IDs who favorited the recipe

### 2. **RecipeUploadController.java** (`/src/main/java/com/example/peakplatesapp/RecipeUploadController.java`)
Handles the recipe upload functionality:
- **handleSelectImage()**: Opens file chooser to select an image from the user's computer
- **handleUpload()**: Validates form and uploads recipe with image data to Firestore
- **handleCancel()**: Returns user to the dashboard
- **initialize()**: Sets up character counter for description field (max 2000 characters)
- Image preview functionality to show selected image before upload

### 3. **UploadRecipe.fxml** (`/src/main/resources/com/example/peakplatesapp/UploadRecipe.fxml`)
FXML UI layout for the recipe upload page featuring:
- Title and description section
- Image selection area with preview
- Recipe description text area with character counter
- Upload and Cancel buttons with styling

### 4. **Updated Dashboard.fxml** (`/src/main/resources/com/example/peakplatesapp/Dashboard.fxml`)
Enhanced dashboard layout with:
- Welcome message with username
- "📤 Upload Recipe" button in the top navigation
- Scrollable recipes container showing all uploaded recipes
- Bottom navigation bar with Home, Favorites, Settings, and Logout buttons

## Updated Files

### 1. **DashboardController.java**
New functionality:
- `loadRecipes()`: Fetches all recipes from Firestore and displays them
- `createRecipeCard()`: Creates a styled recipe card with image, description, and action buttons
- `handleLike()`: Allows users to like recipes (prevents duplicate likes)
- `handleFavorite()`: Allows users to favorite recipes (prevents duplicates)
- `handleShare()`: Increments share count for recipes
- `handleUploadRecipe()`: Navigates to the upload recipe page
- `handleLogout()`: Logs user out and returns to login page

### 2. **MainApp.java**
New method:
- `switchToUploadRecipe(String userId, String username)`: Navigates to the upload recipe page and passes user information to the controller

## How to Use

### Uploading a Recipe:
1. Click the "📤 Upload Recipe" button on the dashboard
2. Click "🖼️ Choose Image" to select a recipe image from your computer
3. Enter a recipe description in the text area (up to 2000 characters)
4. Click "📤 Upload Recipe" to upload
5. You'll be redirected back to the dashboard where your recipe appears

### Interacting with Recipes:
- **❤️ Like**: Click to like a recipe (each user can like once)
- **⭐ Favorite**: Click to favorite a recipe
- **📤 Share**: Click to increment the share counter

## Firestore Database Structure

### Recipes Collection
Each recipe is stored in the `recipes` collection with the following structure:
```
recipes/
  {recipeId}/
    - id: string
    - userId: string
    - username: string
    - description: string
    - imagePath: string
    - imageData: bytes (image binary data)
    - timestamp: long
    - likes: integer
    - favorites: integer
    - shares: integer
    - likedByUsers: array
    - favoriteByUsers: array
```

## Features Implemented

✅ Image Selection - Users can browse and select images from their computer
✅ Image Preview - Selected images are previewed before upload
✅ Recipe Description - Rich text descriptions with character counter
✅ Upload to Firestore - Recipes and images stored in Firestore database
✅ Recipe Display - All recipes shown on dashboard as styled cards
✅ Like Functionality - Users can like recipes (duplicate prevention)
✅ Favorite Functionality - Users can favorite recipes (duplicate prevention)
✅ Share Tracking - Share count increments when users share recipes
✅ User Attribution - Each recipe displays the creator's username
✅ Responsive Design - UI adapts to different screen sizes

## Technical Details

- **Image Storage**: Images are converted to byte arrays and stored directly in Firestore
- **Concurrent Operations**: Uses Firestore ApiFuture for async operations
- **Input Validation**: Validates that both image and description are provided before upload
- **Error Handling**: Comprehensive error messages and alerts for user feedback
- **User Tracking**: Each recipe action (like, favorite, share) is tracked per user to prevent duplicates

## Next Steps (Optional Enhancements)

- Add recipe editing and deletion functionality
- Implement filtering by cuisine type or difficulty level
- Add recipe comments and ratings
- Implement user following/followers system
- Add recipe search functionality
- Create recipe categories or tags
