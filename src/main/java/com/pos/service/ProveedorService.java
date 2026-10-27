package com.pos.service;

import com.pos.config.Conexion;
import com.pos.dao.AuditoriaDAO;
import com.pos.dao.ProveedorDAO;
import com.pos.dto.ProveedorDTO;
import com.pos.util.Permiso;
import com.pos.util.Sesion;
import com.pos.util.Textos;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class ProveedorService {

    private final ProveedorDAO proveedorDAO = new ProveedorDAO();
    private final AuditoriaDAO auditoriaDAO = new AuditoriaDAO();

    public List<ProveedorDTO> buscar(String texto, boolean incluirInactivos) throws NegocioException, SQLException {
        verificarPermiso();
        String filtro = (texto == null) ? "" : texto.trim();
        try (Connection con = Conexion.getConexion()) {
            return proveedorDAO.buscar(con, filtro, incluirInactivos);
        }
    }

    /** Crea el proveedor si es nuevo (id = 0) o lo actualiza si ya existe. */
    public void guardar(ProveedorDTO proveedor) throws NegocioException, SQLException {
        verificarPermiso();
        proveedor.setNombre(Textos.limpiar(proveedor.getNombre()));
        proveedor.setDocumento(Textos.limpiar(proveedor.getDocumento()));
        proveedor.setTelefono(Textos.limpiar(proveedor.getTelefono()));
        proveedor.setEmail(Textos.limpiar(proveedor.getEmail()));
        proveedor.setDireccion(Textos.limpiar(proveedor.getDireccion()));

        if (proveedor.getNombre() == null) {
            throw new NegocioException("Ingrese el nombre del proveedor.");
        }
        if (proveedor.getEmail() != null && !Textos.esEmailValido(proveedor.getEmail())) {
            throw new NegocioException("El email no es válido.");
        }

        Connection con = null;
        try {
            con = Conexion.getConexion();
            con.setAutoCommit(false);

            if (proveedor.getDocumento() != null) {
                ProveedorDTO existente = proveedorDAO.buscarPorDocumento(con, proveedor.getDocumento());
                if (existente != null && existente.getId() != proveedor.getId()) {
                    throw new NegocioException("Ya existe un proveedor con el documento " + proveedor.getDocumento() + ".");
                }
            }

            Integer usuarioId = Sesion.getUsuario() != null ? Sesion.getUsuario().getId() : null;
            String terminal = Sesion.getTerminal();

            if (proveedor.getId() == 0) {
                int id = proveedorDAO.insertar(con, proveedor);
                proveedor.setId(id);
                auditoriaDAO.registrar(con, usuarioId, terminal, "PROVEEDOR_CREADO", "proveedores",
                        String.valueOf(id), "Se creó el proveedor: " + proveedor.getNombre());
            } else {
                proveedorDAO.actualizar(con, proveedor);
                auditoriaDAO.registrar(con, usuarioId, terminal, "PROVEEDOR_MODIFICADO", "proveedores",
                        String.valueOf(proveedor.getId()), "Se actualizó el proveedor ID: " + proveedor.getId()
                                + " (" + proveedor.getNombre() + ")");
            }

            con.commit();
        } catch (SQLException | NegocioException e) {
            Conexion.rollback(con);
            throw e;
        } finally {
            Conexion.cerrar(con);
        }
    }

    private void verificarPermiso() throws NegocioException {
        if (!Sesion.tienePermiso(Permiso.PROVEEDORES_GESTIONAR)) {
            throw new NegocioException("No tiene permiso para administrar proveedores.");
        }
    }
}
