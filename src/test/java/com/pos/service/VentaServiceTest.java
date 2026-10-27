package com.pos.service;

import com.pos.dto.DetalleVentaDTO;
import com.pos.dto.VentaDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pruebas del cálculo de totales (no necesitan base de datos).
 */
class VentaServiceTest {

    private final VentaService ventaService = new VentaService();

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

    @Test
    void sumaVariasLineasConDistintoImpuesto() {
        VentaDTO venta = new VentaDTO();
        venta.getDetalles().add(linea("2", "58.00", "16"));   // 116.00 -> base 100.00
        venta.getDetalles().add(linea("3", "10.00", "0"));    //  30.00 -> exento

        ventaService.calcularTotales(venta);

        assertEquals(new BigDecimal("146.00"), venta.getTotal());
        assertEquals(new BigDecimal("130.00"), venta.getSubtotal());
        assertEquals(new BigDecimal("16.00"), venta.getImpuesto());
        assertEquals(new BigDecimal("116.00"), venta.getDetalles().get(0).getSubtotal());
    }

    @Test
    void redondeaCantidadesDecimales() {
        VentaDTO venta = new VentaDTO();
        venta.getDetalles().add(linea("0.755", "3.99", "0")); // 0.755 kg x 3.99 = 3.01245

        ventaService.calcularTotales(venta);

        assertEquals(new BigDecimal("3.01"), venta.getTotal());
    }
}
