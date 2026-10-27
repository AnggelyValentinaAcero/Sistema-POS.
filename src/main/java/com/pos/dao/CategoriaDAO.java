package com.pos.dao;

import com.pos.dto.CategoriaDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class CategoriaDAO {

    public List<CategoriaDTO> listar(Connection con, boolean incluirInactivas) throws SQLException {
        List<CategoriaDTO> lista = new ArrayList<>();
        String sql = "SELECT id, nombre, descripcion, activo FROM categorias ";
        if (!incluirInactivas) {
            sql += "WHERE activo = TRUE ";
        }
        sql += "ORDER BY nombre ASC";

        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(leerCategoria(rs));
            }
        }
        return lista;
    }

    public CategoriaDTO buscarPorNombre(Connection con, String nombre) throws SQLException {
        String sql = "SELECT id, nombre, descripcion, activo FROM categorias WHERE nombre = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return leerCategoria(rs);
                }
            }
        }
        return null;
    }

    public int insertar(Connection con, CategoriaDTO categoria) throws SQLException {
        String sql = "INSERT INTO categorias (nombre, descripcion, activo) VALUES (?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, categoria.getNombre());
            ps.setString(2, categoria.getDescripcion());
            ps.setBoolean(3, categoria.isActivo());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public void actualizar(Connection con, CategoriaDTO categoria) throws SQLException {
        String sql = "UPDATE categorias SET nombre = ?, descripcion = ?, activo = ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, categoria.getNombre());
            ps.setString(2, categoria.getDescripcion());
            ps.setBoolean(3, categoria.isActivo());
            ps.setInt(4, categoria.getId());
            ps.executeUpdate();
        }
    }

    private CategoriaDTO leerCategoria(ResultSet rs) throws SQLException {
        CategoriaDTO c = new CategoriaDTO();
        c.setId(rs.getInt("id"));
        c.setNombre(rs.getString("nombre"));
        c.setDescripcion(rs.getString("descripcion"));
        c.setActivo(rs.getBoolean("activo"));
        return c;
    }
}
