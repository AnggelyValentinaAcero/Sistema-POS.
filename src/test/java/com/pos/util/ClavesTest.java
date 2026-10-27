package com.pos.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClavesTest {

    @Test
    void hashVerificaSoloLaClaveCorrecta() {
        String hash = Claves.generarHash("secreto1");
        assertNotEquals("secreto1", hash);
        assertTrue(Claves.verificar("secreto1", hash));
        assertFalse(Claves.verificar("secreto2", hash));
    }

    @Test
    void sinHashNuncaVerifica() {
        assertFalse(Claves.verificar("cualquiera", null));
    }

    @Test
    void validaLongitudYNombreUsuario() {
        assertNotNull(Claves.validar("123", "juan"));
        assertNotNull(Claves.validar("JuanPerez", "juanperez"));
        assertNull(Claves.validar("clave-segura", "juan"));
    }

    @Test
    void temporalTieneOchoCaracteresSinAmbiguos() {
        String temporal = Claves.generarTemporal();
        assertEquals(8, temporal.length());
        assertTrue(temporal.chars().noneMatch(ch -> "0O1lI".indexOf(ch) >= 0));
    }
}
