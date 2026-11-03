# Actividad Autónoma N.º 10 · Validación de expiración, almacenamiento y cierre de sesión

**Curso:** Desarrollo de Aplicaciones Móviles · Ciclo VI · 2026-2
**Estudiante:** Miguel Pari
**Proyecto:** PharmaMobil (KMP) sobre PharmaSoft (API REST)
**Repositorio:** https://github.com/P4R1-R3P0S1T0R13/PharmaMobilV2.0 · rama `feature/auth-jwt-pari`
**Plataforma de pruebas:** Android (emulador). iOS: el código está escrito (`AlmacenSeguroIos`, Keychain), pero no se verificó porque no dispongo de macOS.

> **Nota (Sesión 11):** en la versión final (`master`) cambió el escenario 2. Un token vencido ya no cierra la sesión localmente: la app espera el 401 del servidor, para no expulsar al usuario cuando está sin red. Ver la Actividad Autónoma 11, escenario 5. Esta bitácora describe la rama `feature/auth-jwt-pari`.

> Cómo usar este documento: las columnas **Resultado esperado** describen lo que el código implementa. Después de ejecutar cada prueba, completa **Fecha/hora**, **Observado** y la **captura**. Si lo observado no coincide con lo esperado, regístralo como hallazgo.

---

## Preparación del entorno

1. Base de datos: `docker compose up -d` en `pharmaSoft` (rama `config-pari`).
2. Backend con expiración corta para el escenario 2:
   `$env:JWT_EXPIRACION_MINUTOS=1; .\mvnw spring-boot:run`
3. App: `./gradlew :androidApp:installDebug` y abrir PharmaMobil en el emulador.
4. Logcat filtrado por `System.out` para ver el registro de Ktor.

Los escenarios 1, 3, 4 y 5 se lanzan desde **Inicio → Pruebas de sesión**, que muestra el código HTTP y el mensaje literal del servidor.

---

## Producto 1 · Bitácora de escenarios de token

### Resumen

| N.º | Escenario | Cómo se provoca en PharmaMobil | Resultado esperado | Qué ve el usuario |
|---|---|---|---|---|
| 1 | Token válido | Iniciar sesión como `admin` → botón **1. Consultar /auth/me** | 200 | `GET /auth/me con token → HTTP 200 · OK` |
| 2 | Token expirado | Backend con `JWT_EXPIRACION_MINUTOS=1`, iniciar sesión, esperar más de 1 min y abrir Productos | La app detecta la expiración **antes** de enviar (`GestorSesion.tokenVigente`) y no llega a hacer la petición | Vuelve al login con «Tu sesión expiró. Inicia sesión nuevamente.» |
| 3 | Token alterado | Botón **3. Alterar token guardado** → abrir Productos | 401 «El token no es válido» | Vuelve al login con «El servidor ya no acepta tu sesión (401…)» |
| 4 | Sin cabecera | Botón **4. Petición sin cabecera** | 401 «Debe iniciar sesión para acceder a este recurso» | `HTTP 401 · «Debe iniciar sesión…»` (la sesión sigue activa porque la prueba no lleva token) |
| 5 | Prefijo incorrecto | Botón **5. Prefijo 'bearer' en minúscula** | 401: el filtro solo reconoce `Bearer ` y trata la petición como anónima | `HTTP 401 · «Debe iniciar sesión…»` |
| 6 | Usuario deshabilitado | Con la sesión abierta: `update usuarios set estado = false where username = 'vendedor'; commit;` → abrir Productos | 401 «El usuario está deshabilitado» | Vuelve al login con «El servidor ya no acepta tu sesión (401…)» |

Para restaurar el usuario del escenario 6: `update usuarios set estado = true where username = 'vendedor'; commit;`

### Registro detallado (uno por escenario)

Copiar este bloque seis veces:

| Campo | Valor |
|---|---|
| Identificador y nombre | E1 · Token válido |
| Fecha y hora | |
| Plataforma | Android (emulador API __) |
| Pasos exactos | |
| Código HTTP recibido | |
| Mensaje literal (`message`) | |
| Qué vio el usuario | |
| Captura | |
| Conclusión (una línea) | |

### Escenarios 2 y 6: invalidación sin acción del usuario

PharmaMobil **sí detecta los dos casos y lleva al login**. No se queda mostrando errores con una sesión que parece activa:

- **Escenario 2 (expiración):** al iniciar sesión se guarda `expiraEnMs = ahora + expiraEnSegundos × 1000`. Antes de cada petición, el plugin `AutenticacionJwt` pide el token a `GestorSesion.tokenVigente()`. Si ya venció, se borra del almacén y el estado pasa a `Inactiva("Tu sesión expiró…")`. Lo mismo ocurre al abrir la app con un token vencido.
- **Escenario 6 (usuario deshabilitado):** el servidor responde 401 a una petición que sí llevaba token. El hook `on(Send)` del plugin lo detecta y llama a `gestor.rechazadoPorServidor()`, que borra el token y muestra el login con el motivo.

---

## Producto 2 · Evidencia del almacenamiento seguro

| Comprobación | Cómo hacerla | Qué debe verse | Captura |
|---|---|---|---|
| El archivo no está en claro | Android Studio → Device Explorer → `data/data/pe.edu.upeu.pharmamobil/shared_prefs/pharmamobil_seguro.xml` | La entrada `sesion_jwt` contiene `IV:textoCifrado` en Base64, no un `eyJ…` legible | |
| El registro no filtra la cabecera | Ejecutar cualquier operación y revisar Logcat (`System.out`) | `Authorization: ***` (configurado con `sanitizeHeader` en `HttpClientFactory.kt`) | |
| La sesión sobrevive al cierre | Iniciar sesión, cerrar la app desde Recientes y volver a abrirla | Entra directo a Inicio, sin pedir credenciales | |

**Cómo se cifra (Android):** `AlmacenSeguroAndroid` genera una clave AES-256 dentro de **Android Keystore** (alias `pharmamobil_clave_sesion`), que no se puede extraer del dispositivo. Cada valor se cifra con `AES/GCM/NoPadding` y en SharedPreferences solo se guarda `IV:cifrado`. Si el valor se altera, GCM detecta la manipulación, el valor se descarta y la app pide login.

**iOS:** `AlmacenSeguroIos` usa Keychain con `kSecClassGenericPassword`, el servicio `pe.edu.upeu.pharmamobil`, la cuenta `sesion_jwt` y la opción `kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly`. No se verificó porque no dispongo de macOS.

---

## Producto 3 · Cierre de sesión completo

| N.º | Efecto | Cómo lo garantiza el código | Cómo se comprueba | Captura |
|---|---|---|---|---|
| 1 | El token desaparece del almacén | `GestorSesion.cerrar()` → `almacen.borrar("sesion_jwt")` | Reabrir `pharmamobil_seguro.xml`: la clave ya no existe | |
| 2 | El cliente HTTP olvida el token | No hay caché: el plugin pide el token a `GestorSesion` **en cada petición**, y tras cerrar sesión recibe `null` (no se usa el proveedor `bearer` de Ktor, que sí guarda tokens en memoria) | Prueba 4 de Inicio, o Swagger: la petición siguiente sale sin cabecera → 401 | |
| 3 | La interfaz queda limpia | El login arranca con `LoginUiState()` vacío y los ViewModel llevan `key = usuario`, así que el siguiente usuario no ve datos del anterior | Cerrar como `admin` y entrar como `vendedor`: el formulario está vacío y Productos se recarga sin botones de edición | |
| 4 | No se vuelve atrás a una pantalla autenticada | `App()` decide la pantalla por el `EstadoSesion`; con `Inactiva`, la única pantalla que existe es el login | Botón Atrás del sistema en el login: la app se cierra, no vuelve al listado | |

---

## Producto 4 · Matriz de permisos por rol

Los permisos salen de `PermisosPorRol` y `SecurityConfig` de PharmaSoft. En cada celda: código esperado / si la interfaz ofrece la acción.

| Operación | admin | vendedor | almacen |
|---|---|---|---|
| Listar productos (`GET /productos`) | 200 / Sí | 200 / Sí | 200 / Sí |
| Registrar producto (`POST /productos`) | 201 / Sí | 403 / **No** (botón Nuevo oculto) | 201 / Sí |
| Listar clientes (`GET /clientes`) | 200 / No* | 200 / No* | 403 / No* |
| Registrar venta (`POST /ventas`) | 201 / No* | 201 / No* | 403 / No* |
| Ver reportes (`GET /reportes/…`) | 200 / Sí | 403 / Sí** | 403 / Sí** |
| Consultar `/auth/me` | 200 / Sí | 200 / Sí | 200 / Sí |

\* Los módulos Clientes y Pedidos de la app todavía son demostrativos y no consumen la API. Estas filas se prueban con Swagger, usando el token de cada usuario (botón *Authorize*).
\*\* **Hallazgo:** el botón «Probar acceso a reportes» se muestra a todos los roles a propósito, para provocar el 403 que pide la guía práctica. En una versión final debería ocultarse con `sesion.puede(Permisos.REPORTES_VER)`.

**Columna observada** (completar al ejecutar):

| Operación | admin | vendedor | almacen |
|---|---|---|---|
| Listar productos | | | |
| Registrar producto | | | |
| Listar clientes | | | |
| Registrar venta | | | |
| Ver reportes | | | |
| Consultar /auth/me | | | |

---

## Pruebas automatizadas relacionadas

`shared/src/commonTest/.../data/auth/SesionJwtTest.kt` (`./gradlew :shared:testAndroidHostTest`):

| Prueba | Escenario |
|---|---|
| `tokenVigenteSeRecuperaAlReiniciarLaApp` | La sesión sobrevive al cierre |
| `tokenExpiradoAlReiniciarPideLoginYSeBorra` | Escenario 2 al abrir la app |
| `tokenQueVenceDuranteElUsoTerminaLaSesion` | Escenario 2 durante el uso |
| `valorAlteradoEnElAlmacenSeDescarta` | Almacén manipulado |
| `cerrarSesionEliminaElTokenPorCompleto` | Efectos 1 y 2 del cierre |
| `elPluginAdjuntaElTokenYUn401TerminaLaSesion` | Cabecera Bearer + escenarios 3 y 6 |
