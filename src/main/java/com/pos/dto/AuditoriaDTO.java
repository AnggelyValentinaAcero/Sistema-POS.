package com.pos.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Un registro de auditoría: quién hizo qué, cuándo y desde qué caja.
 */
public class AuditoriaDTO {

    private long id;
    private LocalDateTime fecha;
    private String usuario;
    private String terminal;
    private String accion;
    private String entidad;
    private String entidadId;
    private String detalle;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getTerminal() {
        return terminal;
    }

    public void setTerminal(String terminal) {
        this.terminal = terminal;
    }

    public String getAccion() {
        return accion;
    }

    public void setAccion(String accion) {
        this.accion = accion;
    }

    public String getEntidad() {
        return entidad;
    }

    public void setEntidad(String entidad) {
        this.entidad = entidad;
    }

    public String getEntidadId() {
        return entidadId;
    }

    public void setEntidadId(String entidadId) {
        this.entidadId = entidadId;
    }

    public String getDetalle() {
        return detalle;
    }

    public void setDetalle(String detalle) {
        this.detalle = detalle;
    }

    public String getFechaTexto() {
        return fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
    }

    /** Ej: "productos #15" */
    public String getReferencia() {
        if (entidad == null) {
            return "";
        }
        return entidadId == null ? entidad : entidad + " #" + entidadId;
    }
}
