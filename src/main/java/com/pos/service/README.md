# Paquete `service`: lógica de negocio y transacciones

Los services contienen **las reglas del negocio**: quién puede hacer qué, qué datos son válidos y
qué operaciones deben ir juntas en una transacción. Están entre los controllers (la pantalla) y
los DAO (la base de datos).

```
Controller  →  Service  (1. verifica permiso  2. valida datos  3. transacción con los DAO)  →  DAO
```

Las reglas están aquí y no en las pantallas: si mañana se hiciera una versión web, todas las
validaciones seguirían en el mismo lugar.

## Archivos

| Archivo | Responsabilidad | Permiso |
|---|---|---|
| `NegocioException.java` | Excepción para errores de reglas de negocio ("usuario duplicado", "stock insuficiente"…). Su mensaje se muestra tal cual al usuario. | — |
| `AuthService.java` | Inicio de sesión (BCrypt + carga de permisos), cierre de sesión y cambio de contraseña. | — |
| `UsuarioService.java` | Crear y editar usuarios, restablecer contraseñas, y la regla de que siempre quede un administrador activo. | `USUARIOS_GESTIONAR` |
| `ProductoService.java` | Productos (crear con stock inicial, editar, activar/desactivar, auditar cambios de precio) y categorías. | `PRODUCTOS_VER` / `PRODUCTOS_GESTIONAR` |
| `ClienteService.java` | Crear y editar clientes; documento único. | `CLIENTES_GESTIONAR` |
| `ProveedorService.java` | Crear y editar proveedores; documento único. | `PROVEEDORES_GESTIONAR` |
| `CajaService.java` | Abrir y cerrar turnos de caja; calcular el efectivo esperado y la diferencia. | `CAJA_OPERAR` |
| `VentaService.java` | **El corazón del sistema**: calcular totales, registrar la venta en una transacción, anular ventas e historial. | `VENTAS_REALIZAR` / `VENTAS_ANULAR` / `VENTAS_HISTORIAL` |
| `InventarioService.java` | Entradas de mercadería, ajustes, conteo físico, kardex y stock bajo. | `INVENTARIO_GESTIONAR` |
| `ReciboService.java` | Arma los recibos (venta y cierre de caja) y los envía a la impresora configurada. | — |
| `ReporteService.java` | Indicadores y reportes del dashboard. | `REPORTES_VER` |
| `ConfiguracionService.java` | Leer y guardar los datos del negocio (tabla `configuracion`). | `CONFIGURACION_GESTIONAR` |
| `AuditoriaService.java` | Consulta del registro de auditoría (solo lectura). | `AUDITORIA_VER` |

## Partes clave del código

### El patrón de transacción (se repite en todos los services)

```java
Connection con = null;
try {
    con = Conexion.getConexion();
    con.setAutoCommit(false);      // 1. inicia la transacción
    ...                            // 2. varias operaciones con los DAO, usando la misma "con"
    con.commit();                  // 3. si todo salió bien, se guarda todo junto
} catch (SQLException | NegocioException e) {
    Conexion.rollback(con);        // si algo falló, se deshace todo
    throw e;
} finally {
    Conexion.cerrar(con);
}
```

Es la **atomicidad** de ACID: o se guarda todo o no se guarda nada. Las consultas simples, que solo
leen, usan `try (Connection con = Conexion.getConexion()) { ... }` sin transacción.

### `VentaService.registrarVenta()`: la venta como una sola transacción

Es exactamente lo que pide el documento del proyecto:

```java
con.setAutoCommit(false); // inicio de la transacción

TurnoCajaDTO turno = turnoDAO.buscarAbierto(con, Sesion.getTerminal());
if (turno == null) {
    throw new NegocioException("La caja de esta terminal está cerrada. Ábrala antes de vender.");
}

// Se vuelven a leer precio, costo e impuesto desde la base de datos.
// No se confía en los valores que vienen de la pantalla.
for (DetalleVentaDTO d : venta.getDetalles()) {
    ProductoDTO producto = productoDAO.buscarPorId(con, d.getProductoId());
    d.setPrecioUnitario(producto.getPrecioVenta());
    d.setCostoUnitario(producto.getPrecioCompra());
    ...
}
calcularTotales(venta);
validarPago(con, venta);

// 1. Encabezado (maestro)
long ventaId = ventaDAO.insertar(con, venta);

// 2. Detalle + stock + kardex
for (DetalleVentaDTO d : venta.getDetalles()) {
    boolean descontado = productoDAO.descontarStock(con, d.getProductoId(), d.getCantidad());
    if (!descontado) {
        throw new NegocioException("Stock insuficiente de " + d.getNombreProducto() + ...);
    }
    ventaDAO.insertarDetalle(con, ventaId, d);
    movimientoDAO.registrar(con, d.getProductoId(), "VENTA", d.getCantidad().negate(), ...);
}

// 3. Pago   4. Auditoría
ventaDAO.insertarPago(con, ventaId, venta.getPago());
auditoriaDAO.registrar(con, usuarioId, Sesion.getTerminal(), "VENTA_REGISTRADA", ...);

con.commit(); // todo salió bien: se confirman todos los cambios juntos
```

- **Atomicidad:** si se venden arroz y leche y la leche no tiene stock, el `rollback` deshace
  también el descuento del arroz y el encabezado de la venta.
- **Concurrencia:** `descontarStock` usa `UPDATE ... WHERE stock >= ?` (ver `dao/README.md`). Si
  dos cajas venden el último producto a la vez, solo una lo logra.
- **No confiar en la pantalla:** aunque alguien alterara el precio en memoria, se usa el de la
  base de datos.

### `VentaService.calcularTotales()`: precios con IVA incluido

```java
for (DetalleVentaDTO d : venta.getDetalles()) {
    BigDecimal totalLinea = d.getCantidad().multiply(d.getPrecioUnitario()).setScale(2, RoundingMode.HALF_UP);
    d.setSubtotal(totalLinea);

    BigDecimal divisor = BigDecimal.ONE.add(d.getPorcentajeImpuesto().divide(CIEN));
    BigDecimal baseLinea = totalLinea.divide(divisor, 2, RoundingMode.HALF_UP);

    total = total.add(totalLinea);
    base = base.add(baseLinea);
}
venta.setTotal(total);
venta.setSubtotal(base);
venta.setImpuesto(total.subtract(base));
```

El precio que ve el cliente ya incluye el impuesto. Se separa la base: `116.00 / 1.16 = 100.00`,
impuesto `16.00`. Lo prueba `src/test/java/com/pos/service/VentaServiceTest.java`.

### `AuthService.iniciarSesion()`: login seguro

```java
UsuarioDTO usuario = usuarioDAO.buscarPorNombreUsuario(con, nombreUsuario.trim());

// Se muestra el mismo mensaje si el usuario no existe o si la clave es incorrecta,
// así no se le revela a un atacante qué usuarios existen
String hash = null;
if (usuario != null) {
    hash = usuarioDAO.obtenerPasswordHash(con, usuario.getId());
}
if (usuario == null || !Claves.verificar(clave, hash)) {
    auditoriaDAO.registrar(con, id, terminal, "LOGIN_FALLIDO", "usuarios", null, "usuario: " + nombreUsuario);
    throw new NegocioException("Usuario o contraseña incorrectos.");
}

// Login correcto: se cargan los permisos del rol y se guarda la sesión
List<Permiso> permisos = new ArrayList<>();
for (String codigo : usuarioDAO.listarPermisosDeRol(con, usuario.getRolId())) {
    permisos.add(Permiso.valueOf(codigo));
}
Sesion.iniciar(usuario, permisos, terminal);
```

### Otras reglas de negocio importantes

| Dónde | Regla |
|---|---|
| `UsuarioService.actualizar` | Un administrador no puede desactivarse ni quitarse el rol a sí mismo, y siempre debe quedar al menos un administrador activo. |
| `ProductoService.crear` | El stock inicial se registra en el kardex (`ENTRADA` "Stock inicial") en la misma transacción. |
| `ProductoService.actualizar` | El stock no se edita; los cambios de precio se auditan como `PRECIO_MODIFICADO` con el valor anterior y el nuevo. |
| `ProductoService.validar` | El precio de venta debe ser mayor a 0 y no menor al de compra. |
| `CajaService.cerrarCaja` | Efectivo esperado = monto inicial + ventas en **efectivo**; diferencia = contado − esperado (negativa = faltante). |
| `VentaService.anularVenta` | Solo ventas de un turno **abierto**. Devuelve el stock (`ANULACION` en el kardex) y no borra la venta: la marca como `ANULADA`. |
| `InventarioService.registrarConteo` | Calcula la diferencia entre lo contado y el sistema dentro de la transacción, con la fila bloqueada (`FOR UPDATE`). |
| `ReciboService.reciboVenta` | Si es una reimpresión, el recibo lo indica y queda auditado. |
| `ReporteService.ventasPorDia` | Completa con 0 los días sin ventas para que el gráfico no salte días. |
