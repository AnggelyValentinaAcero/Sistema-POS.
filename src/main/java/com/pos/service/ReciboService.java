package com.pos.service;

import com.pos.config.Conexion;
import com.pos.config.ConfiguracionLocal;
import com.pos.dao.AuditoriaDAO;
import com.pos.dto.NegocioDTO;
import com.pos.dto.TurnoCajaDTO;
import com.pos.dto.VentaDTO;
import com.pos.print.GeneradorRecibo;
import com.pos.print.ImpresoraEscPos;
import com.pos.print.ImpresoraSistema;
import com.pos.util.Sesion;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Genera los recibos (venta y comprobante de cierre de caja) y los envía a la impresora
 * configurada en esta terminal.
 */
public class ReciboService {

    private final VentaService ventaService = new VentaService();
    private final CajaService cajaService = new CajaService();
    private final ConfiguracionService configuracionService = new ConfiguracionService();
    private final AuditoriaDAO auditoriaDAO = new AuditoriaDAO();

    /**
     * Arma el recibo de una venta. Si es una reimpresión, el recibo lo indica
     * y queda registrado en auditoría (un recibo reimpreso podría usarse para
     * intentar un reclamo falso).
     */
    public List<String> reciboVenta(long ventaId, boolean reimpresion) throws NegocioException, SQLException {
        VentaDTO venta = ventaService.buscarVentaCompleta(ventaId);
        if (reimpresion) {
            try (Connection con = Conexion.getConexion()) {
                auditoriaDAO.registrar(con, Sesion.getUsuario().getId(), Sesion.getTerminal(), "RECIBO_REIMPRESO",
                        "ventas", String.valueOf(ventaId), null);
            }
        }
        return crearGenerador().reciboVenta(venta, reimpresion);
    }

    public List<String> reciboCierreCaja(TurnoCajaDTO turno) throws SQLException {
        Map<String, BigDecimal> totales = cajaService.totalesPorMetodoPago(turno.getId());
        int cantidadVentas = cajaService.contarVentas(turno.getId());
        return crearGenerador().reciboCierreCaja(turno, totales, cantidadVentas);
    }

    /** Indica si después de cada venta se debe imprimir el recibo sin preguntar. */
    public boolean imprimirAutomaticamente() {
        ConfiguracionLocal config = ConfiguracionLocal.cargar();
        return config.isImprimirAutomatico() && !ConfiguracionLocal.NINGUNA.equals(config.getTipoImpresora());
    }

    /**
     * Envía las líneas a la impresora configurada.
     * @param abrirCajon true después de una venta (si la impresora lo permite)
     */
    public void imprimir(List<String> lineas, boolean abrirCajon) throws NegocioException {
        ConfiguracionLocal config = ConfiguracionLocal.cargar();
        try {
            switch (config.getTipoImpresora()) {
                case ConfiguracionLocal.ESCPOS:
                    ImpresoraEscPos.imprimir(config.getNombreImpresora(), lineas, abrirCajon && config.isAbrirCajon());
                    break;
                case ConfiguracionLocal.SISTEMA:
                    ImpresoraSistema.imprimir(config.getNombreImpresora(), lineas);
                    break;
                default:
                    throw new NegocioException("No hay una impresora configurada en esta terminal. "
                            + "Configúrela en el módulo Configuración.");
            }
        } catch (NegocioException e) {
            throw e;
        } catch (Exception e) {
            throw new NegocioException("No se pudo imprimir: " + e.getMessage());
        }
    }

    private GeneradorRecibo crearGenerador() throws SQLException {
        NegocioDTO negocio = configuracionService.obtenerNegocio();
        int ancho = ConfiguracionLocal.cargar().getCaracteresPorLinea();
        return new GeneradorRecibo(negocio, ancho);
    }
}
