# Aurex

Aurex is a personal finance app for understanding where your money is, what you
owe, and what is due next. It is built with Kotlin Multiplatform and Jetpack
Compose, with a shared domain and data layer and an Android application as the
current primary client.

## What it does

- See a financial dashboard and overview of your current position.
- Track accounts, income, expenses, and transaction categories.
- Manage debts, borrowed and lent money, and EMI schedules.
- Review spending insights and upcoming payment reminders.
- Choose light, dark, or system theme behavior.
- Back up and restore data with Google sign-in and Firebase Firestore on Android.
- Share transaction data as CSV from Android.

## Project status

The Android application is the supported end-to-end client. The iOS target and
shared iOS framework are present in the project, but `iosApp` currently contains
the default SwiftUI host and is not yet wired to the Aurex experience.

## Tech stack

- Kotlin Multiplatform with Kotlin 2.4
- Jetpack Compose and Compose Multiplatform
- SQLDelight for local SQLite persistence
- Kotlin Coroutines and Kotlin Serialization
- Firebase Authentication and Cloud Firestore for Android backup
- Gradle Kotlin DSL

## Repository layout

| Path | Purpose |
| --- | --- |
| `androidApp` | Android application, navigation, screens, notifications, and Firebase integration |
| `sharedLogic` | Shared domain models, use cases, repositories, ViewModels, and SQLDelight database |
| `sharedUI` | Shared Compose UI module and platform-aware UI dependencies |
| `iosApp` | iOS application host and Xcode project |
| `design` | Design explorations and source design files |

## Requirements

- Android Studio with Kotlin Multiplatform support
- JDK 17
- Android SDK with API 37 installed
- Android device or emulator running API 24 or newer
- Xcode on macOS only, when working on the iOS target

The repository includes the Gradle wrapper, so a separate Gradle installation is
not required.

## Getting started

1. Clone the repository and open the project root in Android Studio.
2. Let Gradle sync and allow Android Studio to install any missing SDK tools.
3. Select the `androidApp` run configuration and launch it on an emulator or device.

Firebase features require a valid Android Firebase project configuration in
`androidApp/google-services.json`. Authentication and Firestore rules are defined
in the Firebase configuration files at the repository root.

## Build and test

From the repository root, run the following on Windows:

```powershell
.\gradlew.bat :androidApp:assembleDebug
.\gradlew.bat :sharedUI:testAndroidHostTest :sharedLogic:testAndroidHostTest
.\gradlew.bat :sharedLogic:iosSimulatorArm64Test
```

On macOS or Linux, use `./gradlew` instead of `./gradlew.bat`.

To run the iOS host, open [`iosApp`](./iosApp) in Xcode and select an iOS
simulator. The current host is a scaffold for the future shared Aurex UI.

## Architecture

The application keeps business logic in `sharedLogic` so it can be reused by
each platform. Its repositories use SQLDelight for local storage and expose
platform-independent use cases and ViewModels. `androidApp` provides Android-
specific services such as Firebase backup, Google sign-in, notifications, and
CSV sharing. UI screens and navigation currently live in the Android app, while
`sharedUI` contains the Compose Multiplatform foundation for further sharing.

## Contributing

Keep platform-specific integrations in the relevant source set, put reusable
business behavior in `sharedLogic`, and add or update tests alongside changes
to shared behavior. Before opening a pull request, run the Android host tests
and build the debug application.

## License

No license has been added to this repository yet.
