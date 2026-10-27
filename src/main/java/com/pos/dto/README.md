# Paquete `dto`: objetos de transferencia de datos

Un **DTO** (*Data Transfer Object*) es una clase que solo transporta datos entre capas: tiene
atributos privados, un constructor vacío, getters y setters, y **no tiene lógica de negocio**.

```
MySQL  →  DAO (lee el ResultSet y llena un DTO)  →  Service  →  Controller  →  tabla en pantalla
```

## Archivos

| Archivo | Representa | Tabla(s) de MySQL |
|---|---|---|
| `UsuarioDTO.java` | Un usuario del personal. **No incluye el hash de la contraseña** (nunca sale del DAO/service). | `usuarios` + nombre del rol |
| `RolDTO.java` | Un rol: ADMINISTRADOR, SUPERVISOR o CAJERO. | `roles` |
| `ProductoDTO.java` | Un producto, con los nombres de su categoría, impuesto y proveedor. | `productos` + joins |
| `CategoriaDTO.java` | Una categoría de productos. | `categorias` |
| `ImpuestoDTO.java` | Un impuesto (IVA 16 %, Exento 0 %). | `impuestos` |
| `ProveedorDTO.java` | Un proveedor. | `proveedores` |
| `ClienteDTO.java` | Un cliente (no inicia sesión; solo se asocia a ventas). | `clientes` |
| `VentaDTO.java` | Una venta (**maestro**), con su lista de detalles y su pago. | `ventas` |
| `DetalleVentaDTO.java` | Una línea de la venta (**detalle**), con el precio y el costo *de ese momento*. | `detalle_ventas` |
| `PagoDTO.java` | El pago de una venta: método, monto, efectivo recibido y cambio. | `pagos` |
| `MetodoPagoDTO.java` | Efectivo, Tarjeta o Transferencia. | `metodos_pago` |
| `TurnoCajaDTO.java` | Un turno de caja: apertura, cierre, efectivo esperado, contado y diferencia. | `turnos_caja` |
| `MovimientoDTO.java` | Un movimiento del kardex (entrada, venta, ajuste, anulación). | `movimientos_inventario` |
| `NegocioDTO.java` | Datos del negocio para el encabezado del recibo. | `configuracion` (clave/valor) |
| `AuditoriaDTO.java` | Un registro de auditoría: quién, qué, cuándo y desde qué caja. | `auditoria` |
| `ResumenVentasDTO.java` | Indicadores del dashboard: ventas, total, impuestos, ganancia. | Calculado (`SUM`, `COUNT`) |
| `ItemReporteDTO.java` | Una fila de un reporte agrupado (un producto, un cajero, un día…). | Calculado (`GROUP BY`) |

## Partes clave del código

### Estructura típica de un DTO

```java
public class CategoriaDTO {

    private int id;
    private String nombre;
    private String descripcion;
    private boolean activo;

    public CategoriaDTO() {
        this.activo = true;
    }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    ...

    // Se usa para mostrar la categoría en los ComboBox
    @Override
    public String toString() {
        return nombre;
    }
}
```

`toString()` se sobrescribe en los DTO que aparecen en un `ComboBox` (categorías, impuestos,
roles…): el combo muestra lo que devuelve ese método.

### `BigDecimal` para el dinero, no `double`

```java
private BigDecimal precioCompra;
private BigDecimal precioVenta;
private BigDecimal stock;
```

`double` no puede representar exactamente valores como 0.1 (`0.1 + 0.2 = 0.30000000000000004`).
En un POS esos errores se acumulan y descuadran la caja. `BigDecimal` es exacto, igual que el
tipo `DECIMAL(12,2)` de MySQL. Para comparar se usa `compareTo`, no `equals`:

```java
public boolean isStockBajo() {
    return stock.compareTo(stockMinimo) <= 0;
}
```

### Maestro-detalle: `VentaDTO` contiene sus `DetalleVentaDTO`

```java
public class VentaDTO {
    private long id;
    private BigDecimal subtotal, impuesto, total;
    private List<DetalleVentaDTO> detalles;   // las líneas de la venta
    private PagoDTO pago;                     // cómo se pagó
    ...
}
```

`DetalleVentaDTO` guarda `precioUnitario` y `costoUnitario` **del momento de la venta**. Si después
cambia el precio del producto, la venta antigua no cambia, y los reportes de ganancia siguen
siendo correctos.

### Getters "de apoyo" para las tablas

Algunos DTO tienen getters que no corresponden a una columna; solo dan formato para mostrar:

```java
public String getEstado() {                // UsuarioDTO, ProductoDTO, ClienteDTO...
    return activo ? "Activo" : "Inactivo";
}

public String getCantidadTexto() {         // DetalleVentaDTO: 2 en lugar de 2.000
    return cantidad.stripTrailingZeros().toPlainString();
}
```

En la tabla se usan con `new PropertyValueFactory<>("estado")`, que llama a `getEstado()`.

### Indicadores calculados en `ResumenVentasDTO`

```java
public BigDecimal getReciboPromedio() {
    if (cantidadVentas == 0) {
        return BigDecimal.ZERO;            // evita la división entre cero
    }
    return totalVendido.divide(new BigDecimal(cantidadVentas), 2, RoundingMode.HALF_UP);
}
```

También calcula `getVentaNeta()` (total − impuestos) y `getMargenPorcentaje()`
(ganancia ÷ venta neta). Lo prueba `src/test/java/com/pos/dto/ResumenVentasDTOTest.java`.
