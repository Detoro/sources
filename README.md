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
| **Architecture** | MVVM with StateFlow/SharedFlow |
| **Database** | [Room](https://developer.android.com/training/data-storage/room) |
| **Networking** | [Retrofit](https://square.github.io/retrofit/) + OkHttp |
| **Image Loading** | [Coil](https://coil-kt.github.io/coil/) |
| **Async** | Kotlin Coroutines & Flow |
| **Background Tasks** | WorkManager |
| **Notifications** | Firebase Cloud Messaging (FCM) |
| **Media Storage** | [Cloudinary]([https://developer.android.com/jetpack/compose/glance](https://console.cloudinary.com/app/c-65a873f198d37728cb3399541e13f0/assets/media_library/search?q=&view_mode=mosaic) |

---

## ⚙️ Setup & Installation

### Prerequisites

* JDK 17 or higher
* Android Studio (latest stable)
* PostgreSQL running locally or remotely
* A Cloudinary Account
* A Firebase Project (with `google-services.json` and `google-account.json` configured)
