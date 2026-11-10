# Paso 1 · Ficha de integración cerrada y alcance congelado

**Proyecto:** PharmaMobil (KMP) sobre PharmaSoft · **Rama:** `feature/builds-14` · **Estudiante:** Miguel Pari
**Fecha de congelamiento:** 10 de noviembre de 2026

## Ficha de la sesión 13, clasificada

| N.º | Hueco | Clasificación | Motivo / qué haría falta |
|---|---|---|---|
| H1 | No se veía cuántos cambios faltan por enviar | **Cerrado** (S13) | Barra «Sin conexión · N cambios por enviar» |
| H2 | El reintento duplicaba y la fila quedaba atascada | **Cerrado** (S13) | Idempotencia por nombre ante un 409 |
| H3 | Un error permanente tumbaba la sincronización | **Cerrado** (S13) | Intentos por fila, transitorio/permanente, máximo 5 |
| H5 | Quedaban datos del usuario anterior | **Cerrado** (S13) | Cerrar sesión borra la base local |
| H6 | Sin avisos de envío, fallo o bajo stock | **Cerrado** (S13) | Notificaciones con `POST_NOTIFICATIONS` |
| H7 | Errores de producción sin registro | **Cerrado** (S13) | Firebase Crashlytics detrás de `Telemetria` |
| H11 | El recorrido no mostraba ids ni filas descargadas | **Cerrado** (AA13) | «ID n / ID local -n» y «N productos descargados (HH:mm)» |
| H9 | «Probar acceso a reportes» y las pruebas de sesión aparecían a todos | **Se cierra hoy** | Solo se muestran en la build de depuración (`InfoApp.depuracion`) |
| H12 | El ícono era el de la plantilla KMP | **Se cierra hoy** | Ícono propio: cruz de farmacia sobre el verde corporativo (adaptativo + mipmaps) |
| H13 | La URL del servicio era `10.0.2.2`, solo válida en el emulador | **Se cierra hoy** | URL configurable por build (`local.properties` → `BuildConfig`) y tráfico HTTP permitido para el backend local |
| H4 | Una edición en curso se pierde si el token vence | **Se declara** (limitación) | Una creación nunca se pierde (se guarda primero en local). Para la edición haría falta guardar el borrador en la base o en el `SavedStateHandle` y restaurarlo tras el login. Cambia el flujo de sesión y no se puede volver a probar hoy |
| H8 | No sincroniza sola al recuperar la red | **Se declara** (limitación) | El usuario sincroniza con ⟳; la barra le indica cuántos cambios faltan. Haría falta WorkManager con la restricción `CONNECTED`: es una librería nueva, y el alcance está congelado |
| H10 | La app no se ejecuta en iOS | **Se declara** (limitación) | Kotlin/Native para iOS solo compila en macOS con Xcode, y no dispongo de ninguno. El código de `iosMain` está implementado. Haría falta una Mac o un runner macOS de CI |

## Lo que se descarta (trabajo futuro, nunca estuvo en el alcance)

- CRUD real de Clientes y Pedidos: hoy son pantallas demostrativas.
- Servicio PharmaSoft con HTTPS y dominio público. Hoy es HTTP en la red local, por eso se permite el tráfico sin cifrar.
- Publicación en Google Play.

## Alcance congelado (10/11/2026)

A partir de este punto **no entra funcionalidad nueva**: ni funciones fuera del alcance, ni cambios de arquitectura o de librerías, ni rediseños de pantalla, ni migraciones del esquema. El resto de la sesión solo configura la firma, la minificación y los builds, y registra los defectos de las pruebas en dispositivos reales.

**Alcance entregado:** login JWT con roles · CRUD de productos con categorías y errores tipificados · offline-first con SQLDelight y cola de envío con reintentos · permiso de notificaciones con sus 3 estados · Firebase Crashlytics · precio en soles, compartir e info del dispositivo (expect/actual) · navegación adaptativa con tema claro/oscuro.
