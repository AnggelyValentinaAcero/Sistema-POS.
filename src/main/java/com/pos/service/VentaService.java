package com.pos.service;

import com.pos.config.Conexion;
import com.pos.dao.AuditoriaDAO;
import com.pos.dao.MovimientoInventarioDAO;
import com.pos.dao.ProductoDAO;
import com.pos.dao.TurnoCajaDAO;
import com.pos.dao.VentaDAO;
import com.pos.dto.DetalleVentaDTO;
import com.pos.dto.MetodoPagoDTO;
import com.pos.dto.PagoDTO;
import com.pos.dto.ProductoDTO;
import com.pos.dto.TurnoCajaDTO;
import com.pos.dto.VentaDTO;
import com.pos.util.Textos;
import com.pos.util.Permiso;
import com.pos.util.Sesion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Lógica de negocio de las ventas. El método más importante es registrarVenta,
 * que guarda toda la venta en una única transacción.
 */
public class VentaService {

    private static final BigDecimal CIEN = new BigDecimal("100");

    private final VentaDAO ventaDAO = new VentaDAO();
    private final ProductoDAO productoDAO = new ProductoDAO();
    private final TurnoCajaDAO turnoDAO = new TurnoCajaDAO();
    private final MovimientoInventarioDAO movimientoDAO = new MovimientoInventarioDAO();
    private final AuditoriaDAO auditoriaDAO = new AuditoriaDAO();

    /** Busca un producto activo por su código de barras (lo que lee el escáner). */
    public ProductoDTO buscarProductoPorCodigo(String codigo) throws NegocioException, SQLException {
        verificarPermiso();
        try (Connection con = Conexion.getConexion()) {
            ProductoDTO producto = productoDAO.buscarPorCodigo(con, codigo.trim());
            if (producto == null || !producto.isActivo()) {
                throw new NegocioException("No existe un producto activo con el código " + codigo + ".");
            }
            return producto;
        }
    }

    public List<MetodoPagoDTO> listarMetodosPago() throws SQLException {
        try (Connection con = Conexion.getConexion()) {
            return ventaDAO.listarMetodosPago(con);
        }
    }

    /**
     * Calcula el subtotal de cada línea y los totales de la venta.
     * Los precios ya incluyen el impuesto, así que se separa la base:
     *   base = total / (1 + porcentaje / 100)      impuesto = total - base
     * Ej: 116.00 con IVA 16% -> base 100.00, impuesto 16.00
     */
    public void calcularTotales(VentaDTO venta) {
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal base = BigDecimal.ZERO;

        for (DetalleVentaDTO d : venta.getDetalles()) {
            BigDecimal totalLinea = d.getCantidad().multiply(d.getPrecioUnitario()).setScale(2, RoundingMode.HALF_UP);
            d.setSubtotal(totalLinea);

            BigDecimal divisor = BigDecimal.ONE.add(d.getPorcentajeImpuesto().divide(CIEN));
            BigDecimal baseLinea = totalLinea.divide(divisor, 2, RoundingMode.HALF_UP);

            total = total.add(totalLinea);
            base = base.add(baseLinea);
        }

        venta.setTotal(total);
        venta.setSubtotal(base);
        venta.setImpuesto(total.subtract(base));
    }

    /**
     * Registra la venta completa como UNA SOLA TRANSACCIÓN:
     *   1. inserta el encabezado de la venta (maestro)
     *   2. por cada producto: descuenta el stock, inserta la línea de detalle y el movimiento de kardex
     *   3. inserta el pago
     * Si cualquier paso falla (por ejemplo, no hay stock del último producto)
     * se hace rollback y no queda guardado nada. Devuelve el número de la venta.
     */
    public long registrarVenta(VentaDTO venta) throws NegocioException, SQLException {
        verificarPermiso();
        if (venta.getDetalles().isEmpty()) {
            throw new NegocioException("La venta no tiene productos.");
        }
        if (venta.getPago() == null) {
            throw new NegocioException("Falta registrar el pago.");
        }

        Connection con = null;
        try {
            con = Conexion.getConexion();
            con.setAutoCommit(false); // inicio de la transacción

            TurnoCajaDTO turno = turnoDAO.buscarAbierto(con, Sesion.getTerminal());
            if (turno == null) {
                throw new NegocioException("La caja de esta terminal está cerrada. Ábrala antes de vender.");
            }

            // Se vuelven a leer precio, costo e impuesto desde la base de datos.
            // No se confía en los valores que vienen de la pantalla.
            for (DetalleVentaDTO d : venta.getDetalles()) {
                if (d.getCantidad().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new NegocioException("La cantidad debe ser mayor a 0.");
                }
                ProductoDTO producto = productoDAO.buscarPorId(con, d.getProductoId());
                if (producto == null || !producto.isActivo()) {
                    throw new NegocioException("El producto " + d.getNombreProducto() + " ya no está disponible.");
                }
                d.setNombreProducto(producto.getNombre());
                d.setPrecioUnitario(producto.getPrecioVenta());
                d.setCostoUnitario(producto.getPrecioCompra());
                d.setPorcentajeImpuesto(producto.getPorcentajeImpuesto());
            }
            calcularTotales(venta);
            validarPago(con, venta);

            // 1. Encabezado (maestro)
            int usuarioId = Sesion.getUsuario().getId();
            venta.setTurnoId(turno.getId());
            venta.setUsuarioId(usuarioId);
            long ventaId = ventaDAO.insertar(con, venta);

            // 2. Detalle + stock + kardex
            for (DetalleVentaDTO d : venta.getDetalles()) {
                boolean descontado = productoDAO.descontarStock(con, d.getProductoId(), d.getCantidad());
                if (!descontado) {
                    BigDecimal disponible = productoDAO.obtenerStock(con, d.getProductoId());
                    throw new NegocioException("Stock insuficiente de " + d.getNombreProducto()
                            + ". Disponible: " + disponible.stripTrailingZeros().toPlainString());
                }
                ventaDAO.insertarDetalle(con, ventaId, d);

                BigDecimal stockResultante = productoDAO.obtenerStock(con, d.getProductoId());
                movimientoDAO.registrar(con, d.getProductoId(), "VENTA", d.getCantidad().negate(), stockResultante,
                        d.getCostoUnitario(), ventaId, usuarioId, null);
            }

            // 3. Pago
            ventaDAO.insertarPago(con, ventaId, venta.getPago());

            // 4. Auditoría
            auditoriaDAO.registrar(con, usuarioId, Sesion.getTerminal(), "VENTA_REGISTRADA", "ventas",
                    String.valueOf(ventaId), "Se registró la venta #" + ventaId + " por un total de "
                            + Textos.moneda(venta.getTotal()) + " (" + venta.getDetalles().size() + " productos)");

            con.commit(); // todo salió bien: se confirman todos los cambios juntos
            venta.setId(ventaId);
            return ventaId;
        } catch (SQLException | NegocioException e) {
            Conexion.rollback(con); // algo falló: se deshace todo
            throw e;
        } finally {
            Conexion.cerrar(con);
        }
    }

    /**
     * Anula una venta en UNA transacción:
     *   1. marca la venta como ANULADA (con quién, cuándo y por qué)
     *   2. devuelve al stock cada producto vendido y lo registra en el kardex (ANULACION)
     *   3. registra la anulación en auditoría
     * La venta no se borra: queda en el historial como ANULADA y deja de contar
     * en la caja y en los reportes.
     * Regla: solo se anulan ventas de un turno de caja que siga ABIERTO, porque el
     * cierre de caja ya cuadró el dinero de los turnos cerrados.
     */
    public void anularVenta(long ventaId, String motivo) throws NegocioException, SQLException {
        if (!Sesion.tienePermiso(Permiso.VENTAS_ANULAR)) {
            throw new NegocioException("No tiene permiso para anular ventas.");
        }
        String motivoLimpio = Textos.limpiar(motivo);
        if (motivoLimpio == null) {
            throw new NegocioException("Indique el motivo de la anulación.");
        }

        Connection con = null;
        try {
            con = Conexion.getConexion();
            con.setAutoCommit(false);

            VentaDTO venta = ventaDAO.buscarPorId(con, ventaId);
            if (venta == null) {
                throw new NegocioException("No existe la venta N° " + ventaId + ".");
            }
            if (venta.isAnulada()) {
                throw new NegocioException("La venta N° " + ventaId + " ya está anulada.");
            }
            if (!"ABIERTO".equals(turnoDAO.obtenerEstado(con, venta.getTurnoId()))) {
                throw new NegocioException("No se puede anular: la venta pertenece a un turno de caja ya cerrado.");
            }

            int usuarioId = Sesion.getUsuario().getId();
            // 1. Encabezado: si otra persona la anuló al mismo tiempo, afecta 0 filas
            if (!ventaDAO.anular(con, ventaId, usuarioId, motivoLimpio)) {
                throw new NegocioException("La venta N° " + ventaId + " ya fue anulada.");
            }

            // 2. Devolver el stock de cada producto
            for (DetalleVentaDTO d : ventaDAO.listarDetalles(con, ventaId)) {
                productoDAO.aumentarStock(con, d.getProductoId(), d.getCantidad());
                BigDecimal stockResultante = productoDAO.obtenerStock(con, d.getProductoId());
                movimientoDAO.registrar(con, d.getProductoId(), "ANULACION", d.getCantidad(), stockResultante,
                        d.getCostoUnitario(), ventaId, usuarioId, motivoLimpio);
            }

            // 3. Auditoría
            auditoriaDAO.registrar(con, usuarioId, Sesion.getTerminal(), "VENTA_ANULADA", "ventas",
                    String.valueOf(ventaId), "Se anuló la venta #" + ventaId + " (Total: "
                            + Textos.moneda(venta.getTotal()) + "). Motivo: " + motivoLimpio);

            con.commit();
        } catch (SQLException | NegocioException e) {
            Conexion.rollback(con);
            throw e;
        } finally {
            Conexion.cerrar(con);
        }
    }

    /**
     * Ventas entre dos fechas. Quien no tiene permiso de reportes (el cajero)
     * solo ve sus propias ventas.
     */
    public List<VentaDTO> listarVentas(LocalDate desde, LocalDate hasta) throws NegocioException, SQLException {
        if (!Sesion.tienePermiso(Permiso.VENTAS_HISTORIAL)) {
            throw new NegocioException("No tiene permiso para ver el historial de ventas.");
        }
        if (desde == null || hasta == null || desde.isAfter(hasta)) {
            throw new NegocioException("Seleccione un rango de fechas válido.");
        }
        Integer soloUsuario = null;
        if (!Sesion.tienePermiso(Permiso.REPORTES_VER)) {
            soloUsuario = Sesion.getUsuario().getId();
        }
        try (Connection con = Conexion.getConexion()) {
            return ventaDAO.listar(con, desde, hasta, soloUsuario);
        }
    }

    /** Venta con su detalle y su pago (para imprimir o reimprimir el recibo). */
    public VentaDTO buscarVentaCompleta(long ventaId) throws NegocioException, SQLException {
        try (Connection con = Conexion.getConexion()) {
            VentaDTO venta = ventaDAO.buscarPorId(con, ventaId);
            if (venta == null) {
                throw new NegocioException("No existe la venta N° " + ventaId + ".");
            }
            // el cajero solo puede ver sus propias ventas
            if (!Sesion.tienePermiso(Permiso.REPORTES_VER) && venta.getUsuarioId() != Sesion.getUsuario().getId()) {
                throw new NegocioException("Solo puede ver sus propias ventas.");
            }
            venta.setDetalles(ventaDAO.listarDetalles(con, ventaId));
            venta.setPago(ventaDAO.buscarPago(con, ventaId));
            return venta;
        }
    }

    public List<DetalleVentaDTO> listarDetalles(long ventaId) throws SQLException {
        try (Connection con = Conexion.getConexion()) {
            return ventaDAO.listarDetalles(con, ventaId);
        }
    }

    /** Completa los datos del pago y verifica que el efectivo recibido alcance. */
    private void validarPago(Connection con, VentaDTO venta) throws NegocioException, SQLException {
        PagoDTO pago = venta.getPago();
        MetodoPagoDTO metodo = ventaDAO.buscarMetodoPago(con, pago.getMetodoPagoId());
        if (metodo == null) {
            throw new NegocioException("Seleccione un método de pago válido.");
        }
        pago.setMetodoPago(metodo.getNombre());
        pago.setMonto(venta.getTotal());
        pago.setReferencia(Textos.limpiar(pago.getReferencia()));

        if (metodo.isEfectivo()) {
            if (pago.getMontoRecibido() == null || pago.getMontoRecibido().compareTo(venta.getTotal()) < 0) {
                throw new NegocioException("El efectivo recibido no alcanza para pagar el total.");
            }
            pago.setCambio(pago.getMontoRecibido().subtract(venta.getTotal()));
        } else {
            // con tarjeta o transferencia se cobra el monto exacto
            pago.setMontoRecibido(venta.getTotal());
            pago.setCambio(BigDecimal.ZERO);
        }
    }

    private void verificarPermiso() throws NegocioException {
        if (!Sesion.tienePermiso(Permiso.VENTAS_REALIZAR)) {
            throw new NegocioException("No tiene permiso para realizar ventas.");
        }
    }
}
