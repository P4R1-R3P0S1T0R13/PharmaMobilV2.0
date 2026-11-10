# Paso 1 · Recorrido completo y tabla de huecos

**Proyecto:** PharmaMobil (KMP) sobre PharmaSoft · **Rama:** `feature/integrador-13` · **Estudiante:** Miguel Pari

El recorrido se hizo sobre el `master` de la Unidad 2, en Android (emulador), usándolo como lo haría alguien que la abre por primera vez: instalar, iniciar sesión, crear un producto, perder la conexión, sincronizar, provocar un error de validación y cerrar sesión.

**Gravedad:** **Alta** = se pierde o se duplica un dato, o el usuario queda sin salida · **Media** = el usuario no entiende qué pasó, pero no pierde nada · **Baja** = algo cosmético o una mejora.

## Recorrido guiado (los 8 momentos de la guía)

| N.º | Momento | Qué se observó | ¿Hueco? |
|---|---|---|---|
| 1 | Primera apertura, sin sesión | Se muestra el login con logo y explicación; con Atrás se sale de la app. | No |
| 2 | Login con credenciales incorrectas | «Usuario o contraseña incorrectos» (401) es distinto de «Sin conexión con el servidor» (sin red): `ErrorApi` los separa. | No |
| 3 | Listado con la base local vacía | Estado vacío propio: «Aún no hay datos guardados. Conéctate a internet una vez…». No parece un error. | No |
| 4 | Creación en modo avión | Se guarda con id negativo y el chip «Pendiente de envío». **Pero** la pantalla no dice cuántos cambios faltan por enviar. | H1 |
| 5 | Sincronización al recuperar la señal | El registro llega y la lista se actualiza sola. **Pero** si la respuesta del POST se pierde (tiempo agotado), el reintento choca con el nombre único del servidor (409) y la fila queda pendiente **para siempre**, reintentándose en cada sincronización. | H2 |
| 6 | Error de validación del servidor | Con red, el 400 llega al campo que lo causó. **Pero** si el error lo provoca una fila pendiente al sincronizar, el error tumba toda la sincronización y la fila nunca deja de reintentarse; el usuario no sabe cuál falló. | H3 |
| 7 | Token vencido a mitad de un formulario | La app vuelve al login. Una **creación** no se pierde, porque se guarda primero en local. Una **edición** sí se pierde. | H4 |
| 8 | Cierre de sesión | El token se borra (Keystore). **Pero** `pharma.db` conserva los productos **y los pendientes** del usuario anterior: si entra otro usuario, ve esos datos y sus pendientes se enviarían con su token. | H5 |

## Ficha de integración: huecos, decisión y fecha de cierre

| N.º | Hueco detectado | Gravedad | Decisión | Fecha de cierre | Cómo se cierra |
|---|---|---|---|---|---|
| H1 | No se ve cuántos cambios faltan por enviar | Media | Cerrado hoy | 2026-11-04 | Barra «Sin conexión · N cambios por enviar» |
| H2 | Un reintento tras una respuesta perdida duplica (409) y deja la fila atascada | Alta | Cerrado hoy | 2026-11-04 | Un 409 contra un producto idéntico se reconoce como el mismo envío |
| H3 | Un error permanente de una fila tumba la sincronización y se reintenta sin fin | Alta | Cerrado hoy | 2026-11-04 | Intentos y último error por fila, transitorio/permanente, máximo 5 intentos, fila fallida visible con Reintentar o Descartar |
| H4 | Una edición en curso se pierde si el token vence | Media | Sesión 14 | 2026-11-17 | Guardar el borrador del formulario antes de ir al login |
| H5 | Al cerrar sesión quedan los datos y los pendientes del usuario anterior | Alta | Cerrado hoy | 2026-11-04 | Cerrar sesión borra la base local (con aviso si hay pendientes); también al entrar otro usuario |
| H6 | Nadie avisa cuando los cambios se envían, fallan o hay bajo stock | Media | Cerrado hoy | 2026-11-04 | Notificaciones con `POST_NOTIFICATIONS` |
| H7 | Los errores en producción no se registran | Media | Cerrado hoy | 2026-11-04 | Firebase Crashlytics detrás de `Telemetria` |
| H8 | No se sincroniza sola al recuperar la red (hay que pulsar ⟳) | Media | Sesión 14 | 2026-11-17 | WorkManager con restricción de red conectada |
| H9 | «Probar acceso a reportes» aparece a todos los roles | Baja | Sesión 14 | 2026-11-17 | Ocultarlo en la build de release |
| H10 | La app no se ejecuta en iOS | Media | Descartado | — | Justificación: no dispongo de macOS ni Xcode, que son obligatorios para compilar Kotlin/Native para iOS; el código de `iosMain` está implementado |
| H11 | El recorrido no mostraba el id de cada producto ni las filas y la hora de la última sincronización | Baja | Cerrado hoy | 2026-11-10 | La lista muestra «ID n» o «ID local -n» y la barra «Actualizado hace … (HH:mm) · N productos descargados» |

## Decisiones de diseño que salen del recorrido

- **Sin conexión no consume intentos.** La guía lista «sin red» como fallo transitorio, y lo es: no se marca la fila como fallida. Pero si se contara, cinco aperturas en modo avión rendirían la fila sin que el servidor la haya visto nunca. Por eso, sin red la sincronización **se corta** (no tiene sentido seguir ni bajar) y la fila queda intacta. Tiempo agotado, 5xx y respuesta ilegible sí suman intento.
- **El 401 no es un fallo de la fila.** Si el servidor rechaza el token, la sincronización se detiene sin tocar la cola y el plugin de la Sesión 10 lleva al login. Marcar la fila como fallida castigaría al dato por un problema de la sesión.
- **Idempotencia.** PharmaSoft no acepta una clave de operación, pero el nombre del producto es único. Un 409 con un producto **idéntico** en el servidor se trata como éxito del envío anterior. Si los datos difieren, es un conflicto real y la fila queda fallida para que el usuario decida.
