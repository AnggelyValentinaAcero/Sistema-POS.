package com.pos.controller;

import com.pos.app.Ventanas;
import com.pos.config.Conexion;
import com.pos.config.ConfiguracionLocal;
import com.pos.dto.UsuarioDTO;
import com.pos.service.AuthService;
import com.pos.service.NegocioException;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;

import java.sql.SQLException;

/**
 * Controlador de la ventana de inicio de sesión.
 */
public class LoginController {

    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblMensaje;
    @FXML private Label lblConexion;
    @FXML private ProgressIndicator indicador;
    @FXML private Button btnIngresar;

    private final AuthService authService = new AuthService();

    @FXML
    private void initialize() {
        ConfiguracionLocal config = Conexion.getConfiguracion();
        lblConexion.setText("Base de datos: " + config.getBaseDatos() + " en " + config.getHost()
                + "   |   Terminal: " + config.getTerminal());
        // pone el cursor en el campo usuario cuando la ventana termina de mostrarse
        Platform.runLater(() -> txtUsuario.requestFocus());
    }

    @FXML
    private void ingresar() {
        String usuario = txtUsuario.getText();
        String clave = txtPassword.getText();
        if (usuario.isBlank() || clave.isEmpty()) {
            mostrarError("Ingrese usuario y contraseña.");
            return;
        }

        // El login se hace en segundo plano porque verificar BCrypt y consultar
        // la base de datos puede tardar un momento
        String terminal = Conexion.getConfiguracion().getTerminal();
        Task<UsuarioDTO> tarea = new Task<>() {
            @Override
            protected UsuarioDTO call() throws Exception {
                return authService.iniciarSesion(usuario, clave, terminal);
            }
        };
        tarea.setOnSucceeded(evento -> sesionIniciada(tarea.getValue()));
        tarea.setOnFailed(evento -> {
            bloquear(false);
            txtPassword.clear();
            txtPassword.requestFocus();

            Throwable error = tarea.getException();
            if (error instanceof NegocioException) {
                mostrarError(error.getMessage());
            } else if (error instanceof SQLException) {
                mostrarError(Conexion.traducirError((SQLException) error));
            } else {
                error.printStackTrace();
                mostrarError("Error inesperado: " + error.getMessage());
            }
        });

        bloquear(true);
        new Thread(tarea).start();
    }

    @FXML
    private void configurarConexion() {
        Ventanas.cambiarVista("/fxml/conexion.fxml");
    }

    private void sesionIniciada(UsuarioDTO usuario) {
        // Si es el primer ingreso o le restablecieron la clave, debe cambiarla antes de seguir
        if (usuario.isDebeCambiarPassword()) {
            FXMLLoader loader = Ventanas.cargar("/fxml/cambio_password.fxml");
            CambioPasswordController dialogo = loader.getController();
            dialogo.setObligatorio(true);
            Ventanas.mostrarModal(loader, "Cambiar contraseña");

            if (!dialogo.isCambiado()) {
                authService.cerrarSesion();
                bloquear(false);
                txtPassword.clear();
                mostrarError("Debe cambiar su contraseña para continuar.");
                return;
            }
        }
        Ventanas.cambiarVista("/fxml/principal.fxml");
    }

    private void bloquear(boolean ocupado) {
        indicador.setVisible(ocupado);
        btnIngresar.setDisable(ocupado);
        if (ocupado) {
            lblMensaje.setVisible(false);
            lblMensaje.setManaged(false);
        }
    }

    private void mostrarError(String mensaje) {
        lblMensaje.setText(mensaje);
        lblMensaje.setVisible(true);
        lblMensaje.setManaged(true);
    }
}
