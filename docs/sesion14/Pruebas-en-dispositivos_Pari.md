# Paso 7 · Pruebas de la build firmada en dispositivos reales

**Build probada:** `Pari_ProyectoFinal_Android.apk` (release, firmada, minificada) · versión 1.0.0 (1)

## Preparación

1. PC y teléfonos en la misma red Wi-Fi.
2. IP de la PC: `ipconfig` → «Dirección IPv4».
3. `local.properties`: `pharmasoft.url=http://<IP>:8080/` y regenerar la build.
4. Abrir el puerto en el firewall de Windows (PowerShell como administrador):
   ```
   New-NetFirewallRule -DisplayName "PharmaSoft 8080" -Direction Inbound -Protocol TCP -LocalPort 8080 -Action Allow
   ```
5. Comprobar desde el navegador del teléfono: `http://<IP>:8080/` responde.
6. Desinstalar cualquier versión debug previa (la firma distinta impide actualizar) e instalar el APK.

## Dispositivos

| Id | Marca y modelo | Android | Pantalla |
|---|---|---|---|
| D1 | _completar_ | _completar_ | _completar_ |
| D2 | _completar_ | _completar_ | _completar_ |

## Matriz de pruebas

| N.º | Caso | Pasos | Resultado esperado | D1 | D2 |
|---|---|---|---|---|---|
| P01 | Instalación | Instalar el APK firmado | Se instala; ícono propio de PharmaMobil | | |
| P02 | Login correcto | admin / contraseña válida | Entra a Inicio con el rol ADMIN | | |
| P03 | Login incorrecto | Contraseña errónea | Mensaje de credenciales inválidas, sin cierre | | |
| P04 | Roles | Entrar como vendedor y como almacen | Cada rol ve solo sus opciones | | |
| P05 | Listar productos | Abrir Productos con red | Lista descargada desde PharmaSoft | | |
| P06 | Crear producto | Formulario válido → Guardar | Aparece en la lista con ID del servidor | | |
| P07 | Validación | Guardar con campos vacíos | Errores por campo, no se envía | | |
| P08 | Editar y eliminar | Editar un producto y eliminar otro | Cambios reflejados en lista y servidor | | |
| P09 | Sin conexión | Modo avión → crear producto | Se guarda local («ID local -n»), barra «N cambios por enviar» | | |
| P10 | Sincronizar | Quitar modo avión → ⟳ | Pendientes enviados, contador en 0 | | |
| P11 | Notificaciones | Aceptar / denegar el permiso | Aviso al sincronizar; si se deniega, la app sigue funcionando | | |
| P12 | Cerrar sesión | Cerrar sesión y entrar con otro usuario | No quedan datos del usuario anterior | | |
| P13 | Herramientas de depuración | Revisar Inicio | No aparecen «Probar acceso a reportes» ni «Pruebas de sesión» | | |
| P14 | Rotación y tema | Girar pantalla, cambiar tema del sistema | Sin pérdida de datos; colores correctos | | |
| P15 | Reinicio | Cerrar la app y abrirla | Sesión y datos locales conservados | | |

Resultado: ✅ pasa · ❌ falla (registrar defecto) · — no aplica.

## Registro de defectos

| Id | Caso | Dispositivo | Descripción | Severidad | Estado | Corrección / decisión |
|---|---|---|---|---|---|---|
| D-01 | | | | | | |
| D-02 | | | | | | |

Severidad: **Alta** (bloquea el flujo principal), **Media** (hay alternativa), **Baja** (visual). Solo se corrigen defectos de lo que ya está en el alcance; lo demás se declara.
