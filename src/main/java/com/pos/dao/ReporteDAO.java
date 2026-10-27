package com.pos.dao;

import com.pos.dto.ItemReporteDTO;
import com.pos.dto.ResumenVentasDTO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Reportes calculados por MySQL con funciones de agregación (SUM, COUNT) y GROUP BY.
 */
public class ReporteDAO {

    // Ganancia de una línea = precio sin impuesto - costo.
    // d.subtotal incluye el impuesto, por eso se divide por (1 + porcentaje / 100).
    // Se usan los valores HISTÓRICOS guardados en detalle_ventas al momento de vender.
    private static final String GANANCIA_LINEA =
            "(d.subtotal / (1 + d.porcentaje_impuesto / 100) - d.costo_unitario * d.cantidad)";

    // Filtro común: ventas completadas dentro del rango de fechas
    private static final String FILTRO_VENTAS = "v.estado = 'COMPLETADA' AND v.fecha >= ? AND v.fecha < ? ";

    public ResumenVentasDTO resumen(Connection con, LocalDate desde, LocalDate hasta) throws SQLException {
        ResumenVentasDTO resumen = new ResumenVentasDTO();

        // 1. Cantidad de ventas, total e impuestos (tabla maestro)
        String sql = "SELECT COUNT(*) AS cantidad, COALESCE(SUM(v.total), 0) AS total, "
                + "COALESCE(SUM(v.impuesto), 0) AS impuestos FROM ventas v WHERE " + FILTRO_VENTAS;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            asignarFechas(ps, desde, hasta);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                resumen.setCantidadVentas(rs.getInt("cantidad"));
                resumen.setTotalVendido(rs.getBigDecimal("total"));
                resumen.setTotalImpuestos(rs.getBigDecimal("impuestos"));
            }
        }

        // 2. Ganancia (se calcula línea por línea en la tabla detalle)
        sql = "SELECT COALESCE(SUM(" + GANANCIA_LINEA + "), 0) AS ganancia "
                + "FROM detalle_ventas d INNER JOIN ventas v ON v.id = d.venta_id WHERE " + FILTRO_VENTAS;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            asignarFechas(ps, desde, hasta);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                resumen.setGanancia(redondear(rs.getBigDecimal("ganancia")));
            }
        }
        return resumen;
    }

    public List<ItemReporteDTO> ventasPorDia(Connection con, LocalDate desde, LocalDate hasta) throws SQLException {
        String sql = "SELECT DATE(v.fecha) AS dia, COUNT(*) AS cantidad, SUM(v.total) AS total "
                + "FROM ventas v WHERE " + FILTRO_VENTAS
                + "GROUP BY DATE(v.fecha) ORDER BY dia";
        List<ItemReporteDTO> lista = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            asignarFechas(ps, desde, hasta);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new ItemReporteDTO(rs.getDate("dia").toLocalDate().toString(),
                            rs.getBigDecimal("cantidad"), rs.getBigDecimal("total"), BigDecimal.ZERO));
                }
            }
        }
        return lista;
    }

    public List<ItemReporteDTO> productosMasVendidos(Connection con, LocalDate desde, LocalDate hasta, int limite)
            throws SQLException {
        String sql = "SELECT p.nombre, SUM(d.cantidad) AS cantidad, SUM(d.subtotal) AS total, "
                + "SUM(" + GANANCIA_LINEA + ") AS ganancia "
                + "FROM detalle_ventas d "
                + "INNER JOIN ventas v ON v.id = d.venta_id "
                + "INNER JOIN productos p ON p.id = d.producto_id "
                + "WHERE " + FILTRO_VENTAS
                + "GROUP BY p.id, p.nombre "
                + "ORDER BY cantidad DESC "
                + "LIMIT ?";
        List<ItemReporteDTO> lista = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            asignarFechas(ps, desde, hasta);
            ps.setInt(3, limite);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new ItemReporteDTO(rs.getString("nombre"), rs.getBigDecimal("cantidad"),
                            rs.getBigDecimal("total"), redondear(rs.getBigDecimal("ganancia"))));
                }
            }
        }
        return lista;
    }

    public List<ItemReporteDTO> ventasPorCajero(Connection con, LocalDate desde, LocalDate hasta) throws SQLException {
        String sql = "SELECT u.nombre_completo AS nombre, COUNT(*) AS cantidad, SUM(v.total) AS total "
                + "FROM ventas v INNER JOIN usuarios u ON u.id = v.usuario_id "
                + "WHERE " + FILTRO_VENTAS
                + "GROUP BY u.id, u.nombre_completo ORDER BY total DESC";
        return listarAgrupado(con, sql, desde, hasta);
    }

    public List<ItemReporteDTO> ventasPorMetodoPago(Connection con, LocalDate desde, LocalDate hasta) throws SQLException {
        String sql = "SELECT mp.nombre, COUNT(*) AS cantidad, SUM(p.monto) AS total "
                + "FROM pagos p "
                + "INNER JOIN ventas v ON v.id = p.venta_id "
                + "INNER JOIN metodos_pago mp ON mp.id = p.metodo_pago_id "
                + "WHERE " + FILTRO_VENTAS
                + "GROUP BY mp.id, mp.nombre ORDER BY total DESC";
        return listarAgrupado(con, sql, desde, hasta);
    }

    /** Ejecuta una consulta que devuelve las columnas nombre, cantidad y total. */
    private List<ItemReporteDTO> listarAgrupado(Connection con, String sql, LocalDate desde, LocalDate hasta)
            throws SQLException {
        List<ItemReporteDTO> lista = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            asignarFechas(ps, desde, hasta);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new ItemReporteDTO(rs.getString("nombre"), rs.getBigDecimal("cantidad"),
                            rs.getBigDecimal("total"), BigDecimal.ZERO));
                }
            }
        }
        return lista;
    }

    /** Los dos primeros parámetros de todas las consultas son el rango de fechas. */
    private void asignarFechas(PreparedStatement ps, LocalDate desde, LocalDate hasta) throws SQLException {
        ps.setTimestamp(1, Timestamp.valueOf(desde.atStartOfDay()));
        ps.setTimestamp(2, Timestamp.valueOf(hasta.plusDays(1).atStartOfDay()));
    }

    private BigDecimal redondear(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }
}
