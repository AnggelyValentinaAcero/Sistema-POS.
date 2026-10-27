package com.pos.dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResumenVentasDTOTest {

    @Test
    void calculaReciboPromedioYMargen() {
        ResumenVentasDTO r = new ResumenVentasDTO();
        r.setCantidadVentas(2);
        r.setTotalVendido(new BigDecimal("71.40"));
        r.setTotalImpuestos(new BigDecimal("6.40"));
        r.setGanancia(new BigDecimal("18.00"));

        assertEquals(new BigDecimal("65.00"), r.getVentaNeta());
        assertEquals(new BigDecimal("35.70"), r.getReciboPromedio());
        assertEquals(new BigDecimal("35.70"), r.getTicketPromedio());
        assertEquals(new BigDecimal("27.7"), r.getMargenPorcentaje()); // 18 / 65 = 27.69 %
    }

    @Test
    void sinVentasNoDivideEntreCero() {
        ResumenVentasDTO r = new ResumenVentasDTO();
        assertEquals(BigDecimal.ZERO, r.getReciboPromedio());
        assertEquals(BigDecimal.ZERO, r.getMargenPorcentaje());
    }
}
