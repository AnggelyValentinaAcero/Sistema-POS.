package com.pos.service;

import com.pos.config.Conexion;
import com.pos.dao.AuditoriaDAO;
import com.pos.dao.ConfiguracionDAO;
import com.pos.dto.NegocioDTO;
import com.pos.util.Permiso;
import com.pos.util.Sesion;
import com.pos.util.Textos;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

/**
 * Lectura y modificación de los datos del negocio.
 */
public class ConfiguracionService {

    private final ConfiguracionDAO configuracionDAO = new ConfiguracionDAO();
    private final AuditoriaDAO auditoriaDAO = new AuditoriaDAO();

    public NegocioDTO obtenerNegocio() throws SQLException {
        try (Connection con = Conexion.getConexion()) {
            Map<String, String> valores = configuracionDAO.obtenerTodo(con);
            NegocioDTO negocio = new NegocioDTO();
            negocio.setNombre(valores.getOrDefault("negocio.nombre", "Mi Negocio"));
            negocio.setDocumento(valores.getOrDefault("negocio.documento", ""));
            negocio.setDireccion(valores.getOrDefault("negocio.direccion", ""));
            negocio.setTelefono(valores.getOrDefault("negocio.telefono", ""));
            negocio.setSimboloMoneda(valores.getOrDefault("moneda.simbolo", "$"));
            String mensajePie = valores.get("recibo.mensaje_pie");
            if (mensajePie == null) {
                mensajePie = valores.getOrDefault("ticket.mensaje_pie", "");
            }
            negocio.setMensajePie(mensajePie);
            return negocio;
        }
    }

    public void guardarNegocio(NegocioDTO negocio) throws NegocioException, SQLException {
        if (!Sesion.tienePermiso(Permiso.CONFIGURACION_GESTIONAR)) {
            throw new NegocioException("No tiene permiso para modificar la configuración.");
        }
        if (Textos.estaVacio(negocio.getNombre())) {
            throw new NegocioException("Ingrese el nombre del negocio.");
        }
        if (Textos.estaVacio(negocio.getSimboloMoneda())) {
            throw new NegocioException("Ingrese el símbolo de la moneda.");
        }

        Connection con = null;
        try {
            con = Conexion.getConexion();
            con.setAutoCommit(false);
            configuracionDAO.guardar(con, "negocio.nombre", negocio.getNombre().trim());
            configuracionDAO.guardar(con, "negocio.documento", texto(negocio.getDocumento()));
            configuracionDAO.guardar(con, "negocio.direccion", texto(negocio.getDireccion()));
            configuracionDAO.guardar(con, "negocio.telefono", texto(negocio.getTelefono()));
            configuracionDAO.guardar(con, "moneda.simbolo", negocio.getSimboloMoneda().trim());
            configuracionDAO.guardar(con, "recibo.mensaje_pie", texto(negocio.getMensajePie()));
            configuracionDAO.guardar(con, "ticket.mensaje_pie", texto(negocio.getMensajePie()));
            auditoriaDAO.registrar(con, Sesion.getUsuario().getId(), Sesion.getTerminal(), "CONFIGURACION_MODIFICADA",
                    "configuracion", null, "datos del negocio");
            con.commit();
        } catch (SQLException e) {
            Conexion.rollback(con);
            throw e;
        } finally {
            Conexion.cerrar(con);
        }
    }

    private String texto(String valor) {
        return valor == null ? "" : valor.trim();
    }
}
