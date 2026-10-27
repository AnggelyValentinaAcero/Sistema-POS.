package com.pos.service;

/**
 * Excepción para errores de reglas de negocio (por ejemplo "usuario duplicado").
 * Su mensaje se muestra directamente al usuario.
 */
public class NegocioException extends Exception {

    public NegocioException(String mensaje) {
        super(mensaje);
    }
}
