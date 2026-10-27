package com.pos.util;

import com.pos.dto.UsuarioDTO;

import java.util.ArrayList;
import java.util.List;

/**
 * Guarda los datos del usuario que inició sesión en esta caja.
 * Usa atributos static porque solo hay una sesión abierta a la vez por terminal.
 */
public class Sesion {

    private static UsuarioDTO usuario;
    private static List<Permiso> permisos = new ArrayList<>();
    private static String terminal;

    public static void iniciar(UsuarioDTO usuarioLogueado, List<Permiso> permisosDelRol, String nombreTerminal) {
        usuario = usuarioLogueado;
        permisos = permisosDelRol;
        terminal = nombreTerminal;
    }

    public static void cerrar() {
        usuario = null;
        permisos = new ArrayList<>();
    }

    public static boolean haySesion() {
        return usuario != null;
    }

    public static boolean tienePermiso(Permiso permiso) {
        return permisos.contains(permiso);
    }

    public static UsuarioDTO getUsuario() {
        return usuario;
    }

    public static String getTerminal() {
        return terminal;
    }
}
