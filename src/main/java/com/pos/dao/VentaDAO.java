package com.pos.dao;

import com.pos.dto.DetalleVentaDTO;
import com.pos.dto.MetodoPagoDTO;
import com.pos.dto.PagoDTO;
import com.pos.dto.VentaDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a datos de ventas: maestro (ventas), detalle (detalle_ventas), pagos y métodos de pago.
 */
public class VentaDAO {

    /** Inserta el encabezado (maestro) y devuelve el id generado. */
    public long insertar(Connection con, VentaDTO v) throws SQLException {
        String sql = "INSERT INTO ventas (turno_id, usuario_id, cliente_id, subtotal, impuesto, total, estado) "
                + "VALUES (?, ?, ?, ?, ?, ?, 'COMPLETADA')";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, v.getTurnoId());
            ps.setInt(2, v.getUsuarioId());
            if (v.getClienteId() == null) {
                ps.setNull(3, Types.INTEGER);
            } else {
                ps.setInt(3, v.getClienteId());
            }
            ps.setBigDecimal(4, v.getSubtotal());
            ps.setBigDecimal(5, v.getImpuesto());
            ps.setBigDecimal(6, v.getTotal());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    public void insertarDetalle(Connection con, long ventaId, DetalleVentaDTO d) throws SQLException {
        String sql = "INSERT INTO detalle_ventas (venta_id, producto_id, cantidad, precio_unitario, costo_unitario, "
                + "porcentaje_impuesto, subtotal) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, ventaId);
            ps.setInt(2, d.getProductoId());
            ps.setBigDecimal(3, d.getCantidad());
            ps.setBigDecimal(4, d.getPrecioUnitario());
            ps.setBigDecimal(5, d.getCostoUnitario());
            ps.setBigDecimal(6, d.getPorcentajeImpuesto());
            ps.setBigDecimal(7, d.getSubtotal());
            ps.executeUpdate();
        }
    }

    public void insertarPago(Connection con, long ventaId, PagoDTO p) throws SQLException {
        String sql = "INSERT INTO pagos (venta_id, metodo_pago_id, monto, monto_recibido, cambio, referencia) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, ventaId);
            ps.setInt(2, p.getMetodoPagoId());
            ps.setBigDecimal(3, p.getMonto());
            ps.setBigDecimal(4, p.getMontoRecibido());
            ps.setBigDecimal(5, p.getCambio());
            ps.setString(6, p.getReferencia());
            ps.executeUpdate();
        }
    }

    // La subconsulta trae el nombre del método de pago de cada venta
    private static final String SELECT_VENTA =
            "SELECT v.id, v.turno_id, v.usuario_id, u.nombre_completo AS usuario, v.cliente_id, "
            + "c.nombre AS cliente, v.fecha, v.subtotal, v.impuesto, v.total, v.estado, v.motivo_anulacion, "
            + "(SELECT mp.nombre FROM pagos p INNER JOIN metodos_pago mp ON mp.id = p.metodo_pago_id "
            + " WHERE p.venta_id = v.id LIMIT 1) AS metodo_pago "
            + "FROM ventas v "
            + "INNER JOIN usuarios u ON u.id = v.usuario_id "
            + "LEFT JOIN clientes c ON c.id = v.cliente_id ";

    public VentaDTO buscarPorId(Connection con, long id) throws SQLException {
        String sql = SELECT_VENTA + "WHERE v.id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return leerVenta(rs);
                }
            }
        }
        return null;
    }

    /**
     * Marca la venta como ANULADA, solo si todavía está COMPLETADA.
     * Devuelve false si otra persona ya la había anulado.
     */
    public boolean anular(Connection con, long id, int usuarioId, String motivo) throws SQLException {
        // "AND estado = 'COMPLETADA'" evita anular dos veces la misma venta
        String sql = "UPDATE ventas SET estado = 'ANULADA', anulada_por = ?, fecha_anulacion = NOW(), "
                + "motivo_anulacion = ? WHERE id = ? AND estado = 'COMPLETADA'";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, usuarioId);
            ps.setString(2, motivo);
            ps.setLong(3, id);
            return ps.executeUpdate() == 1;
        }
    }

    /**
     * Ventas entre dos fechas (inclusive).
     * @param usuarioId si no es null, solo las ventas de ese usuario
     */
    public List<VentaDTO> listar(Connection con, LocalDate desde, LocalDate hasta, Integer usuarioId) throws SQLException {
        List<VentaDTO> lista = new ArrayList<>();
        String sql = SELECT_VENTA + "WHERE v.fecha >= ? AND v.fecha < ? ";
        if (usuarioId != null) {
            sql += "AND v.usuario_id = ? ";
        }
        sql += "ORDER BY v.fecha DESC";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            // desde las 00:00 del primer día hasta antes de las 00:00 del día siguiente al último
            ps.setTimestamp(1, Timestamp.valueOf(desde.atStartOfDay()));
            ps.setTimestamp(2, Timestamp.valueOf(hasta.plusDays(1).atStartOfDay()));
            if (usuarioId != null) {
                ps.setInt(3, usuarioId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(leerVenta(rs));
                }
            }
        }
        return lista;
    }

    private VentaDTO leerVenta(ResultSet rs) throws SQLException {
        VentaDTO v = new VentaDTO();
        v.setId(rs.getLong("id"));
        v.setTurnoId(rs.getInt("turno_id"));
        v.setUsuarioId(rs.getInt("usuario_id"));
        v.setUsuario(rs.getString("usuario"));
        v.setClienteId(rs.getObject("cliente_id", Integer.class));
        v.setCliente(rs.getString("cliente"));
        v.setFecha(rs.getTimestamp("fecha").toLocalDateTime());
        v.setSubtotal(rs.getBigDecimal("subtotal"));
        v.setImpuesto(rs.getBigDecimal("impuesto"));
        v.setTotal(rs.getBigDecimal("total"));
        v.setEstado(rs.getString("estado"));
        v.setMotivoAnulacion(rs.getString("motivo_anulacion"));
        v.setMetodoPago(rs.getString("metodo_pago"));
        return v;
    }

    public List<DetalleVentaDTO> listarDetalles(Connection con, long ventaId) throws SQLException {
        List<DetalleVentaDTO> lista = new ArrayList<>();
        String sql = "SELECT d.id, d.producto_id, p.codigo_barras, p.nombre, p.unidad_medida, d.cantidad, "
                + "d.precio_unitario, d.costo_unitario, d.porcentaje_impuesto, d.subtotal "
                + "FROM detalle_ventas d INNER JOIN productos p ON p.id = d.producto_id "
                + "WHERE d.venta_id = ? ORDER BY d.id";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, ventaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DetalleVentaDTO d = new DetalleVentaDTO();
                    d.setId(rs.getLong("id"));
                    d.setProductoId(rs.getInt("producto_id"));
                    d.setCodigoBarras(rs.getString("codigo_barras"));
                    d.setNombreProducto(rs.getString("nombre"));
                    d.setUnidadMedida(rs.getString("unidad_medida"));
                    d.setCantidad(rs.getBigDecimal("cantidad"));
                    d.setPrecioUnitario(rs.getBigDecimal("precio_unitario"));
                    d.setCostoUnitario(rs.getBigDecimal("costo_unitario"));
                    d.setPorcentajeImpuesto(rs.getBigDecimal("porcentaje_impuesto"));
                    d.setSubtotal(rs.getBigDecimal("subtotal"));
                    lista.add(d);
                }
            }
        }
        return lista;
    }

    public PagoDTO buscarPago(Connection con, long ventaId) throws SQLException {
        String sql = "SELECT p.metodo_pago_id, mp.nombre, p.monto, p.monto_recibido, p.cambio, p.referencia "
                + "FROM pagos p INNER JOIN metodos_pago mp ON mp.id = p.metodo_pago_id WHERE p.venta_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, ventaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    PagoDTO p = new PagoDTO();
                    p.setMetodoPagoId(rs.getInt("metodo_pago_id"));
                    p.setMetodoPago(rs.getString("nombre"));
                    p.setMonto(rs.getBigDecimal("monto"));
                    p.setMontoRecibido(rs.getBigDecimal("monto_recibido"));
                    p.setCambio(rs.getBigDecimal("cambio"));
                    p.setReferencia(rs.getString("referencia"));
                    return p;
                }
            }
        }
        return null;
    }

    // ---- Métodos de pago (tabla metodos_pago) ----

    public List<MetodoPagoDTO> listarMetodosPago(Connection con) throws SQLException {
        List<MetodoPagoDTO> lista = new ArrayList<>();
        String sql = "SELECT id, nombre FROM metodos_pago WHERE activo = TRUE ORDER BY id";
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(new MetodoPagoDTO(rs.getInt("id"), rs.getString("nombre")));
            }
        }
        return lista;
    }

    public MetodoPagoDTO buscarMetodoPago(Connection con, int id) throws SQLException {
        String sql = "SELECT id, nombre FROM metodos_pago WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new MetodoPagoDTO(rs.getInt("id"), rs.getString("nombre"));
                }
            }
        }
        return null;
    }
}
