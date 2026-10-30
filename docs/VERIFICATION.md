# Verification record

30 October 2026. This record applies to the editorial revision: concrete catalog/booking actions, separate unavailable-detail copy, shorter loading text and explicit simulated failures. The domain, REST adapter, Room schema, dependencies and lock/version configuration did not change.

## Local checks

- JDK 17.0.14, Android SDK 36. A fresh run executed all **32 JVM tests**: 16 core, 10 HTTP and six ViewModel. After the last English resource correction, the 16 app tests, lint and both APK builds passed again. Lint: zero errors and 17 upgrade/target warnings.
- Debug and unsigned optimized release APKs built successfully. Android test APK also built after the screenshot helper changed.
- Google API 37 emulator, 360 × 800 logical pixels: the full suite has **eight tests** (three Room and five Compose journeys). Final language/text-scale results are listed below. These are local instrumented tests, separate from the hosted API 35 workflow.
- UI journeys cover booking, cached catalog after a simulated failure and recovery to successful loading, independent favorites, system Back and activity recreation. They scroll to virtualized items before checking them. Captures come from the rendered Compose root, without editing pixels; system chrome is excluded.

| Language | Font scale | Result |
|---|---|---|
| English | 1.0 | 8/8, including Room |
| Spanish | 1.0 | 5/5 UI journeys |
| English | 1.5 | 5/5 UI journeys on the same final application APK |
| Spanish | 1.5 | 5/5 UI journeys |

Normal English/Spanish captures are in `images/`; enlarged-text and Lab/plan/recovery frames are retained with the review evidence. A screenshot records a viewport, not every scroll position.

## Reproduce

Run from the project directory. Replace `DEVICE_ID` with an emulator/device serial:

```sh
./gradlew :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease
ANDROID_SERIAL=DEVICE_ID ./gradlew :app:installDebug :app:installDebugAndroidTest
adb -s DEVICE_ID shell am instrument -w -r dev.navalcode.rumbo.test/androidx.test.runner.AndroidJUnitRunner
```

Optional screenshots: add `-e captureLabel en-review` before the runner argument. A lowercase alphanumeric/hyphen label of 1–32 characters is required. The test writes explore, detail, plan, lab, offline and recovered PNGs to the demo's external files directory:

```sh
adb -s DEVICE_ID pull /sdcard/Android/data/dev.navalcode.rumbo/files/editorial-captures ./captures
```

The five UI tests can be selected with `-e class dev.navalcode.rumbo.ui.RumboJourneyTest`. Use a disposable test emulator for locale/font changes; restore its settings after testing. Do not run against another person's active emulator by accident.

## Unsuccessful attempts and limits

Earlier attempts in this revision are retained as failures: one enlarged-text startup ended before JUnit with an ANR while loading DEX; a second run reported activity teardown timeouts. A later Spanish run exceeded the 360-second command limit and included a favorite-state wait timeout. The test runner was not changed to skip or weaken those assertions. Restarting the dedicated emulator allowed subsequent runs to finish; that does not establish the root cause of every timeout. An attempted package compile returned success but package diagnostics reported `verify`, so no AOT speed-up is claimed.

One UI-automation screenshot caught the previous destination during a transition. It was rejected as detail evidence. The optional helper now uses Compose's synchronized root capture, and replacement captures are inspected before publication.

The release audit measured **2.48 MB DEX**, with native ELF and APK ZIP 16 KB alignment checks passing. The debug artifact measured **58.35 MB DEX**. Both audits lack R8 percentage metadata; their advisory result does not verify a shrinking percentage, current Play policy or store acceptance. Release shrinking is enabled in source, but no production signing or store distribution is configured.

No physical-device run, formal accessibility certification or native iOS result is claimed. Activity recreation does not prove every process-death restoration path. Earlier optimized-APK smoke checks in project history do not validate this revision's unsigned release runtime.

## Artifact identity

Unsigned release APK SHA-256: `0d4251ecfed34b9087480eeda56a81f015bb5fc2c1e3c15fccafb93eb7982507`.
