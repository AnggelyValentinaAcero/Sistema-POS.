package com.pos.service;

import com.pos.config.Conexion;
import com.pos.dao.AuditoriaDAO;
import com.pos.dao.MovimientoInventarioDAO;
import com.pos.dao.ProductoDAO;
import com.pos.dao.ProveedorDAO;
import com.pos.dto.MovimientoDTO;
import com.pos.dto.ProductoDTO;
import com.pos.dto.ProveedorDTO;
import com.pos.util.Permiso;
import com.pos.util.Sesion;
import com.pos.util.Textos;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Entradas de mercadería, ajustes de inventario y consulta del kardex.
 * Regla principal: cualquier cambio de stock queda registrado en movimientos_inventario,
 * en la misma transacción en la que se modifica el producto.
 */
public class InventarioService {

    public static final String[] MOTIVOS_AJUSTE = {
        "Conteo físico", "Merma", "Producto dañado", "Producto vencido", "Pérdida / robo", "Otro"
    };

    private final ProductoDAO productoDAO = new ProductoDAO();
    private final ProveedorDAO proveedorDAO = new ProveedorDAO();
    private final MovimientoInventarioDAO movimientoDAO = new MovimientoInventarioDAO();
    private final AuditoriaDAO auditoriaDAO = new AuditoriaDAO();

    public List<ProductoDTO> listarStockBajo() throws NegocioException, SQLException {
        verificarPermiso();
        try (Connection con = Conexion.getConexion()) {
            return productoDAO.listarStockBajo(con);
        }
    }

    public List<ProveedorDTO> listarProveedores() throws SQLException {
        try (Connection con = Conexion.getConexion()) {
            return proveedorDAO.buscar(con, "", false);
        }
    }

    public ProductoDTO buscarProducto(int productoId) throws SQLException {
        try (Connection con = Conexion.getConexion()) {
            return productoDAO.buscarPorId(con, productoId);
        }
    }

    public List<MovimientoDTO> kardex(Integer productoId, LocalDate desde, LocalDate hasta)
            throws NegocioException, SQLException {
        verificarPermiso();
        if (desde == null || hasta == null || desde.isAfter(hasta)) {
            throw new NegocioException("Seleccione un rango de fechas válido.");
        }
        try (Connection con = Conexion.getConexion()) {
            return movimientoDAO.listar(con, productoId, desde, hasta);
        }
    }

    /**
     * Registra la llegada de mercadería (compra a un proveedor).
     * @param actualizarCosto si es true, el costo unitario pasa a ser el nuevo precio de compra del producto
     */
    public void registrarEntrada(int productoId, BigDecimal cantidad, BigDecimal costoUnitario, ProveedorDTO proveedor,
                                 boolean actualizarCosto, String observacion) throws NegocioException, SQLException {
        verificarPermiso();
        if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) <= 0) {
            throw new NegocioException("La cantidad debe ser mayor a 0.");
        }
        if (costoUnitario == null || costoUnitario.compareTo(BigDecimal.ZERO) < 0) {
            throw new NegocioException("El costo unitario no puede ser negativo.");
        }

        // el motivo del kardex indica de quién se compró
        String motivo = "Compra";
        if (proveedor != null && proveedor.getId() > 0) {
            motivo += " a " + proveedor.getNombre();
        }
        String detalle = Textos.limpiar(observacion);
        if (detalle != null) {
            motivo += " - " + detalle;
        }

        Connection con = null;
        try {
            con = Conexion.getConexion();
            con.setAutoCommit(false);

            ProductoDTO producto = productoDAO.buscarPorId(con, productoId);
            if (producto == null || !producto.isActivo()) {
                throw new NegocioException("El producto no existe o está desactivado.");
            }
            int usuarioId = Sesion.getUsuario().getId();

            productoDAO.aumentarStock(con, productoId, cantidad);
            BigDecimal stockResultante = productoDAO.obtenerStock(con, productoId);
            movimientoDAO.registrar(con, productoId, "ENTRADA", cantidad, stockResultante, costoUnitario,
                    null, usuarioId, motivo);

            boolean cambiaCosto = producto.getPrecioCompra().compareTo(costoUnitario) != 0;
            if (actualizarCosto && cambiaCosto) {
                // misma regla que en el catálogo: no vender por debajo del costo
                if (costoUnitario.compareTo(producto.getPrecioVenta()) > 0) {
                    throw new NegocioException("El nuevo costo (" + costoUnitario + ") supera el precio de venta ("
                            + producto.getPrecioVenta() + "). Actualice primero el precio de venta del producto.");
                }
                productoDAO.actualizarPrecioCompra(con, productoId, costoUnitario);
                auditoriaDAO.registrar(con, usuarioId, Sesion.getTerminal(), "PRECIO_MODIFICADO", "productos",
                        String.valueOf(productoId), "compra: " + producto.getPrecioCompra() + " -> " + costoUnitario
                                + " (entrada de mercadería)");
            }

            auditoriaDAO.registrar(con, usuarioId, Sesion.getTerminal(), "ENTRADA_INVENTARIO", "productos",
                    String.valueOf(productoId), producto.getNombre() + ": +" + cantidad.stripTrailingZeros().toPlainString());
            con.commit();
        } catch (SQLException | NegocioException e) {
            Conexion.rollback(con);
            throw e;
        } finally {
            Conexion.cerrar(con);
        }
    }

    /**
     * Ajuste manual: suma (cantidad positiva) o resta (cantidad negativa) stock.
     * Se usa para mermas, productos dañados, vencidos, etc.
     */
    public void registrarAjuste(int productoId, BigDecimal cantidad, String motivo, String detalle)
            throws NegocioException, SQLException {
        verificarPermiso();
        if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) == 0) {
            throw new NegocioException("La cantidad del ajuste no puede ser 0.");
        }
        ajustar(productoId, cantidad, null, armarMotivo(motivo, detalle));
    }

    /**
     * Conteo físico: se cuenta lo que realmente hay en la bodega y el sistema
     * calcula la diferencia contra el stock registrado. La diferencia se calcula
     * DENTRO de la transacción, para que una venta hecha en ese momento no la altere.
     */
    public BigDecimal registrarConteo(int productoId, BigDecimal stockContado, String motivo, String detalle)
            throws NegocioException, SQLException {
        verificarPermiso();
        if (stockContado == null || stockContado.compareTo(BigDecimal.ZERO) < 0) {
            throw new NegocioException("El stock contado no puede ser negativo.");
        }
        return ajustar(productoId, null, stockContado, armarMotivo(motivo, detalle));
    }

    /**
     * Ajuste común a los dos casos anteriores. Recibe la cantidad a sumar/restar,
     * o bien el stock contado (y entonces calcula la cantidad). Devuelve la cantidad ajustada.
     */
    private BigDecimal ajustar(int productoId, BigDecimal cantidad, BigDecimal stockContado, String motivo)
            throws NegocioException, SQLException {
        Connection con = null;
        try {
            con = Conexion.getConexion();
            con.setAutoCommit(false);

            ProductoDTO producto = productoDAO.buscarPorId(con, productoId);
            if (producto == null) {
                throw new NegocioException("El producto no existe.");
            }
            if (stockContado != null) {
                // FOR UPDATE bloquea la fila: nadie puede vender este producto hasta el commit
                BigDecimal stockActual = productoDAO.obtenerStock(con, productoId);
                cantidad = stockContado.subtract(stockActual);
                if (cantidad.compareTo(BigDecimal.ZERO) == 0) {
                    throw new NegocioException("El stock contado coincide con el del sistema. No hay nada que ajustar.");
                }
            }

            if (cantidad.compareTo(BigDecimal.ZERO) > 0) {
                productoDAO.aumentarStock(con, productoId, cantidad);
            } else {
                // se reutiliza la misma operación segura de las ventas: nunca deja stock negativo
                boolean descontado = productoDAO.descontarStock(con, productoId, cantidad.negate());
                if (!descontado) {
                    BigDecimal disponible = productoDAO.obtenerStock(con, productoId);
                    throw new NegocioException("No se pueden restar " + cantidad.negate().stripTrailingZeros().toPlainString()
                            + ": el stock actual es " + disponible.stripTrailingZeros().toPlainString() + ".");
                }
            }

            int usuarioId = Sesion.getUsuario().getId();
            BigDecimal stockResultante = productoDAO.obtenerStock(con, productoId);
            movimientoDAO.registrar(con, productoId, "AJUSTE", cantidad, stockResultante, producto.getPrecioCompra(),
                    null, usuarioId, motivo);
            auditoriaDAO.registrar(con, usuarioId, Sesion.getTerminal(), "AJUSTE_INVENTARIO", "productos",
                    String.valueOf(productoId), producto.getNombre() + ": " + cantidad.stripTrailingZeros().toPlainString()
                            + " (" + motivo + ")");

            con.commit();
            return cantidad;
        } catch (SQLException | NegocioException e) {
            Conexion.rollback(con);
            throw e;
        } finally {
            Conexion.cerrar(con);
        }
    }

    private String armarMotivo(String motivo, String detalle) throws NegocioException {
        if (Textos.estaVacio(motivo)) {
            throw new NegocioException("Seleccione el motivo del ajuste.");
        }
        String texto = motivo.trim();
        String extra = Textos.limpiar(detalle);
        if (extra != null) {
            texto += " - " + extra;
        }
        if ("Otro".equals(motivo) && extra == null) {
            throw new NegocioException("Si el motivo es \"Otro\", describa el detalle.");
        }
        return texto;
    }

    private void verificarPermiso() throws NegocioException {
        if (!Sesion.tienePermiso(Permiso.INVENTARIO_GESTIONAR)) {
            throw new NegocioException("No tiene permiso para gestionar el inventario.");
        }
    }
}
