# Paquete `config`: conexión a MySQL y configuración local

Aquí está todo lo que la aplicación necesita **antes** de poder trabajar: con qué base de datos
conectarse y qué impresora usar en esta computadora.

## Archivos

| Archivo | Qué hace |
|---|---|
| `Conexion.java` | Abre las conexiones JDBC a MySQL. Toda la aplicación pide sus conexiones aquí. También traduce los errores de conexión a mensajes entendibles y tiene ayudas para `rollback` y cierre. |
| `ConfiguracionLocal.java` | Lee y guarda la configuración **propia de esta caja** en el archivo `C:\Users\<usuario>\sistema-pos.properties`: datos de MySQL, nombre de la terminal e impresora de recibos. |

> **¿Qué va en el archivo y qué en la base de datos?** Lo que es distinto en cada caja (a qué
> servidor conectarse, qué impresora tiene) va en el archivo local. Lo que comparten todas las
> cajas (nombre del negocio, RUC, mensaje del recibo) va en la tabla `configuracion` de MySQL.

## Partes clave del código

### `Conexion.getConexion()`: un solo lugar para conectarse

```java
public static Connection getConexion() throws SQLException {
    if (configuracion == null) {
        throw new SQLException("No se ha configurado la conexión a la base de datos");
    }
    return DriverManager.getConnection(configuracion.getUrl(), configuracion.getUsuario(), configuracion.getPassword());
}
```

- Cada operación pide una conexión nueva y la cierra al terminar, normalmente con
  *try-with-resources*: `try (Connection con = Conexion.getConexion()) { ... }`.
- La URL la arma `ConfiguracionLocal.getUrl()`:
  `jdbc:mysql://localhost:3306/pos_db?characterEncoding=UTF-8&allowPublicKeyRetrieval=true`.
  Para un servidor en red solo cambia `localhost` por su IP.

### `Conexion.traducirError()`: errores entendibles

```java
switch (e.getErrorCode()) {
    case 1045: return "Usuario o contraseña de MySQL incorrectos.";
    case 1044: return "El usuario de MySQL no tiene permisos sobre esa base de datos.";
    case 1049: return "La base de datos no existe. Ejecute el script database/base_datos.sql.";
    case 0:    return "No se pudo conectar con el servidor. Verifique que MySQL esté encendido ...";
    default:   return "Error de base de datos: " + e.getMessage();
}
```

Cada error de MySQL tiene un código numérico. En vez de mostrar el mensaje técnico, se muestra
uno que el usuario entiende.

### `Conexion.rollback()` y `Conexion.cerrar()`

Se usan en el `catch` y en el `finally` de todas las transacciones de los services:

```java
} catch (SQLException | NegocioException e) {
    Conexion.rollback(con);   // deshace todo lo hecho en la transacción
    throw e;
} finally {
    Conexion.cerrar(con);     // vuelve a autocommit y cierra la conexión
}
```

### `ConfiguracionLocal`: dos guardados separados

```java
public void guardarConexion() throws IOException {
    Properties props = leerArchivo();          // parte del archivo que ya existe
    props.setProperty("db.host", host);
    props.setProperty("db.puerto", String.valueOf(puerto));
    ...
    escribirArchivo(props);
}

public void guardarImpresora() throws IOException {
    Properties props = leerArchivo();
    props.setProperty("impresora.tipo", tipoImpresora);
    ...
    escribirArchivo(props);
}
```

- Cada método **relee el archivo** y solo cambia sus propias claves. Así, guardar la conexión no
  borra la impresora, y al revés.
- `Properties` es una clase de Java que lee y escribe archivos `clave=valor`.
- El archivo está en la carpeta del usuario y no junto al programa porque, instalado en
  "Archivos de programa", Windows no permite escribir en esa carpeta.
- `cargar()` nunca devuelve `null`: si es la primera vez, devuelve los valores por defecto
  (`localhost`, `3306`, `pos_db`) e `isConexionGuardada()` es `false`.

### `getCaracteresPorLinea()`

```java
public int getCaracteresPorLinea() {
    return anchoMm == 58 ? 32 : 48;
}
```

Una impresora de recibos de 58 mm imprime 32 caracteres por línea y una de 80 mm, 48. Este dato
lo usa `print/GeneradorRecibo` para armar el recibo con el ancho correcto.
