# Paquete `dao`: acceso a la base de datos

Un **DAO** (*Data Access Object*) contiene las consultas SQL de una entidad. Es la **única capa
que habla con MySQL**: los services nunca escriben SQL y los controllers nunca llaman a un DAO.

```
Service  →  DAO (PreparedStatement)  →  MySQL
        ←  DTO (lleno con el ResultSet) ←
```

Todos los métodos **reciben la `Connection` como parámetro**. Así el service puede usar la misma
conexión para varias operaciones dentro de **una sola transacción** (todo o nada).

## Archivos

| Archivo | Tablas | Métodos principales |
|---|---|---|
| `UsuarioDAO.java` | `usuarios`, `roles`, `permisos`, `rol_permiso` | `buscarPorNombreUsuario`, `obtenerPasswordHash`, `insertar`, `actualizar`, `actualizarPassword`, `registrarAcceso`, `contarAdministradoresActivos`, `listarRoles`, `buscarRolPorId`, `listarPermisosDeRol` |
| `ProductoDAO.java` | `productos`, `impuestos` | `buscar`, `buscarPorId`, `buscarPorCodigo`, `insertar`, `actualizar`, `cambiarEstado`, **`descontarStock`**, `aumentarStock`, `obtenerStock`, `actualizarPrecioCompra`, `listarStockBajo`, `listarImpuestos` |
| `CategoriaDAO.java` | `categorias` | `listar`, `buscarPorNombre`, `insertar`, `actualizar` |
| `ProveedorDAO.java` | `proveedores` | `buscar`, `buscarPorDocumento`, `insertar`, `actualizar` |
| `ClienteDAO.java` | `clientes` | `buscar`, `buscarPorDocumento`, `insertar`, `actualizar` |
| `VentaDAO.java` | `ventas`, `detalle_ventas`, `pagos`, `metodos_pago` | `insertar` (maestro), `insertarDetalle`, `insertarPago`, `buscarPorId`, `anular`, `listar`, `listarDetalles`, `buscarPago`, `listarMetodosPago`, `buscarMetodoPago` |
| `TurnoCajaDAO.java` | `turnos_caja` | `buscarAbierto`, `abrir`, `cerrar`, `obtenerEstado`, `totalesPorMetodoPago`, `contarVentas` |
| `MovimientoInventarioDAO.java` | `movimientos_inventario` (kardex) | `registrar`, `listar`, `listarPorProducto` |
| `AuditoriaDAO.java` | `auditoria` | `registrar`, `listar` (con filtros opcionales), `listarAcciones` |
| `ConfiguracionDAO.java` | `configuracion` (clave/valor) | `obtenerTodo`, `guardar` |
| `ReporteDAO.java` | `ventas`, `detalle_ventas`, `pagos`… (solo lectura) | `resumen`, `ventasPorDia`, `productosMasVendidos`, `ventasPorCajero`, `ventasPorMetodoPago` |

Las tablas pequeñas que solo se leen para llenar listas no tienen DAO propio: roles y permisos
están en `UsuarioDAO`, métodos de pago en `VentaDAO` e impuestos en `ProductoDAO`.

## Partes clave del código

### `PreparedStatement`: protección contra inyección SQL

```java
// UsuarioDAO.buscarPorNombreUsuario
String sql = SELECT_USUARIO + "WHERE u.nombre_usuario = ?";
try (PreparedStatement ps = con.prepareStatement(sql)) {
    ps.setString(1, nombreUsuario);          // el valor viaja aparte, nunca se concatena
    try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
            return leerUsuario(rs);          // convierte la fila en un UsuarioDTO
        }
    }
}
return null;                                 // no existe
```

- El `?` es un parámetro. Si alguien escribe `' OR '1'='1` en el login, MySQL busca
  literalmente un usuario con ese nombre: **no hay inyección SQL**.
- *try-with-resources* (`try (...)`) cierra el `PreparedStatement` y el `ResultSet`
  automáticamente, aunque haya un error.

### Patrón repetido: consulta base + método `leerXxx`

```java
private static final String SELECT_USUARIO =
        "SELECT u.id, u.nombre_usuario, u.nombre_completo, u.rol_id, r.nombre AS rol, ... "
        + "FROM usuarios u INNER JOIN roles r ON r.id = u.rol_id ";

private UsuarioDTO leerUsuario(ResultSet rs) throws SQLException {
    UsuarioDTO u = new UsuarioDTO();
    u.setId(rs.getInt("id"));
    u.setNombreUsuario(rs.getString("nombre_usuario"));
    ...
    return u;
}
```

`buscarPorId`, `buscarPorNombreUsuario` y `listar` reutilizan la misma consulta base y cambian
solo el `WHERE` o el `ORDER BY`.

### `ProductoDAO.descontarStock()`: el método más importante para la concurrencia

```java
public boolean descontarStock(Connection con, int id, BigDecimal cantidad) throws SQLException {
    // La condición "stock >= ?" hace que MySQL solo descuente si alcanza.
    // Como el UPDATE es atómico, si dos cajas venden el último producto al mismo
    // tiempo, solo una lo logra: a la otra le afecta 0 filas.
    String sql = "UPDATE productos SET stock = stock - ? WHERE id = ? AND stock >= ?";
    try (PreparedStatement ps = con.prepareStatement(sql)) {
        ps.setBigDecimal(1, cantidad);
        ps.setInt(2, id);
        ps.setBigDecimal(3, cantidad);
        int filasAfectadas = ps.executeUpdate();
        return filasAfectadas == 1;          // false = no había stock suficiente
    }
}
```

No se usa "leer el stock, comparar en Java y luego actualizar", porque entre la lectura y la
actualización otra caja podría vender. Aquí la comprobación y el descuento ocurren en una sola
instrucción de MySQL.

### `ProductoDAO.obtenerStock()`: `SELECT ... FOR UPDATE`

```java
String sql = "SELECT stock FROM productos WHERE id = ? FOR UPDATE";
```

Dentro de una transacción, InnoDB lee una "foto" tomada al inicio. `FOR UPDATE` obliga a leer el
valor **actual** y bloquea la fila hasta el `commit`. Se usa para registrar en el kardex el stock
resultante correcto.

### `Statement.RETURN_GENERATED_KEYS`: obtener el id nuevo

```java
// VentaDAO.insertar
try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
    ...
    ps.executeUpdate();
    try (ResultSet rs = ps.getGeneratedKeys()) {
        rs.next();
        return rs.getLong(1);                // el id AUTO_INCREMENT que asignó MySQL
    }
}
```

El id de la venta (maestro) se necesita para insertar sus líneas de detalle.

### `VentaDAO.anular()`: evitar anular dos veces

```java
String sql = "UPDATE ventas SET estado = 'ANULADA', anulada_por = ?, fecha_anulacion = NOW(), "
        + "motivo_anulacion = ? WHERE id = ? AND estado = 'COMPLETADA'";
...
return ps.executeUpdate() == 1;              // 0 filas = otra persona ya la anuló
```

### Consultas con filtros opcionales (`AuditoriaDAO.listar`, `MovimientoInventarioDAO.listar`)

```java
String sql = "SELECT ... FROM auditoria a ... WHERE a.fecha >= ? AND a.fecha < ? ";
if (usuarioId != null) {
    sql += "AND a.usuario_id = ? ";          // se agrega un trozo FIJO de SQL
}
...
int i = 1;
ps.setTimestamp(i++, ...);
if (usuarioId != null) {
    ps.setInt(i++, usuarioId);               // el VALOR sigue siendo un parámetro
}
```

Se concatenan solo trozos de SQL escritos por el programador, nunca datos del usuario. Por eso
sigue siendo seguro.

### `ReporteDAO`: los cálculos los hace MySQL

```java
// Ganancia de una línea = precio sin impuesto - costo (valores históricos)
private static final String GANANCIA_LINEA =
        "(d.subtotal / (1 + d.porcentaje_impuesto / 100) - d.costo_unitario * d.cantidad)";

// Solo cuentan las ventas completadas (no las anuladas)
private static final String FILTRO_VENTAS = "v.estado = 'COMPLETADA' AND v.fecha >= ? AND v.fecha < ? ";
```

Con `SUM`, `COUNT` y `GROUP BY`, MySQL devuelve los totales ya calculados. Es mucho más
eficiente que traer todas las ventas a Java y sumarlas en un `for`.
