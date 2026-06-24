<div align="center">

# 🚚 OmniRoute

### Smart Logistics & Fleet Optimizer

AI-powered logistics and fleet optimization platform featuring dynamic route planning, real-time GPS tracking, geofencing, offline-first synchronization, and performance analytics.

![Flutter](https://img.shields.io/badge/Flutter-3.x-blue?logo=flutter)
![Flask](https://img.shields.io/badge/Flask-Python-black?logo=flask)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Database-blue?logo=postgresql)
![License](https://img.shields.io/badge/License-MIT-green)

</div>

---

## 📖 Overview

OmniRoute is a modern logistics and fleet management platform designed to help delivery drivers, couriers, and field service agents optimize their daily operations.

The platform leverages intelligent route planning, real-time GPS tracking, geofencing, offline-first capabilities, and analytics dashboards to improve delivery efficiency, reduce travel time, and enhance operational visibility.

---

## ✨ Features

### 🗺️ Dynamic Route Optimization

- Plan routes with multiple delivery destinations
- Calculate the most efficient stop sequence
- Display optimized routes on an interactive map
- Provide Estimated Time of Arrival (ETA) for each destination
- Recalculate routes based on traffic conditions

### 📍 Live Location Tracking

- Real-time GPS tracking
- Driver location monitoring
- Interactive map visualization
- Route progress tracking
- Delivery status updates

### 🔔 Geofencing & Notifications

- Create delivery zones
- Detect arrival and departure automatically
- Trigger local push notifications
- Delivery reminders and confirmations
- Automated workflow updates

### 📊 Performance Analytics Dashboard

Monitor operational performance through:

- Total deliveries completed
- Weekly delivery trends
- Fuel efficiency metrics
- Distance travelled
- Average delivery time
- Time saved through optimization
- Driver productivity score

### 📡 Offline-First Synchronization

Designed for areas with poor connectivity.

- Store delivery data locally
- Complete deliveries offline
- Queue pending actions automatically
- Synchronize data once internet connection is restored
- View sync status in real time

### 📷 Proof of Delivery

- Capture delivery photos
- Collect customer signatures
- Scan QR codes and barcodes
- Generate delivery confirmations
- Store proof securely in the cloud

---

## 🏗️ System Architecture

```text
Flutter Mobile App
        │
        ▼
 REST API (Flask)
        │
 ┌──────┼──────┐
 ▼      ▼      ▼
PostgreSQL Redis AWS S3
        │
        ▼
Firebase Cloud Messaging
```

---

## 📱 Mobile Application

### Driver Features

- Daily delivery task management
- Route optimization
- Navigation assistance
- Delivery completion workflow
- Offline operation support
- Performance dashboard

### Manager Features

- Fleet monitoring
- Driver tracking
- Delivery assignment
- Operational analytics
- Performance reporting
- Route management

---

## 🛠️ Tech Stack

### Frontend

- Flutter
- Dart
- Material 3
- Google Maps SDK / Mapbox

### Backend

- Python
- Flask
- REST API

### Database

- PostgreSQL
- Redis

### Cloud Services

- AWS EC2
- AWS S3
- AWS Lambda
- Firebase Cloud Messaging (FCM)

### Local Storage

- SQLite
- Hive

---

## 📂 Project Structure

```text
omniroute-smart-logistics/

.
├── .env.example                # Example environment configuration file
├── .gitignore                  # Root-level Git ignore rules
├── build.gradle.kts            # Root-level build configuration script
├── gradle.properties           # Global Gradle configuration properties
├── local.properties            # Local SDK paths (automatically generated)
├── metadata.json               # App metadata definitions
├── settings.gradle.kts         # Project settings and module declarations
├── assets/                     # Global static assets
├── gradle/                     # Gradle Wrapper files
└── app/                        # Main application module
    ├── .gitignore              # App-module level Git ignore rules
    ├── build.gradle.kts        # App-module build configuration script
    ├── proguard-rules.pro      # ProGuard rules for code shrinking and obfuscation
    └── src/
        ├── androidTest/        # Instrumented tests running on physical devices/emulators
        ├── test/               # Local unit tests running on the JVM
        └── main/
            ├── AndroidManifest.xml   # Core Android system manifest declaration
            ├── java/
            │   └── com/
            │       └── example/
            │           ├── data/                   # Data Layer (Database, APIs, Repositories)
            │           │   ├── api/
            │           │   │   └── GeminiClient.kt        # API client integration (e.g., Gemini AI API)
            │           │   ├── database/
            │           │   │   ├── DeliveryDao.kt         # Database Access Object for Delivery entities
            │           │   │   ├── DeliveryDatabase.kt    # Room Database configuration
            │           │   │   └── DeliveryEntity.kt      # Database schema entity representing deliveries
            │           │   └── repository/
            │           │       └── DeliveryRepository.kt  # Single source of truth interfacing DB and API
            │           └── ui/                     # Presentation Layer (Jetpack Compose Screens & ViewModels)
            │               ├── MainActivity.kt            # Entry-point Activity for the application
            │               ├── screens/
            │               │   ├── AuthenticationScreen.kt # Login/Auth screen layout and interactions
            │               │   └── MainDashboard.kt        # Primary dashboard/delivery tracking screen
            │               ├── theme/                     # App design tokens and styling configurations
            │               │   ├── Color.kt               # Palette definitions
            │               │   ├── Theme.kt               # Jetpack Compose Theme wrappers
            │               │   └── Type.kt                # Typography configurations
            │               └── viewmodel/
            │                   └── DeliveryViewModel.kt   # Business logic and UI state management
            └── res/            # App Resources
                ├── drawable/           # Vector drawables and raster images
                ├── mipmap-anydpi-v26/  # Adaptive launcher icons
                ├── mipmap-hdpi/        # Launcher icons for high-density screens
                ├── mipmap-mdpi/        # Launcher icons for medium-density screens
                ├── mipmap-xhdpi/       # Launcher icons for extra-high-density screens
                ├── mipmap-xxhdpi/      # Launcher icons for double-extra-high-density screens
                ├── mipmap-xxxhdpi/     # Launcher icons for triple-extra-high-density screens
                ├── values/             # XML-based style elements
                │   ├── colors.xml      # Color resources fallback
                │   ├── strings.xml     # Hardcoded application string constants
                │   └── themes.xml      # Standard Android system theme overrides
                └── xml/                # Custom system resource configurations (e.g., file paths, network configs)

```

---

## 🚀 Getting Started

### Prerequisites

- Flutter SDK
- Python 3.11+
- PostgreSQL
- Git

### Clone Repository

```bash
git clone https://github.com/yourusername/omniroute-smart-logistics.git

cd omniroute-smart-logistics
```

### Mobile Application Setup

```bash
cd mobile-app

flutter pub get

flutter run
```

### Backend Setup

```bash
cd backend

python -m venv venv

source venv/bin/activate

pip install -r requirements.txt

flask run
```

---

## 📸 Screenshots

### Dashboard

<img width="100%" alt="Dashboard Placeholder" src="https://via.placeholder.com/1200x600?text=Dashboard+Preview">

### Route Optimization

<img width="100%" alt="Route Preview" src="https://via.placeholder.com/1200x600?text=Route+Optimization">

### Fleet Tracking

<img width="100%" alt="Fleet Tracking" src="https://via.placeholder.com/1200x600?text=Fleet+Tracking">

---

## 🎯 Future Enhancements

- AI-powered route recommendations
- Predictive delivery delay analysis
- Driver behavior monitoring
- Fuel consumption prediction
- Fleet maintenance scheduling
- Multi-warehouse support
- Real-time traffic intelligence
- Machine learning delivery forecasting

---

## 💼 Portfolio Highlights

This project demonstrates:

- Mobile App Development with Flutter
- Backend API Development with Flask
- Geolocation & Mapping Integration
- Real-Time Tracking
- Geofencing
- Push Notifications
- Offline-First Architecture
- Database Design
- Cloud Integration
- Data Visualization & Analytics
- Enterprise Application Development

---

## 📄 License

This project is licensed under the MIT License.

---

<div align="center">

Built with ❤️ using Flutter, Flask, PostgreSQL, and AWS.

</div>
