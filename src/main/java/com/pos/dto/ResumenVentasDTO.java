package com.pos.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Indicadores principales de un período (los "KPI" del dashboard).
 */
public class ResumenVentasDTO {

    private int cantidadVentas;
    private BigDecimal totalVendido;   // con impuestos (lo que pagaron los clientes)
    private BigDecimal totalImpuestos;
    private BigDecimal ganancia;       // (precio sin impuesto - costo) x cantidad

    public ResumenVentasDTO() {
        this.totalVendido = BigDecimal.ZERO;
        this.totalImpuestos = BigDecimal.ZERO;
        this.ganancia = BigDecimal.ZERO;
    }

    public int getCantidadVentas() {
        return cantidadVentas;
    }

    public void setCantidadVentas(int cantidadVentas) {
        this.cantidadVentas = cantidadVentas;
    }

    public BigDecimal getTotalVendido() {
        return totalVendido;
    }

    public void setTotalVendido(BigDecimal totalVendido) {
        this.totalVendido = totalVendido;
    }

    public BigDecimal getTotalImpuestos() {
        return totalImpuestos;
    }

    public void setTotalImpuestos(BigDecimal totalImpuestos) {
        this.totalImpuestos = totalImpuestos;
    }

    public BigDecimal getGanancia() {
        return ganancia;
    }

    public void setGanancia(BigDecimal ganancia) {
        this.ganancia = ganancia;
    }

    /** Venta sin impuestos (lo que realmente le queda al negocio antes de costos). */
    public BigDecimal getVentaNeta() {
        return totalVendido.subtract(totalImpuestos);
    }

    /** Promedio gastado por cliente en cada compra (valor promedio de recibo). */
    public BigDecimal getReciboPromedio() {
        if (cantidadVentas == 0) {
            return BigDecimal.ZERO;
        }
        return totalVendido.divide(new BigDecimal(cantidadVentas), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal getTicketPromedio() {
        return getReciboPromedio();
    }

    /** Porcentaje de ganancia sobre la venta neta. */
    public BigDecimal getMargenPorcentaje() {
        if (getVentaNeta().signum() == 0) {
            return BigDecimal.ZERO;
        }
        return ganancia.multiply(new BigDecimal("100")).divide(getVentaNeta(), 1, RoundingMode.HALF_UP);
    }
}
