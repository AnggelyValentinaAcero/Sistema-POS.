package com.pos.dto;

/**
 * DTO de una categoría de productos (ej: Bebidas, Lácteos, Ferretería).
 */
public class CategoriaDTO {

    private int id;
    private String nombre;
    private String descripcion;
    private boolean activo;

    public CategoriaDTO() {
        this.activo = true;
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

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public String getEstado() {
        return activo ? "Activa" : "Inactiva";
    }

    // Se usa para mostrar la categoría en los ComboBox
    @Override
    public String toString() {
        return nombre;
    }
}
