package com.pos.dto;

public class MetodoPagoDTO {

    public static final String EFECTIVO = "Efectivo";

    private int id;
    private String nombre;

    public MetodoPagoDTO() {
    }

    public MetodoPagoDTO(int id, String nombre) {
        this.id = id;
        this.nombre = nombre;
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

    public boolean isEfectivo() {
        return EFECTIVO.equals(nombre);
    }

    @Override
    public String toString() {
        return nombre;
    }
}
