# Rumbo

[English](README.md) · [![CI](https://github.com/navalcode/rumbo-android/actions/workflows/checks.yml/badge.svg)](https://github.com/navalcode/rumbo-android/actions/workflows/checks.yml)

Aplicación Android original para explorar escapadas ficticias y observar caché, adaptación REST, estado de presentación de solo lectura, cancelación, navegación y persistencia local.

Catálogo, regiones, precios y reservas son inventados. No requiere servicios de producción, cuentas ni API keys. No solicita permisos ni recoge datos personales. Los favoritos y reservas de demostración permanecen en el dispositivo.

| Explorar | Consultar sin conexión |
|---|---|
| ![Catálogo de Rumbo](docs/images/explore.png) | ![Catálogo guardado cuando falla la fuente](docs/images/offline.png) |

## Qué puedes probar

1. Explora seis escapadas y busca por nombre o región, con o sin acentos.
2. Filtra por montaña, costa o bosque.
3. Abre el detalle, guarda un favorito y crea una reserva local ficticia.
4. Consulta favoritos y reservas en Mi plan.
5. En Laboratorio, selecciona Sin conexión, Error de servicio o Respuesta lenta.
6. Vuelve al catálogo: un fallo de actualización conserva lo guardado. Selecciona Conectado para recuperar.
7. Reinicia para comprobar la persistencia. Atrás vuelve del detalle a su destino anterior.

Sin conexión modifica el transporte simulado, no la conectividad del dispositivo. Para comprobar un primer arranque sin caché, usa una instalación limpia:

```sh
adb shell am start -n dev.navalcode.rumbo/.MainActivity --es demo_scenario OFFLINE
```

Valores: `CONNECTED`, `OFFLINE`, `SERVICE_ERROR`, `SLOW`. El parámetro configura únicamente la fuente ficticia original.

## Diseño de interfaz

[DESIGN.md](DESIGN.md) registra las decisiones visuales. La cabecera compacta lleva a búsqueda etiquetada, filtros y destinos; el favorito es independiente de abrir detalle. Outfit sirve a lectura/controles y Fraunces a títulos de destino, con avisos OFL empaquetados. Los avisos de recuperación se anuncian como live region. Las capturas muestran la app rediseñada en ejecución, no conceptos generados.

## Ejecutar

Requisitos: JDK 17, Android SDK 36 y dispositivo/emulador con API 26 o superior. Abre esta carpeta en Android Studio o configura el SDK mediante `local.properties` sin versionar:

```properties
sdk.dir=/ruta/al/Android/sdk
```

Desde esta carpeta:

```sh
./gradlew :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
./gradlew :app:installDebug
./gradlew :app:connectedDebugAndroidTest
./gradlew :app:bundleRelease
```

Las pruebas conectadas necesitan un dispositivo. Gradle 8.14.3 está fijado con SHA-256; AGP 8.13.2, Kotlin 2.2.10 y las bibliotecas tienen versiones explícitas. Las dependencias vienen de Google Maven y Maven Central. Release habilita R8 y reducción de recursos; no hay firma de producción configurada.

## Por dónde leer el código

| Límite | Código y comportamiento |
|---|---|
| Dominio y coordinación | [`Trips.kt`](core/src/main/kotlin/dev/navalcode/rumbo/core/Trips.kt): contratos, deadline, cancelación, caché y reserva idempotente. Sin dependencia de Android. |
| Adaptador REST | [`TripCatalogApi.kt`](app/src/main/java/dev/navalcode/rumbo/data/TripCatalogApi.kt): Retrofit, validación DTO, fallos HTTP/I/O y transporte de demostración. |
| Persistencia | [`TripDatabase.kt`](app/src/main/java/dev/navalcode/rumbo/data/TripDatabase.kt): Room, reemplazo transaccional y unicidad de reservas. |
| Presentación | [`TripsViewModel.kt`](app/src/main/java/dev/navalcode/rumbo/ui/TripsViewModel.kt): flows de solo lectura, métodos de entrada, estado combinado y sustitución de cargas. |
| Interfaz | [`RumboApp.kt`](app/src/main/java/dev/navalcode/rumbo/ui/RumboApp.kt): Compose, destinos Navigation, detalle por ID y acciones locales. |
| Ilustraciones propias | [`Landscape.kt`](app/src/main/java/dev/navalcode/rumbo/ui/Landscape.kt): paisajes en Canvas. |

La inyección es manual porque el grafo es pequeño. [Arquitectura y decisiones](docs/ARCHITECTURE.md), en inglés, detalla responsabilidades, alternativas, restauración y límites de validación.

## REST sin backend

Retrofit declara `GET /v1/trips` sobre `https://demo.rumbo.invalid/`. Un interceptor de aplicación OkHttp responde todas las peticiones sin reenviarlas. Una cabecera de demostración selecciona éxito, fallo I/O o HTTP 503; el modo lento añade una espera cancelable. La app no tiene permiso de Internet ni abre conexiones de red.

Pruebas independientes con MockWebServer ejercitan HTTP real en loopback: ruta, cabecera, serialización, respuestas fallidas y cancelación. Es un cliente contra un contrato ficticio original; no una integración con una API de viajes de producción.

El repositorio aplica un límite de 3,5 segundos a la fuente. Un fallo conserva catálogo y fecha anteriores. Una nueva actualización cancela la previa. Repetir una reserva conserva referencia y fecha originales. Búsqueda y filtros utilizan la caché. Navigation y Room conservan rutas y datos; consulta/categoría no se restauran tras morir el proceso.

## Validación y límites

Comprobado localmente el 29 de octubre de 2026:

- **32 pruebas JVM:** 16 de core, 10 del adaptador HTTP y seis de ViewModel.
- **Ocho pruebas en emulador API 37:** tres de Room y cinco recorridos Compose cubren reserva, caché sin conexión, Atrás, restauración de ruta y un favorito independiente que persiste al recrear la actividad. La suite rediseñada pasa a 360 × 800 píxeles lógicos; las siete previas también pasaron a 1080 × 1920 / 440 dpi.
- **Lint:** sin errores; quedan avisos de actualización de dependencias y target.
- **Release:** APK rediseñada con R8 y reducción de recursos. El AAB y el arranque release con firma de prueba/caché vacía se verificaron antes de esta revisión visual. No hay firma de producción configurada.
- **Auditoría consultiva:** 2,48 MB DEX y alineación nativa/ZIP de 16 KB. La herramienta avisa de la ausencia de metadatos R8; no mide porcentajes de reducción/optimización/ofuscación desde este APK. No equivale a aprobación de tienda.

El workflow GitHub Actions compila, prueba y ejecuta lint, con un segundo job de emulador API 35. El enlace superior muestra los resultados actuales; tener un workflow en Git no acredita una ejecución correcta.

La interfaz tiene recursos españoles e ingleses. La revisión nativa actual cubre ambos idiomas a 360 × 800 y texto inglés a escala 1,5, incluido detalle desplazable. No se afirma validación física, certificación formal de accesibilidad, disponibilidad real, pagos o distribución en tienda.

## Licencia

Código, documentación e ilustraciones originales: [MIT](LICENSE), copyright 2026 Manuel Naval. [Avisos de terceros](THIRD_PARTY.md) conserva los del wrapper Gradle y las dependencias.
