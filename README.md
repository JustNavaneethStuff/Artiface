# ARTIFACE

Playful artistic selfie → caricature app for Android.

> Phase 1 foundation is in place. Full product docs land in Phase 8; this README is intentionally minimal until then.

## Current status

**Phase 5 — Room gallery**

- Room persistence for caricature results
- Gallery grid with Coil thumbnails, favourites filter, empty state, delete confirmation
- Detail via existing Result screen
- Settings “Clear local gallery” deletes Room rows and result files

## Modules

| Module | Role |
|--------|------|
| `app` | Application entry, navigation, DI composition |
| `core:common` | Shared Result / dispatchers |
| `core:designsystem` | Theme, typography, reusable Compose components |
| `core:model` | Immutable domain models |
| `core:network` | OkHttp / future Retrofit shell |
| `core:database` | Room database, DAOs, gallery entities |
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

## Phase 5 limitations

- Generated art is still a local stylized mock, not a remote model
- Failed jobs are not stored in the gallery (completed results only)
- Forced failure simulation exists on `FakeCaricatureGenerator.forceNextFailure` for tests/debug
- Physical-device camera validation still recommended — see `docs/CAMERA_TESTING.md`
