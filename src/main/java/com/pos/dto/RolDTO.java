package com.pos.dto;

/**
 * DTO de un rol (ADMINISTRADOR, SUPERVISOR, CAJERO).
 */
public class RolDTO {

    public static final String ADMINISTRADOR = "ADMINISTRADOR";

    private int id;
    private String nombre;
    private String descripcion;

    public RolDTO() {
    }

    public RolDTO(int id, String nombre, String descripcion) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
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

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    // El ComboBox de roles muestra lo que devuelve toString()
    @Override
    public String toString() {
        return nombre;
    }
}
