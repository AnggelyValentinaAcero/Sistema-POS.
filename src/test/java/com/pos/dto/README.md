# Pruebas: `dto`

Pruebas unitarias con **JUnit 5** (sin base de datos). Se ejecutan con `.\mvnw.cmd test`.

## Archivos

| Archivo | Qué prueba |
|---|---|
| `ResumenVentasDTOTest.java` | Los indicadores que calcula `ResumenVentasDTO` para el dashboard de reportes. |

## Pruebas

| Prueba | Caso | Resultado esperado |
|---|---|---|
| `calculaReciboPromedioYMargen` | 2 ventas, total 71.40, impuestos 6.40, ganancia 18.00 | venta neta 65.00 · recibo promedio 35.70 · margen 27.7 % |
| `sinVentasNoDivideEntreCero` | Un período sin ventas | recibo promedio 0 y margen 0, sin error de división entre cero |

## Parte clave del código

```java
@Test
void calculaReciboPromedioYMargen() {
    ResumenVentasDTO r = new ResumenVentasDTO();
    r.setCantidadVentas(2);
    r.setTotalVendido(new BigDecimal("71.40"));
    r.setTotalImpuestos(new BigDecimal("6.40"));
    r.setGanancia(new BigDecimal("18.00"));

    assertEquals(new BigDecimal("65.00"), r.getVentaNeta());          // 71.40 - 6.40
    assertEquals(new BigDecimal("35.70"), r.getReciboPromedio());     // 71.40 / 2
    assertEquals(new BigDecimal("27.7"), r.getMargenPorcentaje());    // 18 / 65 = 27.69 %
}
```

Son los mismos números que se verificaron a mano contra la base de datos real.
