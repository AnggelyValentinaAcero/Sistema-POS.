package com.pos.dto;

import java.math.BigDecimal;

/**
 * DTO de un impuesto (ej: IVA 16%, Exento 0%).
 */
public class ImpuestoDTO {

    private int id;
    private String nombre;
    private BigDecimal porcentaje;

    public ImpuestoDTO() {
    }

    public ImpuestoDTO(int id, String nombre, BigDecimal porcentaje) {
        this.id = id;
        this.nombre = nombre;
        this.porcentaje = porcentaje;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public BigDecimal getPorcentaje() {
        return porcentaje;
    }

    public void setPorcentaje(BigDecimal porcentaje) {
        this.porcentaje = porcentaje;
    }

    @Override
    public String toString() {
        return nombre + " (" + porcentaje.stripTrailingZeros().toPlainString() + "%)";
    }
}
