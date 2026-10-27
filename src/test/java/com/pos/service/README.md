# Pruebas: `service`

Pruebas unitarias con **JUnit 5**. No necesitan base de datos: prueban cálculos puros. Se
ejecutan con `.\mvnw.cmd test`.

## Archivos

| Archivo | Qué prueba |
|---|---|
| `VentaServiceTest.java` | `VentaService.calcularTotales()`: cómo se separan la base y el impuesto cuando el precio ya incluye IVA. |

## Pruebas

| Prueba | Caso | Resultado esperado |
|---|---|---|
| `separaLaBaseYElImpuestoDelPrecio` | 1 producto a 116.00 con IVA 16 % | total 116.00 · base 100.00 · impuesto 16.00 |
| `sumaVariasLineasConDistintoImpuesto` | 2 × 58.00 (IVA 16 %) + 3 × 10.00 (exento) | total 146.00 · base 130.00 · impuesto 16.00 |
| `redondeaCantidadesDecimales` | 0.755 kg × 3.99 = 3.01245 | total 3.01 (redondeo `HALF_UP`) |

## Parte clave del código

```java
private DetalleVentaDTO linea(String cantidad, String precio, String porcentajeImpuesto) {
    DetalleVentaDTO d = new DetalleVentaDTO();
    d.setCantidad(new BigDecimal(cantidad));
    d.setPrecioUnitario(new BigDecimal(precio));
    d.setPorcentajeImpuesto(new BigDecimal(porcentajeImpuesto));
    return d;
}

@Test
void separaLaBaseYElImpuestoDelPrecio() {
    VentaDTO venta = new VentaDTO();
    venta.getDetalles().add(linea("1", "116.00", "16"));

    ventaService.calcularTotales(venta);

    assertEquals(new BigDecimal("116.00"), venta.getTotal());
    assertEquals(new BigDecimal("100.00"), venta.getSubtotal());
    assertEquals(new BigDecimal("16.00"), venta.getImpuesto());
}
```

Cada prueba sigue el esquema **preparar → ejecutar → verificar**. `linea(...)` es un método de
ayuda para armar una línea de venta en una sola instrucción. `assertEquals(esperado, obtenido)`
hace fallar la prueba si los valores no coinciden.
