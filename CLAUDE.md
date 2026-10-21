# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Debug APK
./gradlew assembleDebug

# Release APK (requires keystore.properties)
./gradlew assembleRelease

# Beta APK (signed release, no minification)
./gradlew assembleBeta

# Run unit tests
./gradlew test

# Run a single test class
./gradlew :data:testDebugUnitTest --tests "com.streamvault.data.parser.M3uParserTest"

# Code coverage report (generates build/reports/kover/)
./gradlew koverXmlReportCi koverHtmlReportCi

# Install on connected device
./gradlew installDebug

# Force re-seed dev provider (clears app data)
adb shell pm clear com.streamvault.app
```

## Dev Seeding (skip onboarding on first boot)

```bash
cp local.properties.example local.properties
```

Then uncomment one section in `local.properties`:
- **Option A**: Xtream credentials (`xtream.dev.server`, `xtream.dev.username`, `xtream.dev.password`, `xtream.dev.name`)
- **Option B**: M3U URL (`m3u.dev.url`, `m3u.dev.name`)

These values are injected into `BuildConfig` only for `debug` builds; release APKs always get empty strings.

## Module Architecture

The project is split into four Gradle modules following clean architecture:

```
domain/   — models, repository interfaces, use cases, manager interfaces
            No Android dependencies. The source of truth for data shapes.

data/     — Room DB, DAOs, entities, repository implementations, sync workers,
            provider implementations (XtreamProvider, StalkerProvider, M3uParser),
            EPG parsing (XmltvParser), credential encryption (AndroidKeystore)

player/   — PlayerEngine interface + Media3/ExoPlayer implementation.
            UI talks only to PlayerEngine, never to ExoPlayer directly.
            Bundles player/libs/media3-decoder-ffmpeg-1.9.2.aar for audio
            fallback (AC-3, E-AC-3, DTS, MP2, TrueHD).

app/      — Compose UI screens, ViewModels, Hilt DI wiring, navigation,
            plugin IPC client, Cast, TV input service, WorkManager workers.
```

**Dependency direction**: `app` → `data` + `player` + `domain`; `data` → `domain`; `player` → `domain`.

## Key Architectural Patterns

### Adding a new IPTV provider

Implement `domain/src/main/java/com/streamvault/domain/provider/IptvProvider.kt`. This interface normalises all provider types (Xtream, Stalker, M3U) into the same domain models (`Channel`, `Movie`, `Series`, `Program`, etc.). Register the implementation via Hilt in `app/src/main/java/com/streamvault/app/di/`.

### Repository pattern

`domain/` declares repository interfaces (`ChannelRepository`, `EpgRepository`, etc.). `data/` provides Room-backed implementations. All bindings live in `app/src/main/java/com/streamvault/app/di/RepositoryModule.kt`.

### Navigation

Single-activity app. All routes are declared in `app/src/main/java/com/streamvault/app/navigation/AppNavigation.kt`. Playback is initiated by constructing a `PlayerNavigationRequest` and navigating to the player route.

### Sync pipeline

`data/sync/SyncManager.kt` orchestrates catalog fetches. It delegates to strategy classes (`SyncManagerXtreamFetcher`, `SyncManagerM3uImporter`, etc.) and persists via `SyncCatalogStore`. Background sync runs via `ProviderSyncWorker` (WorkManager).

### Plugin system

Plugins are separate Android APKs that expose a `com.streamvault.plugin.API` service. StreamVault discovers them via `PackageManager` and communicates through Android `Messenger` IPC. The host-side client is `app/src/main/java/com/streamvault/app/plugins/PluginMessengerClient.kt`. See `docs/PLUGIN_API.md` for the full message protocol and capabilities (`provider.m3u`, `playback.prepare`, `cast.rewriteUrl`, `configuration.schema`, `configuration.activity`).

### UI / Design system

TV-first Jetpack Compose with `androidx.tv` material. Design tokens are in `app/src/main/java/com/streamvault/app/ui/design/` (`AppColors`, `AppTypography`, `AppSpacing`, `AppShapes`, `AppMotion`). Every interactive element must be D-pad focusable; focus helpers live in `FocusHelpers.kt` / `FocusSpec.kt`.

## Build Types

| Type | minify | signing | `BuildConfig.APP_UPDATE_CHANNEL` |
|------|--------|---------|----------------------------------|
| `debug` | no | debug key | — |
| `beta` | no | release key | `"beta"` |
| `release` | yes + shrink | release key | `"stable"` |

Release signing requires `keystore.properties` at the repo root (not committed). See `local.properties.example` for the expected key names.

## FFmpeg AAR

`player/libs/media3-decoder-ffmpeg-1.9.2.aar` is a pre-built local artifact (not from Maven). To update it: rebuild against Media3 1.9.2, replace the AAR, and run `:player:verifyLocalFfmpegArtifact`.
