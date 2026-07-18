# Contributing

## Development flow

1. Use a feature branch off `main`.
2. Keep changes focused (one phase / concern when possible).
3. Run locally before opening a PR:

```bash
./gradlew testDebugUnitTest :app:lintDebug :app:assembleDebug
```

4. Prefer ViewModel / mapper / DAO unit tests for new behaviour.
5. Do not commit `local.properties`, keystores, or API keys.

## Code style

- Kotlin official code style (`kotlin.code.style=official`)
- Strings in `res/values` for user-facing copy
- Feature modules expose Route + ViewModel; composables do not touch DAOs/Retrofit directly

## Docs

Update the relevant file under `docs/` when behaviour or contracts change.
