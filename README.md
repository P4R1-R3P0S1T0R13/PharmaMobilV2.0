# PharmaMobil

Aplicación de gestión farmacéutica (productos, clientes, pedidos e inventario) construida con
**Kotlin Multiplatform** y **Compose Multiplatform** para Android e iOS.

Curso: Desarrollo de Aplicaciones Móviles · UPeU · Ciclo VI · 2026-2
Autor: Miguel Pari

## Inicio rápido

1. Backend: en `pharmaSoft` (rama `config-pari`) ejecutar `docker compose up -d` y luego `.\mvnw spring-boot:run`.
2. App: abrir este proyecto en Android Studio y ejecutar `androidApp` en el emulador
   (la URL `http://10.0.2.2:8080/` apunta al backend de la PC).
3. Ingresar con `admin / Admin123` (también `vendedor / Vendedor123` y `almacen / Almacen123`).
4. Pruebas: `./gradlew :shared:testAndroidHostTest`.

| Sesión | Rama | Tema |
|---|---|---|
| 1 | `master` (commit inicial) | Entorno, proyecto KMP y Git |
| 2 | `feature/dominio-kmp-pari` | Dominio, null-safety, corrutinas y Flow |
| 3 | `feature/formulario-producto-pari` | Formulario Compose con validaciones |
| 4 | `feature/navegacion-pari` | Navegación, Material 3, tabs y diseño adaptativo |
| 5 | `feature/clean-mvvm` | Clean + MVVM, UiState y Koin |
| 7 | `feature/ktor-client-pari` | Cliente Ktor y GET |
| 8 | `feature/crud-productos-pari` | CRUD REST y errores tipificados |
| 9 | `feature/expect-actual-pari` | Capacidades nativas expect/actual |
| 10 | `feature/auth-jwt-pari` | JWT y almacenamiento seguro |
| 11 | `feature/offline-first-pari` | SQLDelight y offline-first |

> iOS: el código de `iosMain` está escrito, pero no se compiló porque requiere macOS con Xcode.

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

## Sesión 5 · Arquitectura Clean + MVVM

```
presentation  →  ProductoScreen · ProductoViewModel · ProductoUiState
     ↓
domain        →  Producto · ProductoRepository (interfaz) · RegistrarProductoUseCase
     ↑
data          →  ProductoRepositorioEnMemoria (implementa la interfaz)
di            →  AppModule.kt (dataModule, domainModule, presentationModule, platformModule)
```
Las flechas son dependencias: `domain` no importa nada de Compose, Ktor, Android ni iOS.
Koin arranca en `MainApplication` (Android) y en `iOSApp.swift` mediante `initKoinIos()`.

## Sesión 7 · Conectividad REST

| Plataforma | URL base | Motor Ktor |
|---|---|---|
| Emulador Android | `http://10.0.2.2:8080/` | OkHttp |
| Simulador iOS | `http://localhost:8080/` | Darwin |

Se configura en `platformModule` (`androidMain` / `iosMain`). Plugins instalados: ContentNegotiation,
Logging, HttpTimeout y DefaultRequest (`data/remote/HttpClientFactory.kt`).

**Endpoint consumido:** `GET /api/v1/productos?pagina=0&tamanio=100&ordenarPor=id&direccion=asc`

**DTO `ProductoDto`:** `id: Long`, `nombre: String`, `precio: Double`, `stock: Int`, `estado: Boolean`,
`categoriaId: Long`, `categoriaNombre: String?`, `fechaCreacion: String?`, `fechaModificacion: String?`,
dentro de `PaginaDto<T>` (`contenido`, `pagina`, `tamanio`, `totalElementos`, `totalPaginas`, `ultima`).

Backend: PharmaSoft (`LEEME-LOCAL.md` de la rama `config-pari`).

## Sesión 8 · CRUD REST y manejo de errores

| Operación | Verbo y ruta | Respuesta |
|---|---|---|
| Listar | `GET /api/v1/productos` | 200 + `PaginaDto<ProductoDto>` |
| Obtener | `GET /api/v1/productos/{id}` | 200 / 404 |
| Crear | `POST /api/v1/productos` | 201 / 400 / 409 |
| Actualizar | `PUT /api/v1/productos/{id}` | 200 / 400 / 404 / 409 |
| Eliminar | `DELETE /api/v1/productos/{id}` | 204 / 404 / 409 |
| Categorías | `GET /api/v1/categorias` | 200 + `List<CategoriaDto>` |

### Manejo de errores
Las excepciones de Ktor se traducen a `ErrorApi` (dominio) en un único punto: `data/remote/LlamadaApi.kt`.

| Situación | ErrorApi | Qué ve el usuario |
|---|---|---|
| Modo avión / servidor apagado | `SinConexion` | Mensaje + botón Reintentar |
| Respuesta lenta (> 15 s) | `TiempoAgotado` | Mensaje |
| 400 validación | `Validacion(campos)` | Error debajo de cada campo |
| 401 | `NoAutorizado` | Vuelve al login (Sesión 10) |
| 403 | `Prohibido` | "No tiene permisos…" |
| 404 | `NoEncontrado` | Mensaje |
| 409 regla de negocio | `Conflicto` | Mensaje del servidor |
| 5xx | `Servidor(codigo)` | Mensaje |
| JSON inesperado | `RespuestaInvalida` | Mensaje |

## Sesión 9 · Código específico de plataforma (expect/actual)

| Capacidad | Declaración (commonMain) | Android (androidMain) | iOS (iosMain) |
|---|---|---|---|
| Precio en soles | `expect fun formatearSoles` | `NumberFormat` con `Locale es-PE` | `NSNumberFormatter` con `es_PE` |
| Compartir producto | `interface Compartidor` (Koin) | `Intent.ACTION_SEND` + chooser | `UIActivityViewController` |
| Info del dispositivo | `expect fun infoDispositivo` | `android.os.Build` | `UIDevice` / `NSLocale` |

Archivos: `platform/Plataforma.kt`, `platform/Plataforma.android.kt`, `platform/Plataforma.ios.kt` y los
`PlatformModule.*.kt` de `di/`. El código de iOS está escrito pero no se compiló (requiere macOS + Xcode).

## Sesión 10 · Autenticación

- **Login:** `POST /api/v1/auth/login` → `token` JWT, `expiraEnSegundos`, `roles` y `permisos`.
- **Protección de solicitudes:** el plugin `AutenticacionJwt` (`data/auth/PluginAutenticacion.kt`) agrega
  `Authorization: Bearer <token>` a cada petición y, ante un **401** de una petición con token, cierra la sesión.
- **Almacenamiento seguro:** `AlmacenSeguroAndroid` (AES-256/GCM con clave en Android Keystore) y
  `AlmacenSeguroIos` (Keychain). El token nunca se guarda en texto plano.
- **Log saneado:** `sanitizeHeader` muestra `Authorization: ***` en el registro de Ktor, y la petición
  `/auth/login` no se registra (su cuerpo lleva la contraseña y la respuesta, el token).
- **Permisos en la interfaz:** sin `CATALOGO_ESCRIBIR` se ocultan Nuevo, Editar y Eliminar.
  "Probar acceso a reportes" (Inicio) provoca un 403 con cualquier rol que no sea ADMIN.

| Usuario | Contraseña | Rol |
|---|---|---|
| `admin` | `Admin123` | ADMIN |
| `vendedor` | `Vendedor123` | VENDEDOR |
| `almacen` | `Almacen123` | ALMACENERO |

Para probar la expiración, arrancar PharmaSoft con `JWT_EXPIRACION_MINUTOS=2`.

## Sesión 11 · Persistencia offline-first

- **Fuente de verdad:** base local SQLDelight `pharma.db` (`commonMain/sqldelight/.../db/*.sq`): tablas
  `producto` (con la marca `pendiente`), `categoria` y `sincronizacion`.
- **Drivers:** `AndroidSqliteDriver` y `NativeSqliteDriver` (`CreadorDeDriver` en `platformModule`);
  `JdbcSqliteDriver` en memoria para las pruebas en la JVM.
- **Contrato:** `ProductoRepository.observar(): Flow` + `sincronizar()`. El ViewModel se suscribe una vez y nunca
  vuelve a pedir la lista.
- **Sincronizar:** primero sube lo pendiente y después baja del servidor; el reemplazo va en una transacción y
  solo borra filas con `pendiente = 0`.
- **Sin red:** lo creado se guarda con un id local negativo y se envía al reconectar. La pantalla muestra el aviso y
  "Actualizado hace …".
- **Pruebas:** `ProductoRepositorioOfflineTest` (los 6 escenarios de la guía, más el registro con red que devuelve
  el producto del servidor) + `ProductoViewModelTest`.

Documentación de las actividades: `docs/sesion10/` y `docs/sesion11/`.
