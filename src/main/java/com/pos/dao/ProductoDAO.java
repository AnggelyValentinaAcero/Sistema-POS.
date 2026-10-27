package com.pos.dao;

import com.pos.dto.ImpuestoDTO;
import com.pos.dto.ProductoDTO;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class ProductoDAO {

    // Une productos con categorías, impuestos y proveedores para traer también sus nombres.
    // Se usa LEFT JOIN con proveedores porque un producto puede no tener proveedor.
    private static final String SELECT_PRODUCTO =
            "SELECT p.id, p.codigo_barras, p.nombre, p.descripcion, p.categoria_id, c.nombre AS categoria, "
            + "p.proveedor_id, pr.nombre AS proveedor, p.impuesto_id, i.nombre AS impuesto, i.porcentaje, "
            + "p.unidad_medida, p.precio_compra, p.precio_venta, p.stock, p.stock_minimo, p.activo "
            + "FROM productos p "
            + "INNER JOIN categorias c ON c.id = p.categoria_id "
            + "INNER JOIN impuestos i ON i.id = p.impuesto_id "
            + "LEFT JOIN proveedores pr ON pr.id = p.proveedor_id ";

    public List<ProductoDTO> buscar(Connection con, String texto, boolean incluirInactivos) throws SQLException {
        List<ProductoDTO> lista = new ArrayList<>();
        String sql = SELECT_PRODUCTO + "WHERE (p.nombre LIKE ? OR p.codigo_barras LIKE ?) ";
        if (!incluirInactivos) {
            sql += "AND p.activo = TRUE ";
        }
        sql += "ORDER BY p.nombre ASC, p.codigo_barras ASC";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            // los % hacen que LIKE busque el texto en cualquier parte
            ps.setString(1, "%" + texto + "%");
            ps.setString(2, "%" + texto + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(leerProducto(rs));
                }
            }
        }
        return lista;
    }

    public ProductoDTO buscarPorId(Connection con, int id) throws SQLException {
        String sql = SELECT_PRODUCTO + "WHERE p.id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return leerProducto(rs);
                }
            }
        }
        return null;
    }

    public ProductoDTO buscarPorCodigo(Connection con, String codigoBarras) throws SQLException {
        String sql = SELECT_PRODUCTO + "WHERE p.codigo_barras = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, codigoBarras);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return leerProducto(rs);
                }
            }
        }
        return null;
    }

    public int insertar(Connection con, ProductoDTO p) throws SQLException {
        String sql = "INSERT INTO productos (codigo_barras, nombre, descripcion, categoria_id, proveedor_id, impuesto_id, "
                + "unidad_medida, precio_compra, precio_venta, stock, stock_minimo, activo) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getCodigoBarras());
            ps.setString(2, p.getNombre());
            ps.setString(3, p.getDescripcion());
            ps.setInt(4, p.getCategoriaId());
            if (p.getProveedorId() == null) {
                ps.setNull(5, Types.INTEGER);
            } else {
                ps.setInt(5, p.getProveedorId());
            }
            ps.setInt(6, p.getImpuestoId());
            ps.setString(7, p.getUnidadMedida());
            ps.setBigDecimal(8, p.getPrecioCompra());
            ps.setBigDecimal(9, p.getPrecioVenta());
            ps.setBigDecimal(10, p.getStock());
            ps.setBigDecimal(11, p.getStockMinimo());
            ps.setBoolean(12, p.isActivo());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public void actualizar(Connection con, ProductoDTO p) throws SQLException {
        String sql = "UPDATE productos SET codigo_barras = ?, nombre = ?, descripcion = ?, categoria_id = ?, "
                + "proveedor_id = ?, impuesto_id = ?, unidad_medida = ?, precio_compra = ?, precio_venta = ?, "
                + "stock_minimo = ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, p.getCodigoBarras());
            ps.setString(2, p.getNombre());
            ps.setString(3, p.getDescripcion());
            ps.setInt(4, p.getCategoriaId());
            if (p.getProveedorId() == null) {
                ps.setNull(5, Types.INTEGER);
            } else {
                ps.setInt(5, p.getProveedorId());
            }
            ps.setInt(6, p.getImpuestoId());
            ps.setString(7, p.getUnidadMedida());
            ps.setBigDecimal(8, p.getPrecioCompra());
            ps.setBigDecimal(9, p.getPrecioVenta());
            ps.setBigDecimal(10, p.getStockMinimo());
            ps.setInt(11, p.getId());
            ps.executeUpdate();
        }
    }

    public void cambiarEstado(Connection con, int id, boolean activo) throws SQLException {
        String sql = "UPDATE productos SET activo = ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBoolean(1, activo);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public boolean descontarStock(Connection con, int id, BigDecimal cantidad) throws SQLException {
        // La condición "stock >= ?" hace que MySQL solo descuente si alcanza.
        // Como el UPDATE es atómico, si dos cajas venden el último producto al mismo
        // tiempo, solo una lo logra: a la otra le afecta 0 filas.
        String sql = "UPDATE productos SET stock = stock - ? WHERE id = ? AND stock >= ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBigDecimal(1, cantidad);
            ps.setInt(2, id);
            ps.setBigDecimal(3, cantidad);
            int filasAfectadas = ps.executeUpdate();
            return filasAfectadas == 1;
        }
    }

    public BigDecimal obtenerStock(Connection con, int id) throws SQLException {
        // FOR UPDATE lee el valor actual (ya confirmado por otras cajas) y no la
        // "foto" que InnoDB toma al comenzar la transacción
        String sql = "SELECT stock FROM productos WHERE id = ? FOR UPDATE";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal("stock");
                }
            }
        }
        return BigDecimal.ZERO;
    }

    public void aumentarStock(Connection con, int id, BigDecimal cantidad) throws SQLException {
        String sql = "UPDATE productos SET stock = stock + ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBigDecimal(1, cantidad);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public void actualizarPrecioCompra(Connection con, int id, BigDecimal precioCompra) throws SQLException {
        String sql = "UPDATE productos SET precio_compra = ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBigDecimal(1, precioCompra);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public List<ProductoDTO> listarStockBajo(Connection con) throws SQLException {
        List<ProductoDTO> lista = new ArrayList<>();
        String sql = SELECT_PRODUCTO + "WHERE p.activo = TRUE AND p.stock <= p.stock_minimo ORDER BY p.nombre ASC, p.codigo_barras ASC";
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(leerProducto(rs));
            }
        }
        return lista;
    }

    /** Impuestos activos (tabla impuestos), para el ComboBox del formulario de producto. */
    public List<ImpuestoDTO> listarImpuestos(Connection con) throws SQLException {
        List<ImpuestoDTO> lista = new ArrayList<>();
        String sql = "SELECT id, nombre, porcentaje FROM impuestos WHERE activo = TRUE ORDER BY porcentaje";
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(new ImpuestoDTO(rs.getInt("id"), rs.getString("nombre"), rs.getBigDecimal("porcentaje")));
            }
        }
        return lista;
    }

    private ProductoDTO leerProducto(ResultSet rs) throws SQLException {
        ProductoDTO p = new ProductoDTO();
        p.setId(rs.getInt("id"));
        p.setCodigoBarras(rs.getString("codigo_barras"));
        p.setNombre(rs.getString("nombre"));
        p.setDescripcion(rs.getString("descripcion"));
        p.setCategoriaId(rs.getInt("categoria_id"));
        p.setCategoria(rs.getString("categoria"));
        // getObject devuelve null si la columna es NULL (getInt devolvería 0)
        p.setProveedorId(rs.getObject("proveedor_id", Integer.class));
        p.setProveedor(rs.getString("proveedor"));
        p.setImpuestoId(rs.getInt("impuesto_id"));
        p.setImpuesto(rs.getString("impuesto"));
        p.setPorcentajeImpuesto(rs.getBigDecimal("porcentaje"));
        p.setUnidadMedida(rs.getString("unidad_medida"));
        p.setPrecioCompra(rs.getBigDecimal("precio_compra"));
        p.setPrecioVenta(rs.getBigDecimal("precio_venta"));
        p.setStock(rs.getBigDecimal("stock"));
        p.setStockMinimo(rs.getBigDecimal("stock_minimo"));
        p.setActivo(rs.getBoolean("activo"));
        return p;
    }
}
