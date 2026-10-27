package com.pos.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;

/**
 * Funciones de apoyo para textos:
 * - formato de montos para mostrar en pantalla;
 * - limpieza y validación de los textos que ingresa el usuario (las usan los services).
 */
public class Textos {

    // se carga desde la configuración del negocio al iniciar sesión
    private static String simbolo = "$";

    // ---- Formato de montos ----

    public static void setSimbolo(String nuevoSimbolo) {
        simbolo = nuevoSimbolo;
    }

    /** Ej: 1234.5 -> "$ 1,234.50" */
    public static String moneda(BigDecimal valor) {
        if (valor == null) {
            valor = BigDecimal.ZERO;
        }
        DecimalFormat formato = new DecimalFormat("#,##0.00");
        return simbolo + " " + formato.format(valor);
    }

    // ---- Limpieza y validación ----

    /**
     * Quita los espacios al inicio y al final. Si el texto queda vacío devuelve null,
     * así en la base de datos se guarda NULL en lugar de "" (importante para las
     * columnas UNIQUE, como el documento: dos clientes sin documento no deben chocar).
     */
    public static String limpiar(String texto) {
        if (texto == null) {
            return null;
        }
        texto = texto.trim();
        if (texto.isEmpty()) {
            return null;
        }
        return texto;
    }

    public static boolean estaVacio(String texto) {
        return texto == null || texto.trim().isEmpty();
    }

    /** Validación sencilla de email: algo@algo.algo */
    public static boolean esEmailValido(String email) {
        return email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");
    }
}
