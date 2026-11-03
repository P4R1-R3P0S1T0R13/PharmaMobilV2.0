# PharmaMobil

Aplicación de gestión farmacéutica (productos, clientes, pedidos e inventario) construida con
**Kotlin Multiplatform** y **Compose Multiplatform** para Android e iOS.

Curso: Desarrollo de Aplicaciones Móviles · UPeU · Ciclo VI · 2026-2
Autor: Miguel Pari

## Sesión 1 · Entorno, proyecto base y Git

| Herramienta | Versión |
|---|---|
| Android Studio | con Android SDK 36 |
| JDK | 21 (toolchain del daemon de Gradle) |
| Kotlin | 2.4.10 |
| Android Gradle Plugin | 9.0.1 |
| Compose Multiplatform | 1.11.1 |
| Gradle | 9.1.0 (wrapper) |

### Source sets
| Source set | Rol |
|---|---|
| `shared/src/commonMain` | Lógica e interfaz compartidas (modelos, reglas, pantallas Compose) |
| `shared/src/androidMain` | Implementaciones exclusivas de Android |
| `shared/src/iosMain` | Implementaciones exclusivas de iOS |
| `shared/src/commonTest` | Pruebas unitarias del código común |
| `androidApp` | Punto de entrada Android (`MainActivity`) |
| `iosApp` | Proyecto Xcode que embebe el framework `Shared` |

### Comandos
```bash
./gradlew :androidApp:assembleDebug     # compila la app Android
./gradlew :shared:testAndroidHostTest   # pruebas de commonTest en la JVM
```
