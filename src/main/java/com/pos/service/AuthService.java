package com.pos.service;

import com.pos.config.Conexion;
import com.pos.dao.AuditoriaDAO;
import com.pos.dao.UsuarioDAO;
import com.pos.dto.UsuarioDTO;
import com.pos.util.Claves;
import com.pos.util.Permiso;
import com.pos.util.Sesion;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Lógica de inicio de sesión, cierre de sesión y cambio de contraseña.
 */
public class AuthService {

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final AuditoriaDAO auditoriaDAO = new AuditoriaDAO();

    public UsuarioDTO iniciarSesion(String nombreUsuario, String clave, String terminal)
            throws NegocioException, SQLException {

        try (Connection con = Conexion.getConexion()) {
            UsuarioDTO usuario = usuarioDAO.buscarPorNombreUsuario(con, nombreUsuario.trim());

            // Se muestra el mismo mensaje si el usuario no existe o si la clave es incorrecta,
            // así no se le revela a un atacante qué usuarios existen
            String hash = null;
            if (usuario != null) {
                hash = usuarioDAO.obtenerPasswordHash(con, usuario.getId());
            }
            if (usuario == null || !Claves.verificar(clave, hash)) {
                Integer id = (usuario != null) ? usuario.getId() : null;
                auditoriaDAO.registrar(con, id, terminal, "LOGIN_FALLIDO", "usuarios", null, "usuario: " + nombreUsuario);
                throw new NegocioException("Usuario o contraseña incorrectos.");
            }

            if (!usuario.isActivo()) {
                auditoriaDAO.registrar(con, usuario.getId(), terminal, "LOGIN_FALLIDO", "usuarios", null, "usuario inactivo");
                throw new NegocioException("El usuario está desactivado. Contacte al administrador.");
            }

            // Login correcto: se cargan los permisos del rol y se guarda la sesión
            List<Permiso> permisos = new ArrayList<>();
            for (String codigo : usuarioDAO.listarPermisosDeRol(con, usuario.getRolId())) {
                permisos.add(Permiso.valueOf(codigo));
            }
            usuarioDAO.registrarAcceso(con, usuario.getId());
            auditoriaDAO.registrar(con, usuario.getId(), terminal, "LOGIN", "usuarios", String.valueOf(usuario.getId()), null);

            Sesion.iniciar(usuario, permisos, terminal);
            return usuario;
        }
    }

    public void cerrarSesion() {
        if (!Sesion.haySesion()) {
            return;
        }
        int id = Sesion.getUsuario().getId();
        try (Connection con = Conexion.getConexion()) {
            auditoriaDAO.registrar(con, id, Sesion.getTerminal(), "LOGOUT", "usuarios", String.valueOf(id), null);
        } catch (SQLException e) {
            // si falla la auditoría igual se cierra la sesión
            e.printStackTrace();
        }
        Sesion.cerrar();
    }

    public void cambiarPassword(String claveActual, String claveNueva) throws NegocioException, SQLException {
        UsuarioDTO usuario = Sesion.getUsuario();

        String error = Claves.validar(claveNueva, usuario.getNombreUsuario());
        if (error != null) {
            throw new NegocioException(error);
        }
        if (claveNueva.equals(claveActual)) {
            throw new NegocioException("La nueva contraseña debe ser distinta de la actual.");
        }

        try (Connection con = Conexion.getConexion()) {
            String hashActual = usuarioDAO.obtenerPasswordHash(con, usuario.getId());
            if (!Claves.verificar(claveActual, hashActual)) {
                throw new NegocioException("La contraseña actual no es correcta.");
            }
            usuarioDAO.actualizarPassword(con, usuario.getId(), Claves.generarHash(claveNueva), false);
            auditoriaDAO.registrar(con, usuario.getId(), Sesion.getTerminal(), "CAMBIO_PASSWORD", "usuarios",
                    String.valueOf(usuario.getId()), null);
        }
        usuario.setDebeCambiarPassword(false);
    }
}
