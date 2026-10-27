package com.pos.dto;

import java.math.BigDecimal;

/**
 * Una fila de un reporte agrupado: un producto, un cajero, un día o un método de pago,
 * con sus totales.
 */
public class ItemReporteDTO {

    private String nombre;
    private BigDecimal cantidad;  // unidades vendidas o número de ventas, según el reporte
    private BigDecimal total;
    private BigDecimal ganancia;

    public ItemReporteDTO() {
        this.cantidad = BigDecimal.ZERO;
        this.total = BigDecimal.ZERO;
        this.ganancia = BigDecimal.ZERO;
    }

    public ItemReporteDTO(String nombre, BigDecimal cantidad, BigDecimal total, BigDecimal ganancia) {
        this.nombre = nombre;
        this.cantidad = cantidad;
        this.total = total;
        this.ganancia = ganancia;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public void setCantidad(BigDecimal cantidad) {
        this.cantidad = cantidad;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public BigDecimal getGanancia() {
        return ganancia;
    }

    public void setGanancia(BigDecimal ganancia) {
        this.ganancia = ganancia;
    }

    public String getCantidadTexto() {
        return cantidad.stripTrailingZeros().toPlainString();
    }
}
