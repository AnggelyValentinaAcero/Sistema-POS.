package com.pos.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * DTO (Data Transfer Object) de un usuario del personal.
 * Solo transporta datos entre capas; no tiene lógica de negocio.
 * Por seguridad no incluye el hash de la contraseña.
 */
public class UsuarioDTO {

    private int id;
    private String nombreUsuario;
    private String nombreCompleto;
    private int rolId;
    private String rol;
    private boolean activo;
    private boolean debeCambiarPassword;
    private LocalDateTime ultimoAcceso;

    public UsuarioDTO() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public int getRolId() {
        return rolId;
    }

    public void setRolId(int rolId) {
        this.rolId = rolId;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public boolean isDebeCambiarPassword() {
        return debeCambiarPassword;
    }

    public void setDebeCambiarPassword(boolean debeCambiarPassword) {
        this.debeCambiarPassword = debeCambiarPassword;
    }

    public LocalDateTime getUltimoAcceso() {
        return ultimoAcceso;
    }

    public void setUltimoAcceso(LocalDateTime ultimoAcceso) {
        this.ultimoAcceso = ultimoAcceso;
    }

    // Getters de apoyo para mostrar los datos en la tabla de usuarios

    public String getEstado() {
        return activo ? "Activo" : "Inactivo";
    }

    public String getUltimoAccesoTexto() {
        if (ultimoAcceso == null) {
            return "Nunca";
        }
        return ultimoAcceso.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    // Se usa para mostrar el usuario en los ComboBox
    @Override
    public String toString() {
        return nombreUsuario;
    }
}
