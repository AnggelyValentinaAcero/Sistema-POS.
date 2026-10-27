package com.pos.dto;

/**
 * Datos del negocio que aparecen en el encabezado del recibo.
 * Se guardan en la tabla configuracion (clave / valor).
 */
public class NegocioDTO {

    private String nombre;
    private String documento;
    private String direccion;
    private String telefono;
    private String simboloMoneda;
    private String mensajePie;

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDocumento() {
        return documento;
    }

    public void setDocumento(String documento) {
        this.documento = documento;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getSimboloMoneda() {
        return simboloMoneda;
    }

    public void setSimboloMoneda(String simboloMoneda) {
        this.simboloMoneda = simboloMoneda;
    }

    public String getMensajePie() {
        return mensajePie;
    }

    public void setMensajePie(String mensajePie) {
        this.mensajePie = mensajePie;
    }
}
