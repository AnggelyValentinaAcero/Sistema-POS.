package com.pos.controller;

import com.pos.app.Ventanas;
import com.pos.config.Conexion;
import com.pos.config.ConfiguracionLocal;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Controlador de la ventana de configuración de la conexión a MySQL.
 */
public class ConexionController {

    @FXML private TextField txtHost;
    @FXML private TextField txtPuerto;
    @FXML private TextField txtBaseDatos;
    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblMensaje;
    @FXML private ProgressIndicator indicador;
    @FXML private Button btnProbar;
    @FXML private Button btnConectar;

    // initialize() se ejecuta automáticamente después de cargar el FXML
    @FXML
    private void initialize() {
        // si es la primera vez, cargar() devuelve los valores por defecto (localhost, 3306...)
        ConfiguracionLocal config = ConfiguracionLocal.cargar();
        txtHost.setText(config.getHost());
        txtPuerto.setText(String.valueOf(config.getPuerto()));
        txtBaseDatos.setText(config.getBaseDatos());
        txtUsuario.setText(config.getUsuario());
        txtPassword.setText(config.getPassword());
    }

    public void mostrarError(String mensaje) {
        mostrarMensaje(mensaje, false);
    }

    @FXML
    private void probar() {
        ConfiguracionLocal config = leerFormulario();
        if (config != null) {
            probarEnSegundoPlano(config, false);
        }
    }

    @FXML
    private void conectar() {
        ConfiguracionLocal config = leerFormulario();
        if (config != null) {
            probarEnSegundoPlano(config, true);
        }
    }

    /**
     * Prueba la conexión en un hilo aparte. Si el servidor no responde la prueba
     * puede tardar varios segundos, y así la ventana no se queda congelada.
     */
    private void probarEnSegundoPlano(ConfiguracionLocal config, boolean guardarAlTerminar) {
        Task<String> tarea = new Task<>() {
            @Override
            protected String call() throws Exception {
                return Conexion.probar(config);
            }
        };

        // setOnSucceeded y setOnFailed se ejecutan de nuevo en el hilo de JavaFX
        tarea.setOnSucceeded(evento -> {
            bloquear(false);
            if (guardarAlTerminar) {
                guardarYContinuar(config);
            } else {
                mostrarMensaje("Conexión exitosa: " + tarea.getValue(), true);
            }
        });
        tarea.setOnFailed(evento -> {
            bloquear(false);
            Throwable error = tarea.getException();
            if (error instanceof SQLException) {
                mostrarError(Conexion.traducirError((SQLException) error));
            } else {
                mostrarError("Error: " + error.getMessage());
            }
        });

        bloquear(true);
        new Thread(tarea).start();
    }

    private void guardarYContinuar(ConfiguracionLocal config) {
        try {
            config.guardarConexion();
        } catch (IOException e) {
            mostrarError("No se pudo guardar la configuración: " + e.getMessage());
            return;
        }
        Conexion.setConfiguracion(config);
        Ventanas.cambiarVista("/fxml/login.fxml");
    }

    private ConfiguracionLocal leerFormulario() {
        if (txtHost.getText().isBlank() || txtBaseDatos.getText().isBlank() || txtUsuario.getText().isBlank()) {
            mostrarError("Complete servidor, base de datos y usuario.");
            return null;
        }
        int puerto;
        try {
            puerto = Integer.parseInt(txtPuerto.getText().trim());
        } catch (NumberFormatException e) {
            mostrarError("El puerto debe ser un número (normalmente 3306).");
            return null;
        }
        return new ConfiguracionLocal(txtHost.getText().trim(), puerto, txtBaseDatos.getText().trim(),
                txtUsuario.getText().trim(), txtPassword.getText());
    }

    private void bloquear(boolean ocupado) {
        indicador.setVisible(ocupado);
        btnProbar.setDisable(ocupado);
        btnConectar.setDisable(ocupado);
        if (ocupado) {
            lblMensaje.setVisible(false);
            lblMensaje.setManaged(false);
        }
    }

    private void mostrarMensaje(String texto, boolean exito) {
        lblMensaje.setText(texto);
        lblMensaje.getStyleClass().removeAll("mensaje-exito", "mensaje-error");
        lblMensaje.getStyleClass().add(exito ? "mensaje-exito" : "mensaje-error");
        lblMensaje.setVisible(true);
        lblMensaje.setManaged(true);
    }
}
