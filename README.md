# Sources

A full-stack, Android-native comic discovery and reading platform. Sources provides a great reading experience, robust community engagement, real-time chat, and an intelligent recommendation engine.

Built entirely in Kotlin, it's a monorepo architecture sharing data models between a Jetpack Compose frontend and a Ktor backend.

## 🚀 Features

* **Comic Reader & Library Management:** Read comics with vertical/horizontal scrolling, track reading progress, and manage personal libraries and subscriptions. Sideload local CBZ files or upload them to the server with automatic page extraction.
* **Community Platform:** Dedicated engagement feeds for users to create posts, share comics, and participate in nested comment threads on both community posts and specific comic chapters.
* **Real-Time Messaging:** Direct messaging with encrypted payload support, chat search, delivery receipts (single/double ticks), and friend request management. Includes rich sharing capabilities to drop comics or posts directly into chats.
* **Hybrid Recommendation Engine:** A dual-layer algorithm utilizing vector space cosine similarity (Content-Based Filtering via Genre axes) blended with audience overlap analysis (Item-Item Collaborative Filtering) to provide highly personalized comic feeds.
* **Push Notifications:** Integrated Firebase Cloud Messaging (FCM) for likes, follows, comments, and direct messages.

## 🛠️ Tech Stack

| Category | Technology |
|----------|------------|
| **Language** | [Kotlin](https://kotlinlang.org/) 100% |
| **UI Framework** | [Jetpack Compose](https://developer.android.com/jetpack/compose) |
| **Design System** | [Material Design 3](https://m3.material.io/) |
| **Audio Engine** | [Media3 ExoPlayer](https://developer.android.com/guide/topics/media/media3) + FFmpeg |
| **Architecture** | MVVM with StateFlow/SharedFlow |
| **DI** | [Hilt](https://dagger.dev/hilt/) |
| **Database** | [Room](https://developer.android.com/training/data-storage/room) |
| **Networking** | [Retrofit](https://square.github.io/retrofit/) + OkHttp |
| **Image Loading** | [Coil](https://coil-kt.github.io/coil/) |
| **Async** | Kotlin Coroutines & Flow |
| **Background Tasks** | WorkManager |
| **Notifications** | Firebase Cloud Messaging (FCM) |
| **Media Storage** | [Cloudinary]([https://developer.android.com/jetpack/compose/glance](https://console.cloudinary.com/app/c-65a873f198d37728cb3399541e13f0/assets/media_library/search?q=&view_mode=mosaic)) |

---

## 📁 Project Structure

The project is structured as a monorepo:

```text
SourcesProject/
├── frontend/      # Android App (Jetpack Compose)
├── backend/       # Ktor Server 
└── shared/        # Pure Kotlin module for shared data models

```

## ⚙️ Setup & Installation

### Prerequisites

* JDK 17 or higher
* Android Studio (latest stable)
* PostgreSQL running locally or remotely
* A Cloudinary Account
* A Firebase Project (with `google-services.json` and `google-account.json` configured)

### 1. Backend Environment Setup

Create a `.env` file in the root of the `backend/` directory. **Do not commit this file.**

```env
# Database Configuration
DB_URL=jdbc:postgresql://localhost:5432/sources_db
DB_USER=your_postgres_user
DB_PASSWORD=your_postgres_password

# JWT Authentication
JWT_SECRET=your_secret_jwt_key
JWT_ISSUER=http://0.0.0.0:8080
JWT_AUDIENCE=sources-users

# Cloudinary
CLOUDINARY_URL=cloudinary://API_KEY:API_SECRET@CLOUD_NAME

```

*Note: Ensure your Firebase `google-account.json` service account file is placed in the backend's `src/main/resources/` folder.*

### 2. Running the Project

**Start the Backend:**

1. Open the `backend/` folder in IntelliJ IDEA or Android Studio.
2. Run the `Application.kt` file. The Ktor server will start on `http://0.0.0.0:8080`.
3. The Exposed ORM will automatically generate the required database tables on startup.

**Start the Frontend:**

1. Open the `frontend/` folder in Android Studio.
2. Sync the project with Gradle files to ensure the `:shared` module links successfully.
3. Build and run the app on an emulator or physical device.

## 🧠 Architecture Highlights

* **URL-Based State Routing:** Deep links and internal navigation rely on ID-based routing (e.g., `overview/{comicId}`) rather than passing large objects, ensuring robust state restoration and seamless sharing.
* **In-Memory Search Decryption:** Chat search utilizes Kotlin Flow debouncing and in-memory decryption to securely search through encrypted message payloads without exposing plaintext to local SQLite FTS tables.
* **Vector Recommendations:** Comics are mapped to a multi-dimensional DoubleArray based on fixed taxonomy genres. User vectors are dynamically calculated based on centered rating values (-2 to +2) to provide rapid cosine-similarity matches.
