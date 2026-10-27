package com.pos.dao;

import com.pos.dto.ClienteDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ClienteDAO {

    private static final String SELECT_CLIENTE =
            "SELECT id, tipo_documento, documento, nombre, telefono, email, direccion, activo FROM clientes ";

    public List<ClienteDTO> buscar(Connection con, String texto, boolean incluirInactivos) throws SQLException {
        List<ClienteDTO> lista = new ArrayList<>();
        String sql = SELECT_CLIENTE + "WHERE (nombre LIKE ? OR documento LIKE ?) ";
        if (!incluirInactivos) {
            sql += "AND activo = TRUE ";
        }
        sql += "ORDER BY nombre ASC, documento ASC";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, "%" + texto + "%");
            ps.setString(2, "%" + texto + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(leerCliente(rs));
                }
            }
        }
        return lista;
    }

    public ClienteDTO buscarPorDocumento(Connection con, String documento) throws SQLException {
        String sql = SELECT_CLIENTE + "WHERE documento = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, documento);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return leerCliente(rs);
                }
            }
        }
        return null;
    }

    public int insertar(Connection con, ClienteDTO c) throws SQLException {
        String sql = "INSERT INTO clientes (tipo_documento, documento, nombre, telefono, email, direccion, activo) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, c.getTipoDocumento());
            ps.setString(2, c.getDocumento());
            ps.setString(3, c.getNombre());
            ps.setString(4, c.getTelefono());
            ps.setString(5, c.getEmail());
            ps.setString(6, c.getDireccion());
            ps.setBoolean(7, c.isActivo());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public void actualizar(Connection con, ClienteDTO c) throws SQLException {
        String sql = "UPDATE clientes SET tipo_documento = ?, documento = ?, nombre = ?, telefono = ?, email = ?, "
                + "direccion = ?, activo = ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, c.getTipoDocumento());
            ps.setString(2, c.getDocumento());
            ps.setString(3, c.getNombre());
            ps.setString(4, c.getTelefono());
            ps.setString(5, c.getEmail());
            ps.setString(6, c.getDireccion());
            ps.setBoolean(7, c.isActivo());
            ps.setInt(8, c.getId());
            ps.executeUpdate();
        }
    }

    private ClienteDTO leerCliente(ResultSet rs) throws SQLException {
        ClienteDTO c = new ClienteDTO();
        c.setId(rs.getInt("id"));
        c.setTipoDocumento(rs.getString("tipo_documento"));
        c.setDocumento(rs.getString("documento"));
        c.setNombre(rs.getString("nombre"));
        c.setTelefono(rs.getString("telefono"));
        c.setEmail(rs.getString("email"));
        c.setDireccion(rs.getString("direccion"));
        c.setActivo(rs.getBoolean("activo"));
        return c;
    }
}
