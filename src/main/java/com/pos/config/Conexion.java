package com.pos.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Clase encargada de abrir conexiones a la base de datos MySQL usando JDBC.
 * Toda la aplicación pide sus conexiones aquí, así los datos de acceso
 * están en un solo lugar.
 */
public class Conexion {

    private static ConfiguracionLocal configuracion;

    public static void setConfiguracion(ConfiguracionLocal config) {
        configuracion = config;
    }

    public static ConfiguracionLocal getConfiguracion() {
        return configuracion;
    }

    /**
     * Abre una nueva conexión. Quien la pide es responsable de cerrarla
     * (normalmente con try-with-resources).
     */
    public static Connection getConexion() throws SQLException {
        if (configuracion == null) {
            throw new SQLException("No se ha configurado la conexión a la base de datos");
        }
        return DriverManager.getConnection(configuracion.getUrl(), configuracion.getUsuario(), configuracion.getPassword());
    }

    /**
     * Prueba si es posible conectarse con los datos indicados.
     * Devuelve la versión del servidor MySQL si la conexión funciona.
     */
    public static String probar(ConfiguracionLocal config) throws SQLException {
        DriverManager.setLoginTimeout(5); // no esperar más de 5 segundos
        try (Connection con = DriverManager.getConnection(config.getUrl(), config.getUsuario(), config.getPassword())) {
            return "MySQL " + con.getMetaData().getDatabaseProductVersion();
        }
    }

    /**
     * Convierte los errores más comunes de conexión en mensajes entendibles.
     */
    public static String traducirError(SQLException e) {
        switch (e.getErrorCode()) {
            case 1045:
                return "Usuario o contraseña de MySQL incorrectos.";
            case 1044:
                return "El usuario de MySQL no tiene permisos sobre esa base de datos.";
            case 1049:
                return "La base de datos no existe. Ejecute el script database/base_datos.sql.";
            case 0:
                return "No se pudo conectar con el servidor. Verifique que MySQL esté encendido y que el servidor y el puerto sean correctos.";
            default:
                return "Error de base de datos: " + e.getMessage();
        }
    }

    /** Deshace los cambios de una transacción si algo falló. */
    public static void rollback(Connection con) {
        if (con != null) {
            try {
                con.rollback();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /** Cierra la conexión sin lanzar excepciones (se usa en los bloques finally). */
    public static void cerrar(Connection con) {
        if (con != null) {
            try {
                con.setAutoCommit(true);
                con.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
