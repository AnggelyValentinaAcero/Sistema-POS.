package com.pos.service;

import com.pos.config.Conexion;
import com.pos.dao.AuditoriaDAO;
import com.pos.dao.ClienteDAO;
import com.pos.dto.ClienteDTO;
import com.pos.util.Permiso;
import com.pos.util.Sesion;
import com.pos.util.Textos;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class ClienteService {

    public static final String[] TIPOS_DOCUMENTO = {"Cédula / DNI", "NIT / RUC", "Pasaporte", "Otro"};

    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final AuditoriaDAO auditoriaDAO = new AuditoriaDAO();

    public List<ClienteDTO> buscar(String texto, boolean incluirInactivos) throws NegocioException, SQLException {
        verificarPermiso();
        String filtro = (texto == null) ? "" : texto.trim();
        try (Connection con = Conexion.getConexion()) {
            return clienteDAO.buscar(con, filtro, incluirInactivos);
        }
    }

    /** Crea el cliente si es nuevo (id = 0) o lo actualiza si ya existe. */
    public void guardar(ClienteDTO cliente) throws NegocioException, SQLException {
        verificarPermiso();
        cliente.setNombre(Textos.limpiar(cliente.getNombre()));
        cliente.setDocumento(Textos.limpiar(cliente.getDocumento()));
        cliente.setTelefono(Textos.limpiar(cliente.getTelefono()));
        cliente.setEmail(Textos.limpiar(cliente.getEmail()));
        cliente.setDireccion(Textos.limpiar(cliente.getDireccion()));

        if (cliente.getNombre() == null) {
            throw new NegocioException("Ingrese el nombre del cliente.");
        }
        if (cliente.getDocumento() != null && cliente.getTipoDocumento() == null) {
            throw new NegocioException("Seleccione el tipo de documento.");
        }
        if (cliente.getDocumento() == null) {
            cliente.setTipoDocumento(null);
        }
        if (cliente.getEmail() != null && !Textos.esEmailValido(cliente.getEmail())) {
            throw new NegocioException("El email no es válido.");
        }

        Connection con = null;
        try {
            con = Conexion.getConexion();
            con.setAutoCommit(false);

            if (cliente.getDocumento() != null) {
                ClienteDTO existente = clienteDAO.buscarPorDocumento(con, cliente.getDocumento());
                if (existente != null && existente.getId() != cliente.getId()) {
                    throw new NegocioException("Ya existe un cliente con el documento " + cliente.getDocumento()
                            + " (" + existente.getNombre() + ").");
                }
            }

            Integer usuarioId = Sesion.getUsuario() != null ? Sesion.getUsuario().getId() : null;
            String terminal = Sesion.getTerminal();

            if (cliente.getId() == 0) {
                int id = clienteDAO.insertar(con, cliente);
                cliente.setId(id);
                auditoriaDAO.registrar(con, usuarioId, terminal, "CLIENTE_CREADO", "clientes",
                        String.valueOf(id), "Se creó el cliente: " + cliente.getNombre());
            } else {
                clienteDAO.actualizar(con, cliente);
                auditoriaDAO.registrar(con, usuarioId, terminal, "CLIENTE_MODIFICADO", "clientes",
                        String.valueOf(cliente.getId()), "Se actualizó el cliente ID: " + cliente.getId() + " (" + cliente.getNombre() + ")");
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
        if (!Sesion.tienePermiso(Permiso.CLIENTES_GESTIONAR)) {
            throw new NegocioException("No tiene permiso para administrar clientes.");
        }
    }
}
