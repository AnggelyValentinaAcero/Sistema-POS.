package com.pos.print;

import com.pos.dto.DetalleVentaDTO;
import com.pos.dto.NegocioDTO;
import com.pos.dto.PagoDTO;
import com.pos.dto.TurnoCajaDTO;
import com.pos.dto.VentaDTO;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Arma el contenido de los recibos: el de venta y el comprobante de cierre de caja.
 */
public class GeneradorRecibo {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final NegocioDTO negocio;
    private final int ancho;

    public GeneradorRecibo(NegocioDTO negocio, int ancho) {
        this.negocio = negocio;
        this.ancho = ancho;
    }

    public List<String> reciboVenta(VentaDTO venta, boolean reimpresion) {
        ReciboTexto t = new ReciboTexto(ancho);
        encabezado(t);

        if (reimpresion) {
            t.centrado("*** REIMPRESION ***");
        }
        if ("ANULADA".equals(venta.getEstado())) {
            t.centrado("*** VENTA ANULADA ***");
        }
        t.texto("Venta N°: " + venta.getId());
        t.texto("Fecha: " + venta.getFecha().format(FECHA));
        t.texto("Cajero: " + venta.getUsuario());
        t.texto("Cliente: " + venta.getClienteTexto());
        t.separador();

        // Cada producto ocupa dos líneas: el nombre y "cantidad x precio ..... subtotal"
        for (DetalleVentaDTO d : venta.getDetalles()) {
            t.texto(d.getNombreProducto());
            t.columnas("  " + d.getCantidadTexto() + " x " + numero(d.getPrecioUnitario()), numero(d.getSubtotal()));
        }
        t.separador();

        t.columnas("Subtotal", moneda(venta.getSubtotal()));
        t.columnas("Impuestos", moneda(venta.getImpuesto()));
        t.columnas("TOTAL", moneda(venta.getTotal()));
        t.separador();

        PagoDTO pago = venta.getPago();
        if (pago != null) {
            t.columnas("Pago: " + pago.getMetodoPago(), moneda(pago.getMonto()));
            if (pago.getCambio() != null && pago.getCambio().compareTo(BigDecimal.ZERO) > 0) {
                t.columnas("Recibido", moneda(pago.getMontoRecibido()));
                t.columnas("Cambio", moneda(pago.getCambio()));
            }
            if (pago.getReferencia() != null) {
                t.texto("Ref: " + pago.getReferencia());
            }
        }

        t.lineaVacia();
        t.centrado(negocio.getMensajePie());
        return t.getLineas();
    }

    public List<String> reciboCierreCaja(TurnoCajaDTO turno, Map<String, BigDecimal> totales, int cantidadVentas) {
        ReciboTexto t = new ReciboTexto(ancho);
        encabezado(t);

        t.centrado("CIERRE DE CAJA");
        t.lineaVacia();
        t.texto("Turno N°: " + turno.getId());
        t.texto("Terminal: " + turno.getTerminal());
        t.texto("Abierto por: " + turno.getUsuario());
        t.texto("Apertura: " + turno.getFechaApertura().format(FECHA));
        LocalDateTime cierre = turno.getFechaCierre() != null ? turno.getFechaCierre() : LocalDateTime.now();
        t.texto("Cierre:   " + cierre.format(FECHA));
        t.separador();

        t.columnas("Ventas realizadas", String.valueOf(cantidadVentas));
        for (String metodo : totales.keySet()) {
            t.columnas("Ventas " + metodo, moneda(totales.get(metodo)));
        }
        t.separador();

        t.columnas("Monto inicial", moneda(turno.getMontoInicial()));
        t.columnas("Efectivo esperado", moneda(turno.getMontoEsperado()));
        t.columnas("Efectivo contado", moneda(turno.getMontoContado()));

        BigDecimal diferencia = turno.getDiferencia();
        if (diferencia.signum() < 0) {
            t.columnas("FALTANTE", moneda(diferencia.negate()));
        } else if (diferencia.signum() > 0) {
            t.columnas("SOBRANTE", moneda(diferencia));
        } else {
            t.columnas("Diferencia", moneda(diferencia));
        }
        if (turno.getObservaciones() != null) {
            t.separador();
            t.texto("Obs: " + turno.getObservaciones());
        }

        t.lineaVacia();
        t.lineaVacia();
        t.centrado("______________________");
        t.centrado("Firma del cajero");
        return t.getLineas();
    }

    private void encabezado(ReciboTexto t) {
        t.centrado(negocio.getNombre());
        if (!negocio.getDocumento().isEmpty()) {
            t.centrado(negocio.getDocumento());
        }
        t.centrado(negocio.getDireccion());
        if (!negocio.getTelefono().isEmpty()) {
            t.centrado("Tel: " + negocio.getTelefono());
        }
        t.separador();
    }

    private String numero(BigDecimal valor) {
        return new DecimalFormat("#,##0.00").format(valor);
    }

    private String moneda(BigDecimal valor) {
        return negocio.getSimboloMoneda() + " " + numero(valor);
    }
}
