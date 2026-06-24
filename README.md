# 🚚 OmniRoute

> Smart Logistics & Fleet Optimizer built with Kotlin, Jetpack Compose, Room Database, Google Maps APIs, and AI-powered route optimization.

![Kotlin](https://img.shields.io/badge/Kotlin-2.x-purple?logo=kotlin)
![Android](https://img.shields.io/badge/Android-Native-green?logo=android)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-UI-blue)
![Room](https://img.shields.io/badge/Room-Database-orange)
![MVVM](https://img.shields.io/badge/Architecture-MVVM-red)

---

## 📖 Overview

OmniRoute is a modern Android logistics platform designed for delivery drivers, courier services, and fleet operators.

The application helps drivers optimize routes, track deliveries in real time, manage delivery records offline, and analyze performance metrics through an intuitive mobile-first experience.

Built using modern Android development practices, the project demonstrates expertise in:

* Kotlin
* Jetpack Compose
* MVVM Architecture
* Room Database
* Offline-First Development
* REST API Integration
* AI-Powered Optimization
* Location Services
* Enterprise Mobile Application Design

---

## ✨ Features

### 🗺️ Smart Route Optimization

* Generate optimized routes for multiple destinations
* Calculate shortest and fastest delivery paths
* Reduce travel distance and fuel consumption
* Dynamic route recalculation

### 📍 Real-Time Delivery Tracking

* GPS-based location tracking
* Live route progress monitoring
* Delivery status updates
* Route visualization on maps

### 🔔 Geofencing Notifications

* Detect arrival at delivery locations
* Automatic zone entry and exit tracking
* Delivery reminders
* Local push notifications

### 📊 Analytics Dashboard

Monitor operational performance through:

* Total deliveries completed
* Weekly delivery trends
* Average delivery duration
* Distance travelled
* Time saved through optimization
* Driver productivity metrics

### 📡 Offline-First Architecture

Designed for delivery operations in areas with limited connectivity.

* Local data persistence using Room Database
* Offline delivery completion
* Automatic synchronization when connectivity returns
* Reliable data recovery

### 🤖 AI-Assisted Logistics

* Intelligent route suggestions
* Delivery planning assistance
* AI-powered operational insights
* Integrated Gemini API support

---

## 🏗️ Architecture

The application follows Clean MVVM Architecture principles.

```text
UI Layer (Jetpack Compose)
        │
        ▼
ViewModel Layer
        │
        ▼
Repository Layer
        │
 ┌──────┴──────┐
 ▼             ▼
Room DB     API Services
        │
        ▼
Data Sources
```

### Architecture Components

#### UI Layer

Responsible for rendering screens and handling user interactions.

* MainActivity
* Dashboard Screen
* Authentication Screen
* Compose UI Components

#### ViewModel Layer

Responsible for:

* UI State Management
* Business Logic
* Lifecycle Awareness
* Data Processing

#### Repository Layer

Acts as the single source of truth between:

* Remote APIs
* Local Database
* Business Logic

#### Data Layer

Handles:

* API Communication
* Local Persistence
* Data Synchronization
* Entity Mapping

---

## 📂 Project Structure

```text
app/src/main/java/com/example/

├── data/
│   ├── api/
│   │   └── GeminiClient.kt
│   │
│   ├── database/
│   │   ├── DeliveryDao.kt
│   │   ├── DeliveryDatabase.kt
│   │   └── DeliveryEntity.kt
│   │
│   └── repository/
│       └── DeliveryRepository.kt
│
└── ui/
    ├── MainActivity.kt
    │
    ├── screens/
    │   ├── AuthenticationScreen.kt
    │   └── MainDashboard.kt
    │
    ├── viewmodel/
    │   └── DeliveryViewModel.kt
    │
    └── theme/
        ├── Color.kt
        ├── Theme.kt
        └── Type.kt
```

---

## 🛠️ Tech Stack

### Mobile Development

* Kotlin
* Android SDK
* Jetpack Compose
* Material Design 3

### Architecture

* MVVM
* Repository Pattern
* State Management

### Database

* Room Database
* SQLite

### Networking

* REST APIs
* Gemini API Integration

### Android Jetpack

* ViewModel
* Navigation
* Lifecycle Components
* Room Persistence

### Development Tools

* Android Studio
* Gradle Kotlin DSL
* Git & GitHub

---

## 🚀 Getting Started

### Prerequisites

* Android Studio Hedgehog or newer
* JDK 17+
* Android SDK
* Gradle 8+

### Clone Repository

```bash
git clone https://github.com/yourusername/omniroute.git

cd omniroute
```

### Configure Environment

Create a local configuration file:

```properties
API_KEY=YOUR_API_KEY
MAPS_API_KEY=YOUR_MAPS_API_KEY
```

### Build Project

```bash
./gradlew build
```

### Run Application

```bash
./gradlew installDebug
```

or simply run from Android Studio.

---

## 🧪 Testing

### Unit Tests

```bash
./gradlew test
```

### Instrumentation Tests

```bash
./gradlew connectedAndroidTest
```

---

## 📸 Screenshots

### Authentication

> Login and user authentication flow.

### Dashboard

> Overview of deliveries, performance metrics, and route status.

### Route Planner

> Intelligent route optimization interface.

### Analytics

> Weekly performance and productivity tracking.

---

## 🎯 Learning Objectives

This project was built to strengthen knowledge in:

* Android Native Development
* Jetpack Compose
* MVVM Architecture
* Clean Architecture Principles
* Offline-First Mobile Applications
* Local Data Persistence
* AI API Integration
* Logistics System Design
* Scalable Android Applications

---

## 💼 Portfolio Highlights

Through this project, I demonstrate experience with:

✅ Kotlin Development

✅ Android Native Applications

✅ Jetpack Compose

✅ Room Database

✅ MVVM Architecture

✅ Repository Pattern

✅ Offline Data Synchronization

✅ AI Integration

✅ Location-Based Services

✅ Enterprise Mobile Solutions

---

## 📄 License

This project is licensed under the MIT License.

---

### Author

**Wan Muhammad Alif Firdaus**

Junior Mobile Developer specializing in Android and Full-Stack Development.

Passionate about building scalable mobile applications, modern Android solutions, and enterprise-grade software systems.
