package com.pos.dao;

import com.pos.dto.RolDTO;
import com.pos.dto.UsuarioDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a datos de usuarios en MySQL, incluidos sus roles y permisos.
 * Todas las consultas usan PreparedStatement para evitar inyección SQL.
 */
public class UsuarioDAO {

    // Consulta base: une usuarios con roles para obtener también el nombre del rol
    private static final String SELECT_USUARIO =
            "SELECT u.id, u.nombre_usuario, u.nombre_completo, u.rol_id, r.nombre AS rol, "
            + "u.activo, u.debe_cambiar_password, u.ultimo_acceso "
            + "FROM usuarios u INNER JOIN roles r ON r.id = u.rol_id ";

    public UsuarioDTO buscarPorId(Connection con, int id) throws SQLException {
        String sql = SELECT_USUARIO + "WHERE u.id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return leerUsuario(rs);
                }
            }
        }
        return null;
    }

    public UsuarioDTO buscarPorNombreUsuario(Connection con, String nombreUsuario) throws SQLException {
        String sql = SELECT_USUARIO + "WHERE u.nombre_usuario = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombreUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return leerUsuario(rs);
                }
            }
        }
        return null;
    }

    public List<UsuarioDTO> listar(Connection con) throws SQLException {
        List<UsuarioDTO> lista = new ArrayList<>();
        String sql = SELECT_USUARIO + "ORDER BY u.nombre_usuario ASC, u.nombre_completo ASC";
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(leerUsuario(rs));
            }
        }
        return lista;
    }

    public String obtenerPasswordHash(Connection con, int id) throws SQLException {
        String sql = "SELECT password_hash FROM usuarios WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("password_hash");
                }
            }
        }
        return null;
    }

    public int insertar(Connection con, UsuarioDTO usuario, String passwordHash) throws SQLException {
        String sql = "INSERT INTO usuarios (nombre_usuario, nombre_completo, rol_id, password_hash, debe_cambiar_password) "
                + "VALUES (?, ?, ?, ?, ?)";
        // RETURN_GENERATED_KEYS permite obtener el id que MySQL asignó (AUTO_INCREMENT)
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, usuario.getNombreUsuario());
            ps.setString(2, usuario.getNombreCompleto());
            ps.setInt(3, usuario.getRolId());
            ps.setString(4, passwordHash);
            ps.setBoolean(5, usuario.isDebeCambiarPassword());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public void actualizar(Connection con, UsuarioDTO usuario) throws SQLException {
        String sql = "UPDATE usuarios SET nombre_completo = ?, rol_id = ?, activo = ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, usuario.getNombreCompleto());
            ps.setInt(2, usuario.getRolId());
            ps.setBoolean(3, usuario.isActivo());
            ps.setInt(4, usuario.getId());
            ps.executeUpdate();
        }
    }

    public void actualizarPassword(Connection con, int id, String passwordHash, boolean debeCambiar) throws SQLException {
        String sql = "UPDATE usuarios SET password_hash = ?, debe_cambiar_password = ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, passwordHash);
            ps.setBoolean(2, debeCambiar);
            ps.setInt(3, id);
            ps.executeUpdate();
        }
    }

    public void registrarAcceso(Connection con, int id) throws SQLException {
        String sql = "UPDATE usuarios SET ultimo_acceso = NOW() WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public int contarAdministradoresActivos(Connection con) throws SQLException {
        String sql = "SELECT COUNT(*) FROM usuarios u INNER JOIN roles r ON r.id = u.rol_id "
                + "WHERE u.activo = TRUE AND r.nombre = 'ADMINISTRADOR'";
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    // ---- Roles y permisos (tablas roles, permisos y rol_permiso) ----

    public List<RolDTO> listarRoles(Connection con) throws SQLException {
        List<RolDTO> roles = new ArrayList<>();
        String sql = "SELECT id, nombre, descripcion FROM roles ORDER BY id";
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                roles.add(new RolDTO(rs.getInt("id"), rs.getString("nombre"), rs.getString("descripcion")));
            }
        }
        return roles;
    }

    public RolDTO buscarRolPorId(Connection con, int id) throws SQLException {
        String sql = "SELECT id, nombre, descripcion FROM roles WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new RolDTO(rs.getInt("id"), rs.getString("nombre"), rs.getString("descripcion"));
                }
            }
        }
        return null;
    }

    /** Devuelve los códigos de permiso del rol (ej: "VENTAS_REALIZAR"). */
    public List<String> listarPermisosDeRol(Connection con, int rolId) throws SQLException {
        List<String> permisos = new ArrayList<>();
        // rol_permiso es la tabla intermedia de la relación muchos a muchos entre roles y permisos
        String sql = "SELECT p.codigo FROM rol_permiso rp "
                + "INNER JOIN permisos p ON p.id = rp.permiso_id "
                + "WHERE rp.rol_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, rolId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    permisos.add(rs.getString("codigo"));
                }
            }
        }
        return permisos;
    }

    /** Convierte la fila actual del ResultSet en un objeto UsuarioDTO. */
    private UsuarioDTO leerUsuario(ResultSet rs) throws SQLException {
        UsuarioDTO u = new UsuarioDTO();
        u.setId(rs.getInt("id"));
        u.setNombreUsuario(rs.getString("nombre_usuario"));
        u.setNombreCompleto(rs.getString("nombre_completo"));
        u.setRolId(rs.getInt("rol_id"));
        u.setRol(rs.getString("rol"));
        u.setActivo(rs.getBoolean("activo"));
        u.setDebeCambiarPassword(rs.getBoolean("debe_cambiar_password"));

        Timestamp ultimoAcceso = rs.getTimestamp("ultimo_acceso");
        if (ultimoAcceso != null) {
            u.setUltimoAcceso(ultimoAcceso.toLocalDateTime());
        }
        return u;
    }
}
