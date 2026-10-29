# Third-party components

The MIT license at the repository root covers the original Rumbo source, documentation and Canvas illustrations. Dependencies retain their own licenses.

The checked-in Gradle wrapper scripts and JAR come from Gradle 8.14.3 and are licensed under Apache-2.0. The Gradle distribution's [LICENSE](gradle/licenses/gradle-LICENSE.txt) and [NOTICE](gradle/licenses/gradle-NOTICE.txt) are preserved. The wrapper verifies the distribution SHA-256.

The application uses Kotlin, Kotlin Coroutines, AndroidX (including Compose, Room, Lifecycle and Navigation), Retrofit, OkHttp and Gson. These components and MockWebServer are distributed under Apache-2.0. Tests also use JUnit 4, distributed under Eclipse Public License 1.0. Upstream artifacts and source repositories contain the applicable copyright and notice files; transitive dependencies retain their own licenses. Dependency versions are explicit in the Gradle files. No proprietary SDK, remote service credentials, external photography or brand assets are bundled.

## Embedded typefaces

Outfit (copyright 2021 The Outfit Project Authors) and Fraunces (copyright 2018 The Fraunces Project Authors) are distributed under SIL Open Font License 1.1. Static instances were produced from the official Google Fonts variable sources with fontTools. Full notices are packaged in `app/src/main/assets/licenses/`; font metadata retains the upstream copyright and license. See DESIGN.md for their visual roles.

Typeface sources: [Outfit](https://github.com/google/fonts/tree/main/ofl/outfit) and [Fraunces](https://github.com/google/fonts/tree/main/ofl/fraunces).
