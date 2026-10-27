package com.pos.service;

import com.pos.config.Conexion;
import com.pos.dao.AuditoriaDAO;
import com.pos.dao.UsuarioDAO;
import com.pos.dto.AuditoriaDTO;
import com.pos.dto.UsuarioDTO;
import com.pos.util.Permiso;
import com.pos.util.Sesion;
import com.pos.util.Textos;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Consulta del registro de auditoría (solo lectura: la auditoría nunca se modifica).
 */
public class AuditoriaService {

    public static final int MAXIMO_REGISTROS = 1000;

    private final AuditoriaDAO auditoriaDAO = new AuditoriaDAO();
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    /**
     * @param usuarioId 0 o null = todos los usuarios
     * @param accion    null o vacío = todas las acciones
     * @param texto     texto a buscar en el detalle (opcional)
     */
    public List<AuditoriaDTO> buscar(LocalDate desde, LocalDate hasta, Integer usuarioId, String accion, String texto)
            throws NegocioException, SQLException {
        verificarPermiso();
        if (desde == null || hasta == null || desde.isAfter(hasta)) {
            throw new NegocioException("Seleccione un rango de fechas válido.");
        }
        if (usuarioId != null && usuarioId <= 0) {
            usuarioId = null;
        }
        try (Connection con = Conexion.getConexion()) {
            return auditoriaDAO.listar(con, desde, hasta, usuarioId, Textos.limpiar(accion),
                    Textos.limpiar(texto), MAXIMO_REGISTROS);
        }
    }

    public List<String> listarAcciones() throws NegocioException, SQLException {
        verificarPermiso();
        try (Connection con = Conexion.getConexion()) {
            return auditoriaDAO.listarAcciones(con);
        }
    }

    public List<UsuarioDTO> listarUsuarios() throws NegocioException, SQLException {
        verificarPermiso();
        try (Connection con = Conexion.getConexion()) {
            return usuarioDAO.listar(con);
        }
    }

    private void verificarPermiso() throws NegocioException {
        if (!Sesion.tienePermiso(Permiso.AUDITORIA_VER)) {
            throw new NegocioException("No tiene permiso para ver la auditoría.");
        }
    }
}
