package com.pos.dao;

import com.pos.dto.MovimientoDTO;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MovimientoInventarioDAO {

    public void registrar(Connection con, int productoId, String tipo, BigDecimal cantidad, BigDecimal stockResultante,
                          BigDecimal costoUnitario, Long ventaId, int usuarioId, String motivo) throws SQLException {
        String sql = "INSERT INTO movimientos_inventario (producto_id, tipo, cantidad, stock_resultante, costo_unitario, "
                + "venta_id, usuario_id, motivo) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, productoId);
            ps.setString(2, tipo);
            ps.setBigDecimal(3, cantidad);
            ps.setBigDecimal(4, stockResultante);
            ps.setBigDecimal(5, costoUnitario);
            if (ventaId == null) {
                ps.setNull(6, Types.BIGINT);
            } else {
                ps.setLong(6, ventaId);
            }
            ps.setInt(7, usuarioId);
            ps.setString(8, motivo);
            ps.executeUpdate();
        }
    }

    public List<MovimientoDTO> listarPorProducto(Connection con, int productoId, LocalDate desde, LocalDate hasta)
            throws SQLException {
        return listar(con, productoId, desde, hasta);
    }

    public List<MovimientoDTO> listar(Connection con, Integer productoId, LocalDate desde, LocalDate hasta)
            throws SQLException {
        List<MovimientoDTO> lista = new ArrayList<>();
        String sql = "SELECT m.id, m.producto_id, p.codigo_barras, p.nombre AS producto, m.tipo, m.cantidad, "
                + "m.stock_resultante, m.costo_unitario, m.venta_id, u.nombre_completo AS usuario, m.motivo, m.fecha "
                + "FROM movimientos_inventario m "
                + "INNER JOIN productos p ON p.id = m.producto_id "
                + "INNER JOIN usuarios u ON u.id = m.usuario_id "
                + "WHERE m.fecha >= ? AND m.fecha < ? ";
        if (productoId != null) {
            sql += "AND m.producto_id = ? ";
        }
        sql += "ORDER BY m.fecha DESC, m.id DESC";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(desde.atStartOfDay()));
            ps.setTimestamp(2, Timestamp.valueOf(hasta.plusDays(1).atStartOfDay()));
            if (productoId != null) {
                ps.setInt(3, productoId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    MovimientoDTO m = new MovimientoDTO();
                    m.setId(rs.getLong("id"));
                    m.setProductoId(rs.getInt("producto_id"));
                    m.setProducto(rs.getString("codigo_barras") + " - " + rs.getString("producto"));
                    m.setTipo(rs.getString("tipo"));
                    m.setCantidad(rs.getBigDecimal("cantidad"));
                    m.setStockResultante(rs.getBigDecimal("stock_resultante"));
                    m.setCostoUnitario(rs.getBigDecimal("costo_unitario"));
                    m.setVentaId(rs.getObject("venta_id", Long.class));
                    m.setUsuario(rs.getString("usuario"));
                    m.setMotivo(rs.getString("motivo"));
                    m.setFecha(rs.getTimestamp("fecha").toLocalDateTime());
                    lista.add(m);
                }
            }
        }
        return lista;
    }
}
