package com.pos.service;

import com.pos.config.Conexion;
import com.pos.dao.AuditoriaDAO;
import com.pos.dao.TurnoCajaDAO;
import com.pos.dto.MetodoPagoDTO;
import com.pos.dto.TurnoCajaDTO;
import com.pos.util.Permiso;
import com.pos.util.Sesion;
import com.pos.util.Textos;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

/**
 * Apertura y cierre de caja (turnos). Cada terminal puede tener un solo turno abierto,
 * y no se puede vender si la caja de la terminal está cerrada.
 */
public class CajaService {

    private final TurnoCajaDAO turnoDAO = new TurnoCajaDAO();
    private final AuditoriaDAO auditoriaDAO = new AuditoriaDAO();

    /** Devuelve el turno abierto de esta terminal, o null si la caja está cerrada. */
    public TurnoCajaDTO buscarTurnoAbierto() throws SQLException {
        try (Connection con = Conexion.getConexion()) {
            return turnoDAO.buscarAbierto(con, Sesion.getTerminal());
        }
    }

    public TurnoCajaDTO abrirCaja(BigDecimal montoInicial) throws NegocioException, SQLException {
        verificarPermiso();
        if (montoInicial == null || montoInicial.compareTo(BigDecimal.ZERO) < 0) {
            throw new NegocioException("El monto inicial no puede ser negativo.");
        }

        try (Connection con = Conexion.getConexion()) {
            if (turnoDAO.buscarAbierto(con, Sesion.getTerminal()) != null) {
                throw new NegocioException("La caja de esta terminal ya está abierta.");
            }
            TurnoCajaDTO turno = new TurnoCajaDTO();
            turno.setTerminal(Sesion.getTerminal());
            turno.setUsuarioId(Sesion.getUsuario().getId());
            turno.setMontoInicial(montoInicial);
            int id = turnoDAO.abrir(con, turno);

            auditoriaDAO.registrar(con, Sesion.getUsuario().getId(), Sesion.getTerminal(), "CAJA_ABIERTA",
                    "turnos_caja", String.valueOf(id), "monto inicial: " + montoInicial);
            return turnoDAO.buscarAbierto(con, Sesion.getTerminal());
        }
    }

    public Map<String, BigDecimal> totalesPorMetodoPago(int turnoId) throws SQLException {
        try (Connection con = Conexion.getConexion()) {
            return turnoDAO.totalesPorMetodoPago(con, turnoId);
        }
    }

    public int contarVentas(int turnoId) throws SQLException {
        try (Connection con = Conexion.getConexion()) {
            return turnoDAO.contarVentas(con, turnoId);
        }
    }

    /**
     * Efectivo que debería haber en el cajón = monto inicial + ventas en efectivo.
     * (Las ventas con tarjeta o transferencia no entran al cajón.)
     */
    public BigDecimal calcularEfectivoEsperado(TurnoCajaDTO turno, Map<String, BigDecimal> totales) {
        BigDecimal ventasEfectivo = totales.get(MetodoPagoDTO.EFECTIVO);
        if (ventasEfectivo == null) {
            ventasEfectivo = BigDecimal.ZERO;
        }
        return turno.getMontoInicial().add(ventasEfectivo);
    }

    /**
     * Cierra el turno comparando el efectivo contado con el esperado.
     * Una diferencia negativa es un faltante; una positiva, un sobrante.
     */
    public TurnoCajaDTO cerrarCaja(BigDecimal montoContado, String observaciones) throws NegocioException, SQLException {
        verificarPermiso();
        if (montoContado == null || montoContado.compareTo(BigDecimal.ZERO) < 0) {
            throw new NegocioException("Ingrese el efectivo contado en caja.");
        }

        Connection con = null;
        try {
            con = Conexion.getConexion();
            con.setAutoCommit(false);

            TurnoCajaDTO turno = turnoDAO.buscarAbierto(con, Sesion.getTerminal());
            if (turno == null) {
                throw new NegocioException("La caja de esta terminal no está abierta.");
            }
            Map<String, BigDecimal> totales = turnoDAO.totalesPorMetodoPago(con, turno.getId());
            BigDecimal esperado = calcularEfectivoEsperado(turno, totales);

            turno.setMontoEsperado(esperado);
            turno.setMontoContado(montoContado);
            turno.setDiferencia(montoContado.subtract(esperado));
            turno.setObservaciones(Textos.limpiar(observaciones));
            turnoDAO.cerrar(con, turno);

            auditoriaDAO.registrar(con, Sesion.getUsuario().getId(), Sesion.getTerminal(), "CAJA_CERRADA",
                    "turnos_caja", String.valueOf(turno.getId()),
                    "esperado: " + esperado + ", contado: " + montoContado + ", diferencia: " + turno.getDiferencia());

            con.commit();
            return turno;
        } catch (SQLException | NegocioException e) {
            Conexion.rollback(con);
            throw e;
        } finally {
            Conexion.cerrar(con);
        }
    }

    private void verificarPermiso() throws NegocioException {
        if (!Sesion.tienePermiso(Permiso.CAJA_OPERAR)) {
            throw new NegocioException("No tiene permiso para abrir o cerrar la caja.");
        }
    }
}
