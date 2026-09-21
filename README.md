# Aurex

Aurex is an Android-first personal finance app designed to help people understand where their money is going, what they owe, and what is due next. The project is built with Kotlin Multiplatform and Compose, with shared business logic and a platform-specific Android client.

## Overview

Aurex focuses on the practical parts of money management:

- tracking balances across accounts
- recording income and expenses
- organizing transactions by category
- managing debts, loans, and borrowing/lending flows
- viewing upcoming due dates and reminders
- setting monthly budgets per category
- scheduling recurring transactions that post themselves
- importing and exporting transactions as CSV
- keeping a local data store with optional cloud backup
- locking the app behind a PIN or fingerprint
- customizing theme behavior and app preferences

## Current status

The Android app is the primary, fully supported client in this repository. The iOS project is present as a starter host, but it is not yet the main Aurex experience.

## Tech stack

- Kotlin Multiplatform
- Jetpack Compose
- SQLDelight for local persistence
- Kotlin Coroutines and Kotlin Serialization
- Firebase Authentication and Firestore for Android backup flows
- Gradle Kotlin DSL

## Repository layout

| Path | Purpose |
| --- | --- |
| `androidApp` | Android application, navigation, screens, notifications, backup integration, and Firebase setup |
| `sharedLogic` | Shared domain models, use cases, repositories, ViewModels, and the SQLDelight database layer |
| `sharedUI` | Shared Compose UI module and shared UI dependencies |
| `iosApp` | iOS application host and Xcode project |
| `design` | Design explorations and visual concept files |
| `firebase.json` / `firestore.rules` / `firestore.indexes.json` | Firebase project configuration and rules |

## Requirements

- Android Studio with Kotlin Multiplatform support
- JDK 17
- Android SDK configured for the project
- An emulator or physical Android device running API 24 or newer
- Xcode on macOS only if you plan to work on the iOS target

The project includes the Gradle wrapper, so you do not need a separate Gradle installation.

## Getting started

1. Clone the repository.
2. Open the project root in Android Studio.
3. Let Gradle sync and install any required Android SDK components.
4. Run the `androidApp` configuration on an emulator or device.

Firebase-backed features require a valid Android configuration in `androidApp/google-services.json`.

## Build and test

From the repository root, run:

### Windows

```powershell
./gradlew.bat :androidApp:assembleDebug
./gradlew.bat :sharedUI:testAndroidHostTest :sharedLogic:testAndroidHostTest
./gradlew.bat :sharedLogic:iosSimulatorArm64Test
```

### macOS / Linux

```bash
./gradlew :androidApp:assembleDebug
./gradlew :sharedUI:testAndroidHostTest :sharedLogic:testAndroidHostTest
./gradlew :sharedLogic:iosSimulatorArm64Test
```

To work on the iOS host, open the Xcode project in `iosApp` and run it on a simulator.

## Architecture

The app separates concerns by platform:

- `sharedLogic` contains business rules, model definitions, data access, and platform-independent state management.
- `androidApp` contains Android-specific integrations such as Firebase authentication and Firestore, notifications, Google sign-in, and CSV export flows.
- `sharedUI` provides the shared Compose foundation for UI composition and reusable components.
- `iosApp` is currently a host for future iOS integration rather than the main Aurex experience.

## Contributing

Keep platform-specific integrations in the appropriate source set, and put shared logic in `sharedLogic` wherever possible. When changing shared behavior, add or update tests alongside the code.

## License

No license has been added to this repository yet.
