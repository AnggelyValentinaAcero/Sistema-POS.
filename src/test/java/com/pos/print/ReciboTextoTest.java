package com.pos.print;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReciboTextoTest {

    @Test
    void centraElTextoConEspacios() {
        ReciboTexto t = new ReciboTexto(20);
        t.centrado("HOLA");
        List<String> lineas = t.getLineas();
        assertEquals(1, lineas.size());
        assertEquals("        HOLA", lineas.get(0)); // 8 espacios + 4 letras = 12 (centrado en 20)
    }

    @Test
    void alineaColumnasALosExtremos() {
        ReciboTexto t = new ReciboTexto(10);
        t.columnas("A", "B");
        assertEquals("A        B", t.getLineas().get(0)); // 1 + 8 espacios + 1 = 10
    }

    @Test
    void divideTextoLargoPorEspacios() {
        ReciboTexto t = new ReciboTexto(10);
        t.texto("uno dos tres cuatro");
        List<String> lineas = t.getLineas();
        // "uno dos", "tres", "cuatro" (ninguna supera 10 caracteres)
        assertEquals(3, lineas.size());
        assertEquals("uno dos", lineas.get(0));
        assertEquals("tres", lineas.get(1));
        assertEquals("cuatro", lineas.get(2));
    }

    @Test
    void cortaPalabraMasLargaQueElAncho() {
        ReciboTexto t = new ReciboTexto(5);
        t.texto("abcdefghij");
        List<String> lineas = t.getLineas();
        assertEquals(2, lineas.size());
        assertEquals("abcde", lineas.get(0));
        assertEquals("fghij", lineas.get(1));
    }

    @Test
    void separadorLlenaElAncho() {
        ReciboTexto t = new ReciboTexto(16);
        t.separador();
        assertEquals("----------------", t.getLineas().get(0));
    }
}
