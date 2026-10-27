# Instalación en una computadora nueva

Guía para instalar el Sistema POS en una PC con Windows 10/11 que **no tiene nada instalado**:
ni Java, ni MySQL, ni el proyecto.

**Resumen:**
1. Preparar los archivos en tu PC de desarrollo.
2. Instalar MySQL en la PC nueva.
3. Crear la base de datos.
4. Copiar y ejecutar `SistemaPOS.exe`.
5. Configuración inicial.

Tiempo aproximado: 30 minutos.

> La PC nueva **no necesita Java**: el `.exe` trae su propio Java incluido.
> Si la PC nueva será solo una **caja** que se conecta a un MySQL que ya está en otra computadora
> de la red, sáltate los pasos 2 y 3 y en el paso 4 escribe la IP de ese servidor.

---

## Paso 1. Preparar los archivos (en tu PC de desarrollo)

1. Genera el ejecutable actualizado:
   ```
   scripts\crear_ejecutable.bat
   ```
2. Copia a un USB (o a la nube) estas dos cosas:

   | Qué | Dónde está | Tamaño |
   |---|---|---|
   | La carpeta **completa** `SistemaPOS` | `dist\SistemaPOS\` | ~170 MB |
   | El script de la base de datos | `database\base_datos.sql` | pequeño |

   ⚠️ Copia la carpeta entera, no solo el `.exe`: necesita las subcarpetas `app\` y `runtime\`.

---

## Paso 2. Instalar MySQL (en la PC nueva)

### 2.1 Descargar
1. Entra a **https://dev.mysql.com/downloads/mysql/**.
2. Elige la versión **8.4 LTS** (o la 8.0) y el sistema **Microsoft Windows**.
3. Descarga **"Windows (x86, 64-bit), MSI Installer"**.
4. Te ofrecerá iniciar sesión: pulsa **"No thanks, just start my download"**.

Opcional, pero recomendado para ver los datos: descarga también **MySQL Workbench** desde
**https://dev.mysql.com/downloads/workbench/**.

### 2.2 Instalar
1. Ejecuta el `.msi` descargado → **Next** → acepta la licencia → tipo **Typical** → **Install**.
   - Si pide instalar **Microsoft Visual C++ Redistributable**, instálalo (te da el enlace) y
     vuelve a ejecutar el `.msi`.
2. Al terminar, deja marcado **"Run MySQL Configurator"** y pulsa **Finish**.

### 2.3 Configurar (MySQL Configurator)
Avanza con **Next** en cada pantalla y revisa estos valores:

| Pantalla | Qué elegir |
|---|---|
| Data Directory | Dejar el valor por defecto |
| Type and Networking | Config Type: **Development Computer** · Port: **3306** · TCP/IP marcado · "Open Windows Firewall port" marcado (solo si habrá otras cajas en la red) |
| Accounts and Roles | **Contraseña de `root`**: elígela y **anótala** (sin tildes ni ñ) |
| Windows Service | Marcado **"Configure MySQL Server as a Windows Service"** y **"Start the MySQL Server at System Startup"** |
| Server File Permissions / Sample Databases | Dejar por defecto / no instalar ejemplos |
| Apply Configuration | **Execute** → todo en verde ✔ → **Next** → **Finish** |

> Con MySQL 8.0, el instalador se llama **MySQL Installer** y tiene las mismas opciones
> (elige "Server only" o "Custom" con Server + Workbench).

### 2.4 Comprobar que MySQL está funcionando
- `Win + R` → `services.msc` → busca **MySQL84** (o MySQL80): debe decir **En ejecución**.

---

## Paso 3. Crear la base de datos

Elige **una** de las dos formas.

### Opción A: con MySQL Workbench (gráfico)
1. Abre **MySQL Workbench** → en la pantalla de inicio haz clic en **Local instance MySQL**
   (o crea una conexión: host `localhost`, puerto `3306`, usuario `root`).
2. Escribe la contraseña de `root`.
3. **File → Open SQL Script...** → elige `base_datos.sql` (del USB).
4. Ejecuta **todo** el script con el botón del rayo ⚡ (o `Ctrl + Shift + Enter`).
5. Abajo, en **Output**, todas las líneas deben tener ✔ verde.
6. Abre una pestaña nueva (**File → New Query Tab**), pega esto, **cambia la contraseña** y
   ejecútalo:
   ```sql
   CREATE USER 'pos_app'@'localhost' IDENTIFIED BY 'EscribeTuClave2026';
   GRANT ALL PRIVILEGES ON pos_db.* TO 'pos_app'@'localhost';
   FLUSH PRIVILEGES;
   ```
   Usa una contraseña **sin tildes ni ñ** y anótala: la pedirá la aplicación.

### Opción B: con la consola (sin Workbench)
1. Copia `base_datos.sql` a `C:\pos\base_datos.sql`.
2. Abre **Símbolo del sistema** (`cmd`) y ejecuta (ajusta `8.4` si instalaste otra versión):
   ```
   cd "C:\Program Files\MySQL\MySQL Server 8.4\bin"
   mysql -u root -p < C:\pos\base_datos.sql
   ```
   Escribe la contraseña de `root`. Si no muestra errores, la base quedó creada.
3. Entra a la consola de MySQL y crea el usuario de la aplicación:
   ```
   mysql -u root -p
   ```
   ```sql
   CREATE USER 'pos_app'@'localhost' IDENTIFIED BY 'EscribeTuClave2026';
   GRANT ALL PRIVILEGES ON pos_db.* TO 'pos_app'@'localhost';
   FLUSH PRIVILEGES;
   exit
   ```

### Verificar
En Workbench o en la consola (`mysql -u pos_app -p`):
```sql
SELECT COUNT(*) FROM pos_db.permisos;   -- debe dar 13
SELECT nombre_usuario FROM pos_db.usuarios;   -- debe aparecer admin
```

---

## Paso 4. Ejecutar la aplicación

1. Copia la carpeta `SistemaPOS` del USB a la PC, por ejemplo en `C:\SistemaPOS\`.
   (Evita el Escritorio sincronizado con OneDrive.)
2. Entra a la carpeta y haz doble clic en **`SistemaPOS.exe`**.
3. **Aviso de Windows SmartScreen** ("Windows protegió su PC"): aparece porque el programa no
   está firmado digitalmente, es normal en un proyecto propio. Pulsa **Más información →
   Ejecutar de todas formas**. Solo pasa la primera vez.
4. Opcional: clic derecho en `SistemaPOS.exe` → **Enviar a → Escritorio (crear acceso directo)**.

### Ventana de conexión (solo la primera vez)

| Campo | Valor |
|---|---|
| Servidor | `localhost` (o la IP del servidor si MySQL está en otra PC) |
| Puerto | `3306` |
| Base de datos | `pos_db` |
| Usuario | `pos_app` |
| Contraseña | La que definiste en el paso 3 |

Pulsa **Probar conexión** → debe decir "Conexión exitosa". Luego **Guardar y conectar**.

Los datos se guardan en `C:\Users\<usuario de Windows>\sistema-pos.properties`. Si otro usuario de
Windows usa la misma PC, la primera vez también verá la ventana de conexión.

### Primer ingreso
- Usuario **`admin`**, contraseña **`admin`**.
- El sistema obliga a cambiar la contraseña. **Anótala.**

---

## Paso 5. Configuración inicial (como admin)

1. **Configuración** → completa los datos del negocio (nombre, RUC/NIT, dirección, teléfono,
   moneda, mensaje del recibo) → **Guardar**.
2. **Configuración** → impresora de esta terminal:
   - con impresora térmica de recibos: **"Impresora térmica (ESC/POS)"**, elige la impresora y el
     ancho del papel (58 u 80 mm);
   - con impresora normal o sin impresora: **"Impresora del sistema"** → **Microsoft Print to PDF**;
   - pulsa **Imprimir prueba**.
3. **Usuarios** → crea los cajeros y supervisores.
4. **Categorías** → **Productos** → carga el catálogo con su stock inicial.
5. **Caja** → abrir caja → ya se puede vender en **Punto de venta**.

---

## Problemas frecuentes

| Problema | Solución |
|---|---|
| "No se pudo conectar con el servidor" | El servicio MySQL no está iniciado: `services.msc` → MySQL84 → Iniciar. O el puerto no es 3306 |
| "Usuario o contraseña de MySQL incorrectos" | Revisa la contraseña de `pos_app`. Si tiene tildes o ñ, cámbiala con `ALTER USER 'pos_app'@'localhost' IDENTIFIED BY 'NuevaClave';` |
| "La base de datos no existe" | No se ejecutó `base_datos.sql` (paso 3) |
| El script falla con `Database exists` o `Table already exists` | Ya estaba creada. Para empezar de cero (**borra todo**): `DROP DATABASE pos_db;` y vuelve a ejecutar el script |
| `SistemaPOS.exe` no abre nada | Verifica que copiaste la carpeta completa (con `app\` y `runtime\`). Prueba fuera de OneDrive |
| El antivirus bloquea el `.exe` | Agrega la carpeta `C:\SistemaPOS` a las exclusiones del antivirus |
| Olvidé la contraseña de `admin` | Ver `CREDENCIALES.md`, sección 3 (en tu PC de desarrollo) |
| Una caja de la red no se conecta al servidor | Ver "Varias cajas en red" más abajo |

---

## Varias cajas en red

MySQL se instala **solo en una PC (el servidor)**. Las demás (cajas) solo necesitan la carpeta
`SistemaPOS` (paso 4).

En el **servidor**:
1. Ver su IP: `cmd` → `ipconfig` → "Dirección IPv4" (ej: `192.168.1.10`).
2. Crear el usuario para la red, como `root` (`192.168.1.%` = cualquier equipo de esa red):
   ```sql
   CREATE USER 'pos_app'@'192.168.1.%' IDENTIFIED BY 'EscribeTuClave2026';
   GRANT ALL PRIVILEGES ON pos_db.* TO 'pos_app'@'192.168.1.%';
   FLUSH PRIVILEGES;
   ```
3. Abrir el puerto 3306 en el firewall (PowerShell **como administrador**):
   ```powershell
   New-NetFirewallRule -DisplayName "MySQL POS" -Direction Inbound -Protocol TCP -LocalPort 3306 -Action Allow -Profile Private
   ```

En cada **caja**: en la ventana de conexión, en "Servidor", escribe la IP del servidor.

Ten en cuenta:
- Cada caja usa el nombre de su PC como terminal, así que cada una tiene su propio turno de caja.
- Si el servidor se apaga, ninguna caja puede vender.

---

## Respaldo de la base de datos (recomendado)

Para hacer un respaldo en la PC donde está MySQL (`cmd`). Cambia la fecha del nombre del archivo
cada vez:
```
mkdir C:\pos
cd "C:\Program Files\MySQL\MySQL Server 8.4\bin"
mysqldump -u root -p pos_db > C:\pos\respaldo_2026-10-24.sql
```
Para restaurar en una base vacía:
```
mysql -u root -p -e "CREATE DATABASE pos_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
mysql -u root -p pos_db < C:\pos\respaldo_2026-10-24.sql
```
