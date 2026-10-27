package com.pos.dao;

import com.pos.dto.AuditoriaDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Registro y consulta de auditoría en MySQL: quién hizo qué acción y cuándo.
 */
public class AuditoriaDAO {

    public void registrar(Connection con, Integer usuarioId, String terminal, String accion,
                          String entidad, String entidadId, String detalle) throws SQLException {
        String sql = "INSERT INTO auditoria (usuario_id, terminal, accion, entidad, entidad_id, detalle) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            // el usuario puede ser null, por ejemplo en un login fallido con un usuario que no existe
            if (usuarioId == null) {
                ps.setNull(1, Types.INTEGER);
            } else {
                ps.setInt(1, usuarioId);
            }
            ps.setString(2, terminal);
            ps.setString(3, accion);
            ps.setString(4, entidad);
            ps.setString(5, entidadId);
            ps.setString(6, detalle);
            ps.executeUpdate();
        }
    }

    /**
     * Busca registros entre dos fechas. Los filtros usuarioId, accion y texto
     * son opcionales (null = sin filtro). Devuelve como máximo "limite" filas, las más recientes primero.
     */
    public List<AuditoriaDTO> listar(Connection con, LocalDate desde, LocalDate hasta, Integer usuarioId,
                                     String accion, String texto, int limite) throws SQLException {
        // La consulta se arma según los filtros elegidos. Solo se agregan trozos fijos de SQL;
        // los valores siempre van como parámetros (?), así que no hay riesgo de inyección SQL.
        String sql = "SELECT a.id, a.fecha, u.nombre_usuario, a.terminal, a.accion, a.entidad, a.entidad_id, a.detalle "
                + "FROM auditoria a LEFT JOIN usuarios u ON u.id = a.usuario_id "
                + "WHERE a.fecha >= ? AND a.fecha < ? ";
        if (usuarioId != null) {
            sql += "AND a.usuario_id = ? ";
        }
        if (accion != null) {
            sql += "AND a.accion = ? ";
        }
        if (texto != null) {
            sql += "AND a.detalle LIKE ? ";
        }
        sql += "ORDER BY a.id DESC LIMIT ?";

        List<AuditoriaDTO> lista = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            int i = 1;
            ps.setTimestamp(i++, Timestamp.valueOf(desde.atStartOfDay()));
            ps.setTimestamp(i++, Timestamp.valueOf(hasta.plusDays(1).atStartOfDay()));
            if (usuarioId != null) {
                ps.setInt(i++, usuarioId);
            }
            if (accion != null) {
                ps.setString(i++, accion);
            }
            if (texto != null) {
                ps.setString(i++, "%" + texto + "%");
            }
            ps.setInt(i, limite);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AuditoriaDTO a = new AuditoriaDTO();
                    a.setId(rs.getLong("id"));
                    a.setFecha(rs.getTimestamp("fecha").toLocalDateTime());
                    // si el usuario es null (login fallido de un usuario inexistente) se muestra "-"
                    String usuario = rs.getString("nombre_usuario");
                    a.setUsuario(usuario == null ? "-" : usuario);
                    a.setTerminal(rs.getString("terminal"));
                    a.setAccion(rs.getString("accion"));
                    a.setEntidad(rs.getString("entidad"));
                    a.setEntidadId(rs.getString("entidad_id"));
                    a.setDetalle(rs.getString("detalle"));
                    lista.add(a);
                }
            }
        }
        return lista;
    }

    /** Las distintas acciones registradas (para llenar el filtro). */
    public List<String> listarAcciones(Connection con) throws SQLException {
        List<String> acciones = new ArrayList<>();
        String sql = "SELECT DISTINCT accion FROM auditoria ORDER BY accion";
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                acciones.add(rs.getString("accion"));
            }
        }
        return acciones;
    }
}
