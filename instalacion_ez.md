# Guía Rápida de Instalación (Para no desarrolladores) 🚀

Esta guía está diseñada paso a paso para cualquier persona que **no sea programadora** y quiera probar el **Sistema POS** en una computadora con Windows (10 u 11).

No necesitas saber de programación ni instalar Java (el programa ya lo incluye). Solo necesitamos preparar la base de datos (MySQL) una sola vez.

---

## 📌 Antes de empezar: Las 3 contraseñas

Para no confundirte, ten claro que durante este proceso verás 3 contraseñas:

| Contraseña | ¿Quién la usa? | Ejemplo que te sugerimos usar |
|---|---|---|
| **1. Contraseña de MySQL (`root`)** | La que inventas al instalar MySQL en tu PC | `Admin1234` *(¡anótala!)* |
| **2. Contraseña de conexión (`pos_app`)** | La clave técnica para que el programa lea los datos | `PosApp2026` |
| **3. Contraseña de ingreso al POS (`admin`)** | Para entrar a vender en el sistema | Viene de fábrica como `admin` |

> ⚠️ **Regla de oro:** No uses tildes (á, é, í) ni la letra `ñ` en ninguna contraseña para evitar errores de conexión en Windows.

---

## Paso 1: Instalar MySQL (El motor de datos)

El sistema necesita MySQL para guardar los productos, ventas y usuarios.

### 1.1 Descargar MySQL
1. Entra a este enlace oficial: **[Descargar MySQL Server](https://dev.mysql.com/downloads/mysql/)**
2. Donde dice *Operating System*, asegúrate de que esté en **Microsoft Windows**.
3. Haz clic en el botón azul de descarga **Download** en la opción **Windows (x86, 64-bit), MSI Installer**.
4. En la pantalla siguiente verás que te pide iniciar sesión; **no crees cuenta**, solo haz clic abajo en el texto gris:  
   👉 *"No thanks, just start my download"* (No gracias, solo iniciar mi descarga).

*(Opcional: Si el instalador te dice que te falta "Visual C++", descárgalo gratis directamente de Microsoft aquí: [Instalador Visual C++](https://aka.ms/vs/17/release/vc_redist.x64.exe), instálalo y vuelve a abrir el de MySQL).*

---

### 1.2 Instalar y Configurar MySQL
1. Abre el archivo descargado (`.msi`).
2. Acepta los términos de la licencia y pulsa **Next**.
3. Elige la instalación **Typical** (Típica) y presiona **Install**.
4. Al terminar, deja marcada la casilla **"Run MySQL Configurator"** y haz clic en **Finish**.

Se abrirá una ventana para configurar MySQL. Solo debes pulsar **Next** (Siguiente) y prestar atención a estas 2 pantallas:

* **Pantalla "Accounts and Roles":**
  Aquí defines la contraseña maestra (`root`). Escribe una clave sencilla y **anótala en un papel** (por ejemplo: `Admin1234`).
* **Pantalla "Apply Configuration":**
  Haz clic en el botón **Execute**. Verás cómo se marcan puntos verdes ✔️. Al terminar pulsa **Finish**.

¡Listo! MySQL ya está funcionando en tu computadora.

---

## Paso 2: Crear la Base de Datos del Sistema

Ahora le diremos a MySQL que cree las tablas del POS (productos, caja, inventario).

1. Abre el menú Inicio de Windows y busca: **MySQL Command Line Client** (haz clic en él para abrirlo).
2. Se abrirá una ventana negra que te dice: `Enter password:`.  
   👉 **Escribe la contraseña que inventaste en el Paso 1** (ejemplo: `Admin1234`) y presiona `Enter`.  
   *(Nota: Por seguridad, en esa ventana negra no se ven las letras al escribir, tú solo escríbela y pulsa Enter).*
3. Una vez dentro (verás que dice `mysql>`), copia y pega el siguiente bloque de texto tal cual:

```sql
CREATE DATABASE IF NOT EXISTS pos_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'pos_app'@'localhost' IDENTIFIED BY 'PosApp2026';
GRANT ALL PRIVILEGES ON pos_db.* TO 'pos_app'@'localhost';
FLUSH PRIVILEGES;
```
*(Presiona `Enter` para asegurarte de que se ejecute).*

4. Ahora cargaremos las tablas del sistema:
   * Copia el archivo `base_datos.sql` (que te pasaron junto con el programa) en tu disco local `C:\` (para que quede en la ruta `C:\base_datos.sql`).
   * En la misma ventana negra de MySQL escribe exactamente esto y presiona `Enter`:
     ```sql
     source C:/base_datos.sql;
     ```
     *(Fíjate que las barras van hacia adelante `/`).*
   * Verás que pasan varias líneas diciendo `Query OK`.
5. Escribe `exit` y presiona `Enter` para cerrar la ventana. ¡La base de datos ya está lista!

---

## Paso 3: Abrir el Sistema POS

1. Toma la carpeta completa **`SistemaPOS`** (la que te compartieron) y cópiala en tu disco `C:\` (por ejemplo en `C:\SistemaPOS`).  
   *(Evita ponerla en el Escritorio si usas OneDrive para evitar bloqueos).*
2. Entra a la carpeta y haz doble clic en el archivo **`SistemaPOS.exe`**.

> 💡 **Si Windows muestra una pantalla azul ("Windows protegió su PC"):**  
> Esto es normal la primera vez porque es un software desarrollado a medida y no tiene firma comercial comprada a Microsoft.  
> Haz clic en **Más información** y luego en el botón **Ejecutar de todas formas**.

---

## Paso 4: Conectar la pantalla inicial (Solo la primera vez)

Al abrir el programa por primera vez, verás una ventana llamada **Conexión a la base de datos**. Rellénala así:

| Campo | Qué poner |
|---|---|
| **Servidor** | `localhost` |
| **Puerto** | `3306` |
| **Base de datos** | `pos_db` |
| **Usuario** | `pos_app` *(o `root` si lo prefieres)* |
| **Contraseña** | `PosApp2026` *(o la clave de root del Paso 1)* |

1. Haz clic en el botón **Probar conexión**.
2. Te saldrá un mensaje verde: **"¡Conexión exitosa!"**.
3. Haz clic en **Guardar y conectar**.

---

## Paso 5: Iniciar Sesión

Aparecerá la pantalla de Login del Sistema POS:

1. **Usuario:** `admin`
2. **Contraseña:** `admin`
3. Haz clic en **Ingresar**.
4. Como es el primer inicio, el sistema te pedirá cambiar tu contraseña por seguridad. Escribe una nueva (por ejemplo `Admin2026`) y confirma.

🎉 **¡Felicidades! Ya estás dentro del Sistema POS.**  
Desde el menú lateral ya puedes acceder al **Punto de Venta**, abrir tu **Caja**, ver el **Inventario**, registrar productos y realizar ventas.

---

## ❓ Preguntas Frecuentes y Solución de Problemas

* **¿Dice "No se pudo conectar con el servidor"?**  
  Asegúrate de haber instalado MySQL en el Paso 1. Si reiniciaste tu PC, MySQL normalmente arranca solo, pero puedes verificar que esté activo buscando `Servicios` en Windows y viendo que `MySQL` diga *En ejecución*.
* **¿Dice "Usuario o contraseña incorrectos" en la ventana de conexión?**  
  Asegúrate de escribir la misma contraseña que definiste en el Paso 2 (`PosApp2026`). Si tienes dudas, puedes poner de usuario `root` y la contraseña que pusiste al instalar MySQL.
* **¿No abre el `.exe`?**  
  Asegúrate de haber copiado toda la carpeta `SistemaPOS` entera (con las carpetas `app` y `runtime` adentro), no solamente el archivo `.exe` suelto.
