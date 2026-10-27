package com.pos.dao;

import com.pos.dto.ProveedorDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ProveedorDAO {

    private static final String SELECT_PROVEEDOR =
            "SELECT id, nombre, documento, telefono, email, direccion, activo FROM proveedores ";

    public List<ProveedorDTO> buscar(Connection con, String texto, boolean incluirInactivos) throws SQLException {
        List<ProveedorDTO> lista = new ArrayList<>();
        String sql = SELECT_PROVEEDOR + "WHERE (nombre LIKE ? OR documento LIKE ?) ";
        if (!incluirInactivos) {
            sql += "AND activo = TRUE ";
        }
        sql += "ORDER BY nombre ASC, documento ASC";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, "%" + texto + "%");
            ps.setString(2, "%" + texto + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(leerProveedor(rs));
                }
            }
        }
        return lista;
    }

    public ProveedorDTO buscarPorDocumento(Connection con, String documento) throws SQLException {
        String sql = SELECT_PROVEEDOR + "WHERE documento = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, documento);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return leerProveedor(rs);
                }
            }
        }
        return null;
    }

    public int insertar(Connection con, ProveedorDTO p) throws SQLException {
        String sql = "INSERT INTO proveedores (nombre, documento, telefono, email, direccion, activo) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getNombre());
            ps.setString(2, p.getDocumento());
            ps.setString(3, p.getTelefono());
            ps.setString(4, p.getEmail());
            ps.setString(5, p.getDireccion());
            ps.setBoolean(6, p.isActivo());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public void actualizar(Connection con, ProveedorDTO p) throws SQLException {
        String sql = "UPDATE proveedores SET nombre = ?, documento = ?, telefono = ?, email = ?, direccion = ?, activo = ? "
                + "WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, p.getNombre());
            ps.setString(2, p.getDocumento());
            ps.setString(3, p.getTelefono());
            ps.setString(4, p.getEmail());
            ps.setString(5, p.getDireccion());
            ps.setBoolean(6, p.isActivo());
            ps.setInt(7, p.getId());
            ps.executeUpdate();
        }
    }

    private ProveedorDTO leerProveedor(ResultSet rs) throws SQLException {
        ProveedorDTO p = new ProveedorDTO();
        p.setId(rs.getInt("id"));
        p.setNombre(rs.getString("nombre"));
        p.setDocumento(rs.getString("documento"));
        p.setTelefono(rs.getString("telefono"));
        p.setEmail(rs.getString("email"));
        p.setDireccion(rs.getString("direccion"));
        p.setActivo(rs.getBoolean("activo"));
        return p;
    }
}
