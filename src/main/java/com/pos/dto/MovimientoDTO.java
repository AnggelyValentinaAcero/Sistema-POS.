package com.pos.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Un movimiento del kardex: cada vez que el stock de un producto sube o baja.
 */
public class MovimientoDTO {

    private long id;
    private int productoId;
    private String producto;           // código y nombre del producto
    private String tipo;               // VENTA, ANULACION, ENTRADA o AJUSTE
    private BigDecimal cantidad;       // positiva = entra, negativa = sale
    private BigDecimal stockResultante;
    private BigDecimal costoUnitario;
    private Long ventaId;
    private String usuario;
    private String motivo;
    private LocalDateTime fecha;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getProductoId() {
        return productoId;
    }

    public void setProductoId(int productoId) {
        this.productoId = productoId;
    }

    public String getProducto() {
        return producto;
    }

    public void setProducto(String producto) {
        this.producto = producto;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public void setCantidad(BigDecimal cantidad) {
        this.cantidad = cantidad;
    }

    public BigDecimal getStockResultante() {
        return stockResultante;
    }

    public void setStockResultante(BigDecimal stockResultante) {
        this.stockResultante = stockResultante;
    }

    public BigDecimal getCostoUnitario() {
        return costoUnitario;
    }

    public void setCostoUnitario(BigDecimal costoUnitario) {
        this.costoUnitario = costoUnitario;
    }

    public Long getVentaId() {
        return ventaId;
    }

    public void setVentaId(Long ventaId) {
        this.ventaId = ventaId;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    // Getters de apoyo para la tabla del kardex

    public String getFechaTexto() {
        return fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    /** Entradas con signo + para que se distingan de las salidas. */
    public String getCantidadTexto() {
        String texto = cantidad.stripTrailingZeros().toPlainString();
        return cantidad.signum() > 0 ? "+" + texto : texto;
    }

    public String getStockResultanteTexto() {
        return stockResultante.stripTrailingZeros().toPlainString();
    }

    public String getDetalle() {
        if (ventaId != null) {
            return "Venta N° " + ventaId;
        }
        return motivo == null ? "" : motivo;
    }
}
