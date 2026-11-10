# Pasos 2 a 6 · Firma, minificación, builds y versión de entrega

**Proyecto:** PharmaMobil · **Rama:** `feature/builds-14` · **Estudiante:** Miguel Pari

## Paso 2 · Almacén de claves

| Dato | Valor |
|---|---|
| Archivo | `pharmamobil.jks`, **fuera** de la carpeta del proyecto |
| Alias | `pharmamobil` |
| Algoritmo | RSA 2048, validez 10 000 días |
| Respaldo | Copia en un medio externo a la PC (USB/nube personal) |
| Contraseñas | Solo en `keystore.properties` local, nunca en el repositorio ni en este documento |

Si se pierde el almacén, no se puede publicar una actualización de la misma app: por eso el respaldo fuera de la PC.

## Paso 3 · Firma sin secretos en Git

- `androidApp/build.gradle.kts` lee `keystore.properties` de la raíz **solo si existe**. Quien clona el repositorio compila en debug sin errores; la release solo se firma en la PC que tiene las claves.
- `.gitignore` excluye `keystore.properties`, `*.jks` y `*.keystore`. En el repositorio solo está `keystore.properties.example`, sin valores reales.
- Comprobación antes de cada push: `git status` no debe listar ninguno de esos archivos.

## Paso 4 · Builds de entrega

| Artefacto | Comando | Uso |
|---|---|---|
| `Pari_ProyectoFinal_Android.aab` | `./gradlew :androidApp:entregables` | Formato de Google Play (la tienda genera los APK por dispositivo) |
| `Pari_ProyectoFinal_Android.apk` | (misma tarea) | Instalación directa en los dispositivos de prueba |

La tarea `entregables` ejecuta `bundleRelease` y `assembleRelease` y copia ambos archivos a `entregables/builds/` con la nomenclatura de entrega.

Verificación de la firma:
```
apksigner verify --print-certs entregables/builds/Pari_ProyectoFinal_Android.apk
jarsigner -verify -verbose -certs entregables/builds/Pari_ProyectoFinal_Android.aab
```
El certificado mostrado debe ser el del alias `pharmamobil`, no el de depuración (`CN=Android Debug`).

## Paso 5 · Minificación (R8)

- `isMinifyEnabled = true` en release, con `proguard-android-optimize.txt` + `proguard-rules.pro`.
- Reglas propias: serializadores de kotlinx.serialization de los DTO, clases generadas de SQLDelight (`pe.edu.upeu.pharmamobil.db`), `dontwarn` de dependencias de Ktor que no existen en Android, y número de línea conservado para que las pilas de Crashlytics sean legibles.
- **Plan de respaldo:** si la build minificada falla en una prueba que en debug pasa, primero se agrega la regla `-keep` de la clase afectada; si el tiempo no alcanza, se entrega con `isMinifyEnabled = false` y se declara aquí como decisión.

## Paso 6 · iOS (IPA): limitación declarada

| Requisito | Estado |
|---|---|
| Código `iosMain` (Keychain, permisos, notificaciones, Darwin) | Implementado |
| Compilar el framework y la app iOS | **No realizado**: Kotlin/Native para iOS y Xcode solo funcionan en macOS |
| Firmar y exportar el `.ipa` | **No realizado**: requiere Xcode y una cuenta de Apple Developer |

No dispongo de una Mac. Para generar el IPA haría falta una Mac (o un runner macOS de CI) y una cuenta de Apple Developer: `Product → Archive` en Xcode y luego `Distribute App → Ad Hoc` o `Development`. La entrega final es Android.

## Versión de entrega

| Campo | Valor | Motivo |
|---|---|---|
| applicationId | `pe.edu.upeu.pharmamobil` | Se mantiene el id con el que está registrada la app en Firebase (`google-services.json`); cambiarlo rompería Crashlytics |
| versionCode | 1 | Primera build firmada |
| versionName | 1.0.0 | Versión final del curso |
| Etiqueta Git | `v1.0-final` | Se crea solo después de verificar la build firmada en los dispositivos |
