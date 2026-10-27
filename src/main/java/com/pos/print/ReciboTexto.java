package com.pos.print;

import java.util.ArrayList;
import java.util.List;

/**
 * Arma un recibo como una lista de líneas de texto de ancho fijo.
 * Como las impresoras térmicas y matriciales usan letra monoespaciada (todas las letras
 * miden lo mismo), alinear es cuestión de agregar espacios.
 */
public class ReciboTexto {

    private final int ancho; // caracteres por línea (32 para 58 mm, 48 para 80 mm)
    private final List<String> lineas = new ArrayList<>();

    public ReciboTexto(int ancho) {
        this.ancho = ancho;
    }

    /**
     * Agrega texto alineado a la izquierda. Si es más largo que el ancho, lo divide
     * en varias líneas cortando en el último espacio (para no partir palabras).
     */
    public void texto(String texto) {
        if (texto == null || texto.isEmpty()) {
            return;
        }
        while (texto.length() > ancho) {
            int corte = texto.lastIndexOf(' ', ancho);
            if (corte <= 0) {
                corte = ancho; // una sola palabra más larga que la línea: se corta donde sea
            }
            lineas.add(texto.substring(0, corte).trim());
            texto = texto.substring(corte).trim();
        }
        lineas.add(texto);
    }

    public void centrado(String texto) {
        if (texto == null || texto.isEmpty()) {
            return;
        }
        if (texto.length() >= ancho) {
            texto(texto);
            return;
        }
        int espacios = (ancho - texto.length()) / 2;
        lineas.add(" ".repeat(espacios) + texto);
    }

    /** Texto a la izquierda y valor a la derecha, ej: "TOTAL               $ 25.00" */
    public void columnas(String izquierda, String derecha) {
        int espacioIzquierda = ancho - derecha.length() - 1;
        if (izquierda.length() > espacioIzquierda) {
            izquierda = izquierda.substring(0, espacioIzquierda);
        }
        int espacios = ancho - izquierda.length() - derecha.length();
        lineas.add(izquierda + " ".repeat(espacios) + derecha);
    }

    public void separador() {
        lineas.add("-".repeat(ancho));
    }

    public void lineaVacia() {
        lineas.add("");
    }

    public List<String> getLineas() {
        return lineas;
    }
}
