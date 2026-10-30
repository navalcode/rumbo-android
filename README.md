# Rumbo

[Español](README.es.md) · [Build and tests](https://github.com/navalcode/rumbo-android/actions/workflows/checks.yml)

An Android app for browsing six fictional trips, saving favorites and keeping demo bookings on the device. When a refresh fails, the saved catalog remains available.

The implementation uses Kotlin, Compose, Navigation, Room and Retrofit, with domain logic in a separate core module. Places and prices are invented, and a demo booking does not reserve a real trip. No account, API key or backend is needed; the app has no Internet permission.

<p>
<img src="docs/images/explore.png" alt="Rumbo catalog running in the Android emulator" width="240">
<img src="docs/images/offline.png" alt="Rumbo keeps the saved catalog visible after a simulated connection failure" width="240">
</p>

## Try it

1. Search by trip title or region, with or without accents, and filter by category.
2. Open a trip. The heart saves a favorite independently of opening the detail.
3. Select **Add demo booking**. Find the booking and your favorites in **My plan**. Repeating a booking keeps its original reference.
4. In **Lab**, select Offline, Service error or Slow response, then return to Explore. A failed update preserves the saved catalog.
5. Choose **Successful load** in Lab to recover. Restart the process to check saved favorites and bookings.

The scenarios change local responses, not your device’s connection. To test a first load without saved data, use a fresh demo installation and start with the documented scenario argument. Replace `DEVICE_ID` with your device’s serial:

```sh
adb -s DEVICE_ID shell am start -n dev.navalcode.rumbo/.MainActivity --es demo_scenario OFFLINE
```

Values: `CONNECTED`, `OFFLINE`, `SERVICE_ERROR`, `SLOW`. Only this demo’s source is affected.

## Build and run

Use **JDK 17**, **Android SDK 36**, and an emulator or device with **API 26+**. Open the project in Android Studio or set the SDK path in an untracked `local.properties` file:

```properties
sdk.dir=/path/to/Android/sdk
```

From this directory:

```sh
./gradlew :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
./gradlew :app:installDebug
./gradlew :app:connectedDebugAndroidTest
./gradlew :app:assembleRelease
```

Connected tests need an emulator or device. For an explicit serial and optional screenshots, see [verification commands](docs/VERIFICATION.md). Use `:app:bundleRelease` to generate an AAB. Release enables R8 and resource shrinking; production signing is not configured.

Gradle 8.14.3 is pinned with SHA-256; AGP 8.13.2, Kotlin 2.2.10 and library versions are fixed. Dependencies come from Google Maven and Maven Central.

## Read the implementation

- [Trips.kt](core/src/main/kotlin/dev/navalcode/rumbo/core/Trips.kt): source/store contracts, refresh deadline, cancellation, cached content and idempotent bookings, without Android dependencies.
- [TripCatalogApi.kt](app/src/main/java/dev/navalcode/rumbo/data/TripCatalogApi.kt): Retrofit DTO validation and mapping of HTTP/I/O failures.
- [TripDatabase.kt](app/src/main/java/dev/navalcode/rumbo/data/TripDatabase.kt): transactional catalog replacement and unique bookings in Room.
- [TripsViewModel.kt](app/src/main/java/dev/navalcode/rumbo/ui/TripsViewModel.kt): read-only flows, input methods and cancellation of a superseded refresh.
- [RumboApp.kt](app/src/main/java/dev/navalcode/rumbo/ui/RumboApp.kt): Compose, Navigation destinations, stable detail IDs and local actions.
- [Landscape.kt](app/src/main/java/dev/navalcode/rumbo/ui/Landscape.kt): original Canvas illustrations.

[Architecture](docs/ARCHITECTURE.md) explains the small manual dependency graph, alternatives and restoration limits. [DESIGN.md](DESIGN.md) describes the interface and locally bundled fonts.

## REST without a backend

Retrofit declares `GET /v1/trips` at `https://demo.rumbo.invalid/`. An OkHttp application interceptor answers every request locally without forwarding it. The scenario selects a response, I/O failure, HTTP 503 or cancellable delay. The repository applies a 3.5-second deadline and preserves the previous catalog and timestamp on expected failures.

MockWebServer tests use real loopback HTTP to check the contract and cancellation. This is a simulated travel source. Query/category state survives activity recreation through the ViewModel, but is not restored after process death; Room data persists independently.

## Checks and limits

The project has **32 JVM tests** (16 core, 10 HTTP and six ViewModel) and **eight Android tests** for Room, booking, cached content, Back, independent favorites and activity recreation. [Verification](docs/VERIFICATION.md) records the current runs, screenshots, artifact audits and any failed attempts; follow the workflow link above for hosted results.

Resources are Spanish with an English fallback. The app is a learning example: no real availability, payments, production signing or store distribution. Emulator checks and calculated layout checks do not establish physical-device behavior or formal accessibility certification.

## License

Original code, documentation and artwork: [MIT](LICENSE), Manuel Naval, 2026. [Third-party notices](THIRD_PARTY.md) retain the Gradle and dependency notices.
