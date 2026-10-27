package com.pos.service;

import com.pos.config.Conexion;
import com.pos.dao.ReporteDAO;
import com.pos.dto.ItemReporteDTO;
import com.pos.dto.ResumenVentasDTO;
import com.pos.util.Permiso;
import com.pos.util.Sesion;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reportes para el dueño del negocio (Business Intelligence):
 * cuánto se vendió, cuánto se ganó, qué productos se venden más, etc.
 */
public class ReporteService {

    public static final int TOP_PRODUCTOS = 10;
    private static final int MAXIMO_DIAS = 366;

    private final ReporteDAO reporteDAO = new ReporteDAO();

    public ResumenVentasDTO resumen(LocalDate desde, LocalDate hasta) throws NegocioException, SQLException {
        validar(desde, hasta);
        try (Connection con = Conexion.getConexion()) {
            return reporteDAO.resumen(con, desde, hasta);
        }
    }

    /**
     * Ventas día por día. Los días sin ventas también se incluyen (con 0),
     * para que el gráfico no salte días.
     */
    public List<ItemReporteDTO> ventasPorDia(LocalDate desde, LocalDate hasta) throws NegocioException, SQLException {
        validar(desde, hasta);
        List<ItemReporteDTO> conVentas;
        try (Connection con = Conexion.getConexion()) {
            conVentas = reporteDAO.ventasPorDia(con, desde, hasta);
        }

        // se pasan a un mapa fecha -> datos para buscarlos rápido
        Map<String, ItemReporteDTO> porFecha = new HashMap<>();
        for (ItemReporteDTO item : conVentas) {
            porFecha.put(item.getNombre(), item);
        }

        List<ItemReporteDTO> todosLosDias = new ArrayList<>();
        for (LocalDate dia = desde; !dia.isAfter(hasta); dia = dia.plusDays(1)) {
            ItemReporteDTO item = porFecha.get(dia.toString());
            if (item == null) {
                item = new ItemReporteDTO(dia.toString(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
            }
            todosLosDias.add(item);
        }
        return todosLosDias;
    }

    public List<ItemReporteDTO> productosMasVendidos(LocalDate desde, LocalDate hasta) throws NegocioException, SQLException {
        validar(desde, hasta);
        try (Connection con = Conexion.getConexion()) {
            return reporteDAO.productosMasVendidos(con, desde, hasta, TOP_PRODUCTOS);
        }
    }

    public List<ItemReporteDTO> ventasPorCajero(LocalDate desde, LocalDate hasta) throws NegocioException, SQLException {
        validar(desde, hasta);
        try (Connection con = Conexion.getConexion()) {
            return reporteDAO.ventasPorCajero(con, desde, hasta);
        }
    }

    public List<ItemReporteDTO> ventasPorMetodoPago(LocalDate desde, LocalDate hasta) throws NegocioException, SQLException {
        validar(desde, hasta);
        try (Connection con = Conexion.getConexion()) {
            return reporteDAO.ventasPorMetodoPago(con, desde, hasta);
        }
    }

    private void validar(LocalDate desde, LocalDate hasta) throws NegocioException {
        if (!Sesion.tienePermiso(Permiso.REPORTES_VER)) {
            throw new NegocioException("No tiene permiso para ver reportes.");
        }
        if (desde == null || hasta == null || desde.isAfter(hasta)) {
            throw new NegocioException("Seleccione un rango de fechas válido.");
        }
        if (ChronoUnit.DAYS.between(desde, hasta) > MAXIMO_DIAS) {
            throw new NegocioException("El rango máximo es de un año.");
        }
    }
}
