# PharmaMobil - Aplicación Kotlin Multiplatform (KMP)

Proyecto desarrollado para la asignatura **Desarrollo de Aplicaciones Móviles** (Sesión 09 - Unidad 2).

## 🚀 Código específico de plataforma (Capacidades Nativas)

En la Sesión 09 se implementaron las siguientes capacidades nativas utilizando el mecanismo `expect`/`actual` e inyección de dependencias con Koin:

| Capacidad | Firma `expect` / Interface | API usada en Android (`androidMain`) | API usada en iOS (`iosMain`) | Ubicación |
| :--- | :--- | :--- | :--- | :--- |
| **Formato de moneda** | `expect fun formatearSoles(valor: Double): String` | `NumberFormat` con `Locale("es", "PE")` | `NSNumberFormatter` con `NSLocale("es_PE")` | `platform/Formato.kt` |
| **Compartir producto** | `interface Compartidor` | `Intent.ACTION_SEND` (`FLAG_ACTIVITY_NEW_TASK`) | `UIActivityViewController` | `domain/platform/Compartidor.kt` |
| **Información del dispositivo** | `expect class InfoDispositivo()` | `Build.VERSION.RELEASE`, `Build.MODEL` | `UIDevice.currentDevice.systemName/Version/model` | `platform/InfoDispositivo.kt` |
| **Módulo de inyección Koin** | `expect val platformModule: Module` | `module { single<Compartidor> { CompartidorAndroid(androidContext()) } }` | `module { single<Compartidor> { CompartidorIos() } }` | `di/PlatformModule.kt` |

---

## 🛠️ Estructura del Proyecto

* **`/androidApp`**: Aplicación Android nativa con Compose.
* **`/iosApp`**: Aplicación iOS nativa (SwiftUI) para la integración KMP.
* **`/shared`**: Módulo compartido Compose Multiplatform.
  * **`commonMain`**: Código común (Lógica de dominio, Casos de uso, ViewModels, UI Compose común).
  * **`androidMain`**: Implementaciones `actual` específicas de Android.
  * **`iosMain`**: Implementaciones `actual` específicas de iOS.

---

## 🏃 Ejecución del Proyecto

* **App Android:** `./gradlew :androidApp:assembleDebug`
* **Pruebas unitarias:** `./gradlew test`
* **App iOS:** Abrir la carpeta `/iosApp` en Xcode y ejecutar.

---
Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…
