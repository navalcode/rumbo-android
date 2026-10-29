# Rumbo

[Español](README.es.md) · [Android checks](https://github.com/navalcode/rumbo-android/actions/workflows/checks.yml)

An original Android app for exploring fictional trips and inspecting mobile engineering behavior: cached data, REST adaptation, immutable presentation state, cancellation, navigation and local persistence.

All catalog entries, regions, prices and bookings are fictional. No production service, account or API key is needed. The app requests no permissions and collects no personal data. Favorites and demo bookings stay on the device.

| Explore | Keep reading cached content |
|---|---|
| ![Rumbo catalog](docs/images/explore.png) | ![Cached catalog during a source failure](docs/images/offline.png) |

## Try the app

1. Explore six trips. Search by title or region, with or without accents.
2. Filter by mountain, coast or forest.
3. Open a detail, save a favorite and make a local demo booking.
4. Find saved trips and bookings in My plan.
5. In Lab, select Offline, Service error or Slow response.
6. Return to the catalog: failed refreshes preserve cached content. Restore Connected to recover.
7. Restart the app to inspect persisted favorites and bookings. System Back returns from a detail to its previous destination.

Offline mode changes the fictional transport, not the device's connectivity. To test a first load without a cache, use a clean installation and start with:

```sh
adb shell am start -n dev.navalcode.rumbo/.MainActivity --es demo_scenario OFFLINE
```

Allowed values: `CONNECTED`, `OFFLINE`, `SERVICE_ERROR`, `SLOW`. This argument only configures the original demo fixture.

## Build and run

Requires JDK 17, Android SDK 36 and a device/emulator with API 26 or newer. Open this directory in Android Studio, or configure the SDK in an untracked `local.properties`:

```properties
sdk.dir=/path/to/Android/sdk
```

From this directory:

```sh
./gradlew :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
./gradlew :app:installDebug
./gradlew :app:connectedDebugAndroidTest
./gradlew :app:bundleRelease
```

Connected tests require a device. Gradle 8.14.3 is pinned with a distribution SHA-256; AGP 8.13.2, Kotlin 2.2.10 and library versions are explicit. Google Maven and Maven Central supply dependencies. Release has R8 and resource shrinking enabled; production signing is intentionally not configured.

## Read the code

| Boundary | Files and behavior |
|---|---|
| Domain and orchestration | [`Trips.kt`](core/src/main/kotlin/dev/navalcode/rumbo/core/Trips.kt): source/store contracts, refresh deadline, cancellation, cache and booking idempotence. No Android dependency. |
| REST adapter | [`TripCatalogApi.kt`](app/src/main/java/dev/navalcode/rumbo/data/TripCatalogApi.kt): Retrofit, DTO validation, HTTP/I/O failure mapping and intercepted fixture responses. |
| Persistence | [`TripDatabase.kt`](app/src/main/java/dev/navalcode/rumbo/data/TripDatabase.kt): Room, transactional replacement and booking uniqueness. |
| Presentation | [`TripsViewModel.kt`](app/src/main/java/dev/navalcode/rumbo/ui/TripsViewModel.kt): read-only flows, user-input methods, combined state and replacement cancellation. |
| Interface | [`RumboApp.kt`](app/src/main/java/dev/navalcode/rumbo/ui/RumboApp.kt): Compose, Navigation destinations, ID-based detail and local actions. |
| Original artwork | [`Landscape.kt`](app/src/main/java/dev/navalcode/rumbo/ui/Landscape.kt): Canvas illustrations. |

The app uses manual injection because its graph is small. [Architecture and decisions](docs/ARCHITECTURE.md) explain alternatives, ownership, restoration and validation limits.

## How REST works without a backend

Retrofit declares `GET /v1/trips` against `https://demo.rumbo.invalid/`. An OkHttp application interceptor answers every request locally and never forwards it. A demo header selects success, offline I/O or HTTP 503; slow mode adds a cancellable delay. The app has no Internet permission and opens no network connection.

Separate MockWebServer tests exercise real loopback HTTP, serialization, path/header behavior, failed responses and cancellation. This demonstrates a client against an original fictional contract. It does not claim integration with a production travel API.

The repository applies a 3.5-second source deadline. A failure preserves the previous catalog and timestamp. A replacement refresh cancels the earlier one. Repeated local bookings preserve their reference and original date. Query/filter changes use cached data. Navigation and Room own route and data persistence; query/category are not restored after process death.

## Verification and limits

Validated locally on 29 October 2026:

- **32 JVM tests:** 16 core, 10 HTTP adapter and six ViewModel tests.
- **Seven API 37 emulator tests:** Room persistence and Compose journeys, including reservation, cached offline content, system Back and detail restoration after activity recreation. These seven checks also passed at a smaller 1080 × 1920 viewport with 420 dpi.
- **Lint:** no errors; dependency/target upgrade notices remain.
- **Release:** APK and AAB built with R8; an optimized APK signed with a local test key loaded the catalog after clearing the demo cache.
- **Advisory artifact audit:** 2.45 MB DEX and aligned native libraries at 16 KB, without tool warnings. This is not store approval.

The GitHub Actions workflow builds/tests/lints the app and has a separate API 35 emulator job. Follow the workflow link above for current results; a checked-in workflow alone is not a passing run.

UI resources are Spanish and English. Earlier visual checks covered both languages and enlarged text. No physical-device validation, formal accessibility certification, real availability, payments or store distribution is claimed.

## License

Original code, documentation and artwork: [MIT](LICENSE), copyright 2026 Manuel Naval. See [third-party notices](THIRD_PARTY.md) for the Gradle wrapper and dependencies.
