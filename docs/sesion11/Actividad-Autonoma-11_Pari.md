# Actividad Autónoma N.º 11 · Pruebas unitarias y evidencia de escenarios offline

**Curso:** Desarrollo de Aplicaciones Móviles · Ciclo VI · 2026-2
**Estudiante:** Miguel Pari
**Proyecto:** PharmaMobil (KMP) sobre PharmaSoft (API REST)
**Repositorio:** https://github.com/P4R1-R3P0S1T0R13/PharmaMobilV2.0 · rama `feature/offline-first-pari`
**Plataforma de pruebas:** Android (emulador). iOS: el driver nativo (`CreadorDeDriverIos`) y las pruebas en `iosTest` están escritos, pero no se ejecutaron porque no dispongo de macOS.

> Las columnas **Resultado esperado** describen lo que implementa el código. Después de ejecutar cada escenario, completa **Fecha/hora**, **Observado** y la **captura**.

---

## Arquitectura offline-first en una figura

```
          ┌──────────── ProductoScreen ────────────┐
          │  lee SIEMPRE de la base local (Flow)   │
          └───────────────▲────────────────────────┘
                          │ observar()
                 ProductoViewModel ── sincronizar() ──┐
                          │                           │
              ProductoRepositorioOffline              │
                 │                  │                 │
   ProductoLocal (SQLDelight)   ProductoApi (Ktor + JWT)
     pharma.db: producto,           PharmaSoft
     categoria, sincronizacion
```

`sincronizar()` = **1)** subir lo pendiente → **2)** bajar productos y categorías → **3)** reemplazar solo las filas con `pendiente = 0`, dentro de una transacción → **4)** guardar la hora en la tabla `sincronizacion`.

*Figura: elaboración propia.*

---

## Producto 1 · Suite de pruebas en commonTest

Archivo: `shared/src/commonTest/kotlin/pe/edu/upeu/pharmamobil/data/repository/ProductoRepositorioOfflineTest.kt`
Base: SQLDelight en memoria, una por prueba (`fakes/BaseEnMemoria.kt` usa `JdbcSqliteDriver` en la JVM e `inMemoryDriver` en iOS). Servidor: `fakes/ServidorSimulado.kt` sobre el `MockEngine` de Ktor, con un interruptor `sinRed` que simula el modo avión.

| N.º | Prueba de la guía | Método | Qué verifica |
|---|---|---|---|
| 1 | Sincronización con red | `p1SincronizacionConRedLlenaLaBaseYElFlowEmiteTodo` | La base recibe los 2 productos del servidor, el Flow los emite y se guarda la hora |
| 2 | Registro sin red | `p2RegistroSinRedQuedaPendienteYElFlowLoEmite` | Queda con `pendiente = true` e id negativo, y el Flow lo emite |
| 3 | Envío al reconectar | `p3AlReconectarSeEnviaLoPendienteYDejaDeEstarMarcado` | Llega al servidor y vuelve con un id positivo, ya no marcado |
| 4 | La bajada no borra lo pendiente | `p4LaBajadaDelServidorNoBorraLoPendiente` | `reemplazarSincronizados` conserva la fila pendiente |
| 5 | Identificadores sin colisión | `p5IdLocalYDelServidorConvivenSinSobrescribirse` | Conviven los ids `1`, `-1` y `-2` |
| 6 | Sincronizar dos veces no duplica | `p6SincronizarDosVecesNoDuplica` | 2 filas locales, 2 en el servidor y 1 sola petición POST |

Ejecución completa (adjuntar la salida de consola entera, no la captura de un solo método):

```bash
./gradlew :shared:testAndroidHostTest
```

Informe HTML: `shared/build/reports/tests/testAndroidHostTest/index.html`

**Salida obtenida:** *(pegar aquí)*

Otras pruebas de la sesión: `ProductoViewModelTest` cubre la primera apertura sin red, el registro pendiente sin recargar la lista y la antigüedad en lenguaje natural.

---

## Producto 2 · Bitácora de escenarios offline

| N.º | Escenario | Pasos en PharmaMobil | Resultado esperado | En la base local |
|---|---|---|---|---|
| 1 | Apertura sin red con datos previos | Abrir con red y esperar a que sincronice → cerrar la app → modo avión → abrir | Se ve el listado; la barra superior dice «Sin conexión: mostrando los datos guardados» y la antigüedad | Filas con `pendiente = 0` |
| 2 | Registro sin red | En modo avión: Nuevo → Ibuprofeno / 4.50 / 20 / Analgésicos → Registrar | Aparece de inmediato, primero en la lista, con el chip «Pendiente de envío». Mensaje: «…guardado sin conexión; se enviará al sincronizar» | Fila nueva con `id = -1` y `pendiente = 1` |
| 3 | Reconexión y envío | Quitar el modo avión → botón Sincronizar (⟳) | Desaparece el chip y el producto queda con id real. **Evidencia:** aparece en Swagger `GET /api/v1/productos` con id positivo | La fila `-1` se reemplaza por la del servidor |
| 4 | Primera apertura sin red | Ajustes → Apps → PharmaMobil → Borrar datos → modo avión → abrir → iniciar sesión* | Estado vacío informativo: «Aún no hay datos guardados. Conéctate a internet una vez…» (no es una pantalla de error) | Tablas vacías |
| 5 | Sesión expirada sin red | Backend con `JWT_EXPIRACION_MINUTOS=1` → iniciar sesión y sincronizar → esperar más de 1 min → modo avión → abrir la app | Se muestran los datos locales y no se pide login. Al quitar el modo avión y sincronizar, el servidor responde 401 y la app vuelve al login | Datos intactos |

\* Para el escenario 4 hay que iniciar sesión, y eso requiere red. Opciones: hacer login con red, borrar solo la base (Device Explorer → `databases/pharma.db`) y luego activar el modo avión; o documentar que el escenario empieza ya con la sesión iniciada.

Para inspeccionar la base: Android Studio → **App Inspection → Database Inspector** → `pharma.db`.

### Registro detallado (copiar cinco veces)

| Campo | Valor |
|---|---|
| Identificador y nombre | O1 · Apertura sin red con datos previos |
| Fecha y hora | |
| Plataforma | Android (emulador API __) |
| Pasos exactos | |
| Qué mostró la aplicación | |
| Qué había en la base local | |
| Captura | |
| Conclusión (una línea) | |

### Escenario 5: decisión y argumento

**Qué hace hoy la app:** si el token venció y no hay red, la sesión **se mantiene** y el usuario ve sus datos locales. Al reconectar, la siguiente petición lleva el token vencido, el servidor responde 401 y el plugin `AutenticacionJwt` cierra la sesión y muestra el login.

**Por qué:** mandar al usuario al login cuando no hay red para iniciar sesión lo deja sin nada. Mostrarle los datos que ya tiene es más útil, y la seguridad no se pierde porque el servidor sigue rechazando el token vencido.

**Cambio respecto a la Sesión 10:** en la rama `feature/auth-jwt-pari`, `GestorSesion` cerraba la sesión localmente en cuanto el token vencía. En esta rama se cambió `tokenVigente()` por `tokenActual()`: la app ya no cierra la sesión sola y deja que el servidor decida con un 401. La prueba `tokenExpiradoNoCierraLaSesionHastaQueElServidorLoRechace` documenta el comportamiento nuevo.

---

## Producto 3 · Antigüedad de los datos en pantalla

| Elemento | Implementación |
|---|---|
| Registro del instante | Tabla `sincronizacion` (una sola fila, `id = 1`, `ultima_ms`). Se escribe dentro de la misma transacción que reemplaza los datos, así que solo se guarda si la sincronización termina bien |
| Presentación | Barra superior de Productos: `textoAntiguedad()` en `ProductoUiState.kt` muestra «Actualizado hace un momento / hace 5 minutos / hace 2 horas / hace 3 días». Se refresca cada minuto |
| Caso sin sincronización | «Aún no se sincronizó con el servidor» |
| Evidencia | Captura 1: justo después de sincronizar. Captura 2, con más de un día: en Database Inspector ejecutar `UPDATE sincronizacion SET ultima_ms = ultima_ms - 172800000;` (resta 2 días) y volver a la pantalla |

---

## Producto 4 · Guion de sustentación del Producto U2 (8 minutos)

**Preparación previa**
- `docker compose up -d` y PharmaSoft corriendo (`.\mvnw spring-boot:run`), con Swagger abierto en el navegador.
- La app instalada, con una sincronización hecha y **la sesión cerrada** (para mostrar el login).
- Emulador con el acceso rápido a modo avión a mano.
- Terminal con `./gradlew :shared:testAndroidHostTest` ya ejecutado y su reporte abierto.

| Minuto | Criterio | Usuario | Qué se muestra |
|---|---|---|---|
| 0–1 | Autenticación JWT | `admin` | Login con `admin / clave-mala` → «Usuario o contraseña incorrectos». Luego `admin / Admin123` → Inicio con rol y permisos. Logcat: `Authorization: ***` |
| 1–3 | Consumo REST y CRUD | `admin` | Productos: listar (pestañas Activos / Inactivos / Bajo stock) → Nuevo «Ketoprofeno 100 mg» → editar su stock → eliminarlo. Swagger para confirmar cada paso |
| 3–5 | Estados y errores | `almacen`, luego `vendedor` | Con `almacen`: registrar un nombre ya existente → 409 en el snackbar. Con `vendedor`: no aparecen Nuevo / Editar / Eliminar, e Inicio → «Probar acceso a reportes» → 403. Si el servidor devuelve un 400 de validación, el mensaje aparece debajo del campo |
| 5–7 | Offline-first | `admin` | Modo avión → la lista sigue visible con el aviso → registrar «Cetirizina 10 mg» (chip Pendiente) → quitar el modo avión → Sincronizar → el producto aparece en Swagger con id real. Mostrar el reporte de las 6 pruebas |
| 7–8 | Multiplataforma | — | Android en vivo. iOS: mostrar `iosMain` (Darwin, Keychain, NativeSqliteDriver) y explicar que no se ejecutó por falta de macOS |

**Plan B:** si el backend falla durante la demo, se cumple igual el tramo offline (la app sigue funcionando sin servidor) y se muestra el reporte de pruebas.
