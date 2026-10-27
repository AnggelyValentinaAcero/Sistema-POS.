package com.pos.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class ConfiguracionDAO {

    public Map<String, String> obtenerTodo(Connection con) throws SQLException {
        Map<String, String> valores = new HashMap<>();
        String sql = "SELECT clave, valor FROM configuracion";
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                valores.put(rs.getString("clave"), rs.getString("valor"));
            }
        }
        return valores;
    }

    public void guardar(Connection con, String clave, String valor) throws SQLException {
        // ON DUPLICATE KEY UPDATE: si la clave ya existe (es la PRIMARY KEY) se actualiza el valor
        String sql = "INSERT INTO configuracion (clave, valor) VALUES (?, ?) "
                + "ON DUPLICATE KEY UPDATE valor = VALUES(valor)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, clave);
            ps.setString(2, valor);
            ps.executeUpdate();
        }
    }
}
