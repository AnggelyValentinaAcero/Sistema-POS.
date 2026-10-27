package com.pos.app;

import com.pos.config.Conexion;
import com.pos.config.ConfiguracionLocal;
import com.pos.controller.ConexionController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;

import java.sql.SQLException;

/**
 * Clase principal de JavaFX. Decide qué ventana mostrar al iniciar:
 * - Si no hay configuración guardada, o la conexión falla: ventana de conexión.
 * - Si la conexión funciona: ventana de login.
 */
public class PosApplication extends Application {

    @Override
    public void start(Stage stage) {
        Ventanas.setVentanaPrincipal(stage);

        ConfiguracionLocal config = ConfiguracionLocal.cargar();
        if (!config.isConexionGuardada()) {
            Ventanas.cambiarVista("/fxml/conexion.fxml");
        } else {
            try {
                Conexion.probar(config);
                Conexion.setConfiguracion(config);
                Ventanas.cambiarVista("/fxml/login.fxml");
            } catch (SQLException e) {
                FXMLLoader loader = Ventanas.cambiarVista("/fxml/conexion.fxml");
                ConexionController controller = loader.getController();
                controller.mostrarError(Conexion.traducirError(e));
            }
        }

        stage.setTitle("Sistema POS");
        stage.setMinWidth(1024);
        stage.setMinHeight(700);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
