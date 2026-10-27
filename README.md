# Sistema POS

Sistema de Punto de Venta de escritorio hecho en **Java (JavaFX)** con base de datos **MySQL**.

Arquitectura: **MVC** (vistas FXML + controladores), una capa de **services** con la lógica de negocio
y una capa de persistencia con los patrones **DAO** y **DTO** usando JDBC con `PreparedStatement`.

## Funcionalidades

- **Seguridad:**
  - login con contraseñas cifradas (BCrypt);
  - roles Administrador, Supervisor y Cajero, con permisos por módulo;
  - gestión de usuarios.
- **Catálogo:** productos (código de barras, impuesto, stock mínimo), categorías, proveedores y clientes.
- **Punto de venta:**
  - escaneo de códigos de barras y cobro en efectivo (con cálculo del cambio), tarjeta o transferencia;
  - **cada venta se guarda en una transacción ACID**.
- **Caja:** apertura y cierre por turno, con efectivo esperado contra contado (faltante o sobrante).
- **Recibos:** impresoras térmicas (ESC/POS) o cualquier impresora de Windows/PDF, reimpresión y
  comprobante de cierre.
- **Inventario:** entradas de mercadería, ajustes con motivo, conteo físico, kardex y alertas de
  stock bajo.
- **Reportes:** ventas, ganancia real, recibo promedio, productos más vendidos, métodos de pago y
  ventas por cajero.
- **Control:**
  - anulación de ventas (devuelve el stock);
  - auditoría de todas las acciones sensibles (precios, caja, anulaciones, accesos).

## Requisitos

- JDK 21 o superior (solo para compilar; el ejecutable generado incluye su propio Java)
- MySQL 8

No hace falta instalar Maven, porque el proyecto incluye el wrapper (`mvnw`).

## Instalación de la base de datos

1. Abrir `database/base_datos.sql` en MySQL Workbench y ejecutarlo con el usuario `root`.
   El script crea la base `pos_db`, las tablas y los datos iniciales.
2. Crear un usuario de MySQL para la aplicación (las instrucciones están al inicio del script).
   Use una contraseña sin acentos ni ñ.
3. Ejecutar la aplicación. La primera vez pide los datos de conexión (servidor local o remoto) y los
   guarda en `C:\Users\<usuario>\sistema-pos.properties`.
4. Ingresar con **admin / admin**. El sistema pide cambiar la contraseña en el primer ingreso.

## Comandos

Los pasos explicados con más calma están en `COMPILACION.md`.

```bash
mvnw.cmd javafx:run              # ejecutar la aplicación
mvnw.cmd test                    # ejecutar las pruebas
mvnw.cmd package                 # generar target/sistema-pos-0.1.0-SNAPSHOT.jar
scripts\crear_ejecutable.bat     # generar dist\SistemaPOS\SistemaPOS.exe (con Java incluido)
scripts\crear_ejecutable.bat exe # generar un instalador .exe (requiere WiX Toolset 3)
```

`crear_ejecutable.bat` usa `jpackage` del JDK. Lo busca en `JAVA_HOME`, en el PATH y en
`C:\Program Files\Java\jdk-*`. Si el JDK está en otra carpeta, defina `JAVA_HOME`.

## Estructura

```
database/base_datos.sql     script de la base de datos
database/datos_prueba.sql   datos de ejemplo para la demostración
scripts/                    generación del ejecutable de Windows
src/main/java/com/pos
 ├─ app/          arranque y apertura de ventanas
 ├─ config/       configuración y conexión a MySQL
 ├─ dto/          objetos de transferencia de datos
 ├─ dao/          clases DAO (JDBC con PreparedStatement)
 ├─ service/      lógica de negocio y transacciones
 ├─ controller/   controladores de las vistas
 ├─ print/        armado del recibo e impresión (ESC/POS y Windows)
 └─ util/         sesión, permisos, contraseñas, alertas
src/main/resources
 ├─ fxml/         vistas
 └─ css/          estilos
```

Cada carpeta de código (`src/main/java/com/pos/*` y `src/test/java/com/pos/*`) tiene su propio
`README.md`. Explica qué hace cada archivo `.java` de esa carpeta y muestra sus partes de código
más importantes.

## Avance

| Fase | Contenido | Estado |
|---|---|---|
| 0 | Proyecto base, estructura | ✅ |
| 1 | Base de datos y ventana de conexión | ✅ |
| 2 | Login, roles, permisos y gestión de usuarios | ✅ |
| 3 | Productos, categorías, proveedores, clientes | ✅ |
| 4 | Punto de venta, cobro, transacción de venta, caja, historial | ✅ |
| 5 | Impresión de recibos, cierre de caja y configuración | ✅ |
| 6 | Inventario: entradas, ajustes, conteo físico, kardex, stock bajo | ✅ |
| 7 | Dashboard: KPIs, ganancia, ventas por día, top productos, métodos de pago, cajeros | ✅ |
| 8 | Anulación de ventas, visor de auditoría, ejecutable de Windows | ✅ |
