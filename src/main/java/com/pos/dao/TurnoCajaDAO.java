package com.pos.dao;

import com.pos.dto.TurnoCajaDTO;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

public class TurnoCajaDAO {

    public TurnoCajaDTO buscarAbierto(Connection con, String terminal) throws SQLException {
        String sql = "SELECT t.id, t.terminal, t.usuario_id, u.nombre_completo, t.fecha_apertura, t.monto_inicial "
                + "FROM turnos_caja t INNER JOIN usuarios u ON u.id = t.usuario_id "
                + "WHERE t.terminal = ? AND t.estado = 'ABIERTO'";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, terminal);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    TurnoCajaDTO t = new TurnoCajaDTO();
                    t.setId(rs.getInt("id"));
                    t.setTerminal(rs.getString("terminal"));
                    t.setUsuarioId(rs.getInt("usuario_id"));
                    t.setUsuario(rs.getString("nombre_completo"));
                    t.setFechaApertura(rs.getTimestamp("fecha_apertura").toLocalDateTime());
                    t.setMontoInicial(rs.getBigDecimal("monto_inicial"));
                    t.setEstado("ABIERTO");
                    return t;
                }
            }
        }
        return null;
    }

    public int abrir(Connection con, TurnoCajaDTO turno) throws SQLException {
        String sql = "INSERT INTO turnos_caja (terminal, usuario_id, monto_inicial, estado) VALUES (?, ?, ?, 'ABIERTO')";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, turno.getTerminal());
            ps.setInt(2, turno.getUsuarioId());
            ps.setBigDecimal(3, turno.getMontoInicial());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public String obtenerEstado(Connection con, int turnoId) throws SQLException {
        String sql = "SELECT estado FROM turnos_caja WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, turnoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("estado");
                }
            }
        }
        return null;
    }

    public void cerrar(Connection con, TurnoCajaDTO turno) throws SQLException {
        String sql = "UPDATE turnos_caja SET fecha_cierre = NOW(), monto_esperado = ?, monto_contado = ?, "
                + "diferencia = ?, observaciones = ?, estado = 'CERRADO' WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBigDecimal(1, turno.getMontoEsperado());
            ps.setBigDecimal(2, turno.getMontoContado());
            ps.setBigDecimal(3, turno.getDiferencia());
            ps.setString(4, turno.getObservaciones());
            ps.setInt(5, turno.getId());
            ps.executeUpdate();
        }
    }

    public Map<String, BigDecimal> totalesPorMetodoPago(Connection con, int turnoId) throws SQLException {
        // LinkedHashMap mantiene el orden en que se insertan los datos
        Map<String, BigDecimal> totales = new LinkedHashMap<>();
        String sql = "SELECT mp.nombre, SUM(p.monto) AS total "
                + "FROM pagos p "
                + "INNER JOIN ventas v ON v.id = p.venta_id "
                + "INNER JOIN metodos_pago mp ON mp.id = p.metodo_pago_id "
                + "WHERE v.turno_id = ? AND v.estado = 'COMPLETADA' "
                + "GROUP BY mp.nombre ORDER BY mp.nombre";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, turnoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    totales.put(rs.getString("nombre"), rs.getBigDecimal("total"));
                }
            }
        }
        return totales;
    }

    public int contarVentas(Connection con, int turnoId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM ventas WHERE turno_id = ? AND estado = 'COMPLETADA'";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, turnoId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }
}
