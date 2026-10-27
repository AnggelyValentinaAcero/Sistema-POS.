package com.pos.util;

import org.mindrot.jbcrypt.BCrypt;

import java.security.SecureRandom;

/**
 * Funciones para manejar contraseñas.
 * Las contraseñas nunca se guardan en texto plano: se guarda su hash BCrypt,
 * que no se puede revertir. Para validar un login se compara la clave escrita
 * contra el hash guardado.
 */
public class Claves {

    public static final int LONGITUD_MINIMA = 6;

    // Letras y números sin caracteres que se confunden (0/O, 1/l/I)
    private static final String CARACTERES = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";

    public static String generarHash(String clave) {
        return BCrypt.hashpw(clave, BCrypt.gensalt());
    }

    public static boolean verificar(String clave, String hash) {
        if (clave == null || hash == null) {
            return false;
        }
        return BCrypt.checkpw(clave, hash);
    }

    /**
     * Revisa que la contraseña cumpla las reglas mínimas.
     * Devuelve el mensaje de error, o null si la contraseña es válida.
     */
    public static String validar(String clave, String nombreUsuario) {
        if (clave == null || clave.length() < LONGITUD_MINIMA) {
            return "La contraseña debe tener al menos " + LONGITUD_MINIMA + " caracteres.";
        }
        if (clave.equalsIgnoreCase(nombreUsuario)) {
            return "La contraseña no puede ser igual al nombre de usuario.";
        }
        return null;
    }

    /** Genera una contraseña temporal de 8 caracteres para restablecer el acceso de un usuario. */
    public static String generarTemporal() {
        // SecureRandom genera números aleatorios difíciles de predecir (adecuado para contraseñas)
        SecureRandom random = new SecureRandom();
        String temporal = "";
        for (int i = 0; i < 8; i++) {
            temporal += CARACTERES.charAt(random.nextInt(CARACTERES.length()));
        }
        return temporal;
    }
}
