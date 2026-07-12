# ARTIFACE

Playful artistic selfie → caricature app for Android.

> Phase 1 foundation is in place. Full product docs land in Phase 8; this README is intentionally minimal until then.

## Current status

**Phase 2 — Onboarding & settings**

- DataStore preferences (onboarding, theme, contextual toggles)
- Real 3-page onboarding with Skip / Next / Get Started
- Settings screen with theme, privacy toggles, about, version
- Splash routes based on onboarding completion

## Modules

| Module | Role |
|--------|------|
| `app` | Application entry, navigation, DI composition |
| `core:common` | Shared Result / dispatchers |
| `core:designsystem` | Theme, typography, reusable Compose components |
| `core:model` | Immutable domain models |
| `core:network` | OkHttp / future Retrofit shell |
| `core:database` | Room shell |
| `core:preferences` | DataStore-backed user preferences |
| `core:testing` | Shared test helpers |
| `feature:*` | Feature UI shells (onboarding, camera, …) |

## Setup

Requirements:

- Android Studio (or JDK 17+)
- Android SDK Platform 36

1. Set `JAVA_HOME` to a JDK 17+ install (Android Studio JBR works):

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
```

2. Sync Gradle in Android Studio, or from the terminal:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :core:common:test
```

## Architecture (preview)

```mermaid
flowchart TB
  app[app] --> features[feature modules]
  features --> design[core:designsystem]
  features --> model[core:model]
  features --> common[core:common]
  app --> network[core:network]
  app --> database[core:database]
  network --> model
  database --> model
```

## Phase 2 limitations

- Clear gallery is a confirmed no-op until Room arrives in Phase 5
- Location context stores the preference only; no location permission or provider yet
- Camera remains a placeholder (Phase 3)
- Custom brand fonts not bundled yet (system serif/sans fallbacks)
