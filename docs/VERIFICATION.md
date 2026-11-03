# Verification record

## Gradle patch and cache ownership · 3 November 2026

Both wrappers now use Gradle 8.14.6. The distribution and wrapper JAR were checked against official SHA-256 values, and the wrapper was generated from that verified distribution. The patch includes fixes described in the [Gradle release notes](https://docs.gradle.org/8.14.6/release-notes.html). JDK 17, SDK 36, plugin/dependency versions and application source are unchanged.

The device job reads the basic Gradle cache; only the build/checks job may save it on the default branch. This avoids competing writes to the same key. No caches were deleted. The Gradle action still reports Gradle 8 as behind Gradle 9 because its support classifier compares major versions. That notice is retained; a major upgrade needs a separate Kotlin/AGP/Flutter compatibility review.

Actionlint 1.7.12 passes with a local label allowlist for the official `ubuntu-26.04` image, absent from that linter's built-in list. The initial unknown-label diagnostics were retained. ShellCheck was unavailable; shell commands are unchanged. Rumbo pins the Ubuntu 26.04 image observed in its preceding run; Flutter retains Ubuntu 24.04. Both labels are listed in [runner-images](https://github.com/actions/runner-images).

Local validation executed 114 Gradle tasks: **35 JVM tests**, lint with **zero errors / 17 warnings**, debug APK, unsigned optimized release APK and release AAB. The Kotlin compatibility table lists a lower fully-tested AGP maximum than this project's inherited Kotlin 2.2.10 / AGP 8.13.2 pairing; these project results are evidence for this combination, not a blanket upstream compatibility guarantee. See the [Kotlin matrix](https://kotlinlang.org/docs/gradle-configure-project.html).

Advisory audits report **58.35 MB DEX** in debug and **2.48 MB** in optimized APK/AAB. Both AAB R8 options and stats were read directly from the ZIP: the complements of no-shrinking / no-optimization / no-obfuscation match **98.15% / 97.18% / 98.08%** in the tool report.

All six local packages pass the advisory ELF/ZIP 16 KB checks. APKs lack percentage R8 metadata; the AAB ZIP-alignment result has no split entries and does not measure Play-generated APKs. The percentages above are metadata-derived, not runtime performance, whole-app reduction or store certification. No new local device or release-runtime test is attributed to this patch. Earlier device results below retain their historical scope.

Local artifact SHA-256:

- rumbo-debug: `926ac2ade3f8dab27b47dd4e584fd903086addca4288edaeacd730246fd2d47b`.
- rumbo-release: `f3b3dd2588cad9d7811e66204fa208cf0467c404f61a6a4f5f8374751cec4f1f`.
- rumbo-bundle: `3d89cbf582d3efd0fc0d3e257fda63776599efa0f4d069396cad4f35a543a05e`.

Hosted verification for this patch is pending; the previous CI action-maintenance runs below do not validate these new wrapper bytes.

## CI action runtime maintenance · 3 November 2026

The workflow pins maintained releases of checkout, Java setup, artifact upload, Gradle setup and Android setup by full commit SHA. Their JavaScript runtime is Node 24; this does not change the app's JDK 17, SDK 36, dependency versions or test commands. The emulator action keeps its existing reviewed commit.

`setup-gradle` uses `cache-provider: basic`, retaining GitHub Actions caching instead of selecting the new enhanced provider. `setup-android` explicitly retains command-line tools archive `12266719`, which was the previous action's implicit default. Events, read-only contents permission, disabled credential persistence, runners, timeouts and artifact retention are unchanged.

Local validation: actionlint 1.7.12 and a byte-for-byte comparison after reversing only the reviewed pins and explicit cache/SDK inputs. ShellCheck was unavailable; existing shell commands are unchanged. Hosted verification: [run 41609164693](https://github.com/navalcode/rumbo-android/actions/runs/41609164693) succeeded at `8eeb987` in both jobs (Android build/bundle and API 35 emulator). Logs contain the reviewed action pins and no Node.js 20/setup-java v4 deprecation warning. Earlier app/runtime records below retain their original scope.

Remaining annotations concern Gradle 8.14.3 maintenance, the existing Ubuntu label migration (Android only), and a basic-cache save reservation race between jobs. Both jobs succeeded; a cache save warning does not change their test/build result. Gradle compatibility and cache ownership require a separate reviewed maintenance increment. The workflow uploads reports/diagnostics rather than APK/AAB files; earlier local artifact audits below are not audits of these cloud outputs.

## Filter restoration · 3 November 2026

Search text and category now use Android saved state. The ViewModel factory obtains the handle from the activity's CreationExtras. No domain, Room schema, fixture data, navigation graph, visual layout or network contract changes. Lifecycle saved-state is explicitly declared at the same pinned 2.9.2 version as the existing lifecycle dependencies.

- **35 JVM tests pass**: 16 core, 10 HTTP and nine ViewModel. The three new cases cover restored filters before catalog loading, saving/clearing values, and an unknown saved category falling back to All. A fresh all-task run executed 87 Gradle tasks; a subsequent final build included the explicit dependency and new native test.
- Lint passes with zero errors and **18 warnings** about target/tool/library updates, including the newly explicit lifecycle artifact. Debug APK, test APK and unsigned optimized release build successfully with JDK 17.0.14 / SDK 36.
- **Nine native tests pass** on the dedicated API 37 emulator at 360 × 800, Spanish / font scale 1.0: three Room cases and six Compose journeys, including a new search/category activity-recreation check. The installed normal debug APK hash was compared with the built artifact before running the suite.
- Separate normal-entry-point process-restoration check passes twice on the normal debug APK. The activity reached STOPPED, `am kill` removed the background process, `pidof` confirmed its absence, and launcher return created a different PID. First return restored `Alba`, Mountain and one matching trip; after clearing the query and selecting All, the second return kept both filters cleared. The same two cycles also pass on the optimized release APK, signed with the standard local Android debug key solely for installation. Its installed hash was checked before the run. This checks the restored filter flow through R8, not every release behavior. Activity recreation above is not used as proof of process-death restoration.

Saved UI state belongs to a retained task. Force-stop, dismissing the task, rebooting and opening a fresh task are outside the guarantee; Room data persists separately. The Lab scenario stays process-local. Current Android captures confirm the existing Spanish layout; earlier English/enlarged-text rows below remain historical checks, not fresh results.

The first software-rendered emulator reported a SurfaceFlinger driver stall and installation timed out without an acknowledgement. Package queries and rendering diagnostics were inspected before stopping only the dedicated emulator. A host-rendered cold start then installed both APKs successfully; the full nine-test suite passed. This is an environment recovery, not proof of a universal emulator fix. The first external process-check harness read selection from the text child instead of the chip's checked semantics; another expected `mState` where API 37 prints `state`. Those attempts are retained as harness failures, not application failures or passed process checks.

Advisory APK audits: debug **58.35 MB DEX**, release **2.48 MB DEX**, native ELF and APK ZIP 16 KB alignment checks pass. Percentage R8 metrics are absent; the tool's overall pass does not establish a shrinking/optimization/obfuscation percentage, current store policy or acceptance. No physical-device, store, native iOS or formal accessibility result is claimed.

Current artifact SHA-256:

- Normal debug: `926ac2ade3f8dab27b47dd4e584fd903086addca4288edaeacd730246fd2d47b`.
- Unsigned optimized release: `0b6c6536a5684955f06c1d256bbe525fd9a38f960ead67c1e2653438bd59287f`.
- Same optimized release, locally test-signed: `b66a73342e55b0d29c8faefb46b465d07b3ff76418bd0fa4e4c54e85d4e724df`.

No production signing configuration or downloadable release is added.

### Reproduce retained-task restoration

Install the normal debug APK. In Explore, enter `Alba` and select Mountain. Press Home, then confirm the activity has reached STOPPED with `adb -s DEVICE_ID shell dumpsys activity activities`. Record `adb -s DEVICE_ID shell pidof dev.navalcode.rumbo`, then:

```sh
adb -s DEVICE_ID shell am kill dev.navalcode.rumbo
adb -s DEVICE_ID shell pidof dev.navalcode.rumbo
adb -s DEVICE_ID shell am start -W -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -f 0x10200000 -n dev.navalcode.rumbo/.MainActivity
```

The middle command must show no process before returning; check that the new PID differs. The retained task must show `Alba`, Mountain selected and the matching catalog result. Clear the query, select All and repeat; old filters must not return. Do not use force-stop or clear app data for this check. Local evidence records the actual PIDs, APK hash and screenshots; these external checks are separate from the hosted instrumentation workflow.

## Earlier editorial verification · 30 October 2026

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
