package com.pos.service;

import com.pos.config.Conexion;
import com.pos.dao.AuditoriaDAO;
import com.pos.dao.UsuarioDAO;
import com.pos.dto.RolDTO;
import com.pos.dto.UsuarioDTO;
import com.pos.util.Claves;
import com.pos.util.Permiso;
import com.pos.util.Sesion;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Lógica de negocio para administrar los usuarios del sistema.
 * Solo los usuarios con el permiso USUARIOS_GESTIONAR pueden usarla.
 */
public class UsuarioService {

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final AuditoriaDAO auditoriaDAO = new AuditoriaDAO();

    public List<UsuarioDTO> listar() throws NegocioException, SQLException {
        verificarPermiso();
        try (Connection con = Conexion.getConexion()) {
            return usuarioDAO.listar(con);
        }
    }

    public List<RolDTO> listarRoles() throws NegocioException, SQLException {
        verificarPermiso();
        try (Connection con = Conexion.getConexion()) {
            return usuarioDAO.listarRoles(con);
        }
    }

    public void crear(UsuarioDTO usuario, String clave) throws NegocioException, SQLException {
        verificarPermiso();
        validarDatos(usuario);
        if (!usuario.getNombreUsuario().matches("[A-Za-z0-9._-]{3,50}")) {
            throw new NegocioException("El usuario debe tener entre 3 y 50 caracteres (letras, números, punto o guion).");
        }
        String errorClave = Claves.validar(clave, usuario.getNombreUsuario());
        if (errorClave != null) {
            throw new NegocioException(errorClave);
        }

        Connection con = null;
        try {
            con = Conexion.getConexion();
            con.setAutoCommit(false); // inicio de la transacción

            if (usuarioDAO.buscarPorNombreUsuario(con, usuario.getNombreUsuario()) != null) {
                throw new NegocioException("Ya existe un usuario llamado '" + usuario.getNombreUsuario() + "'.");
            }
            // el usuario nuevo debe cambiar la contraseña la primera vez que ingrese
            usuario.setDebeCambiarPassword(true);
            int id = usuarioDAO.insertar(con, usuario, Claves.generarHash(clave));
            registrarAuditoria(con, "USUARIO_CREADO", id, "Se creó el usuario: " + usuario.getNombreUsuario()
                    + " (" + usuario.getNombreCompleto() + ")");

            con.commit(); // se guardan el usuario y la auditoría juntos
        } catch (SQLException | NegocioException e) {
            Conexion.rollback(con); // si algo falló no se guarda nada
            throw e;
        } finally {
            Conexion.cerrar(con);
        }
    }

    public void actualizar(UsuarioDTO usuario) throws NegocioException, SQLException {
        verificarPermiso();
        validarDatos(usuario);

        Connection con = null;
        try {
            con = Conexion.getConexion();
            con.setAutoCommit(false);

            UsuarioDTO anterior = usuarioDAO.buscarPorId(con, usuario.getId());
            RolDTO rolNuevo = usuarioDAO.buscarRolPorId(con, usuario.getRolId());

            // ¿El usuario era administrador activo y deja de serlo?
            boolean eraAdmin = anterior.isActivo() && anterior.getRol().equals(RolDTO.ADMINISTRADOR);
            boolean sigueAdmin = usuario.isActivo() && rolNuevo.getNombre().equals(RolDTO.ADMINISTRADOR);
            if (eraAdmin && !sigueAdmin) {
                if (usuario.getId() == Sesion.getUsuario().getId()) {
                    throw new NegocioException("No puede desactivar ni quitar el rol de administrador a su propio usuario.");
                }
                if (usuarioDAO.contarAdministradoresActivos(con) <= 1) {
                    throw new NegocioException("Debe quedar al menos un administrador activo.");
                }
            }

            usuarioDAO.actualizar(con, usuario);
            registrarAuditoria(con, "USUARIO_MODIFICADO", usuario.getId(),
                    "Se actualizó el usuario ID: " + usuario.getId() + " (" + usuario.getNombreUsuario()
                    + "). Rol: " + anterior.getRol() + " -> " + rolNuevo.getNombre() + ", activo: " + usuario.isActivo());

            con.commit();
        } catch (SQLException | NegocioException e) {
            Conexion.rollback(con);
            throw e;
        } finally {
            Conexion.cerrar(con);
        }
    }

    /**
     * Asigna una contraseña temporal al usuario y lo obliga a cambiarla al ingresar.
     * Devuelve la contraseña temporal para que el administrador se la entregue.
     */
    public String restablecerPassword(int usuarioId) throws NegocioException, SQLException {
        verificarPermiso();
        String temporal = Claves.generarTemporal();
        try (Connection con = Conexion.getConexion()) {
            usuarioDAO.actualizarPassword(con, usuarioId, Claves.generarHash(temporal), true);
            registrarAuditoria(con, "PASSWORD_RESTABLECIDA", usuarioId, null);
        }
        return temporal;
    }

    private void verificarPermiso() throws NegocioException {
        if (!Sesion.tienePermiso(Permiso.USUARIOS_GESTIONAR)) {
            throw new NegocioException("No tiene permiso para administrar usuarios.");
        }
    }

    private void validarDatos(UsuarioDTO usuario) throws NegocioException {
        if (usuario.getNombreCompleto() == null || usuario.getNombreCompleto().trim().isEmpty()) {
            throw new NegocioException("Ingrese el nombre completo.");
        }
        if (usuario.getRolId() <= 0) {
            throw new NegocioException("Seleccione un rol.");
        }
    }

    private void registrarAuditoria(Connection con, String accion, int usuarioAfectado, String detalle) throws SQLException {
        auditoriaDAO.registrar(con, Sesion.getUsuario().getId(), Sesion.getTerminal(), accion,
                "usuarios", String.valueOf(usuarioAfectado), detalle);
    }
}
