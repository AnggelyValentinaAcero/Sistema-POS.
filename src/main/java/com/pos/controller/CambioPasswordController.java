package com.pos.controller;

import com.pos.app.Ventanas;
import com.pos.service.AuthService;
import com.pos.service.NegocioException;
import com.pos.util.Alertas;
import com.pos.util.Claves;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.sql.SQLException;

/**
 * Ventana para que el usuario actual cambie su contraseña.
 */
public class CambioPasswordController {

    @FXML private Label lblAviso;
    @FXML private PasswordField txtActual;
    @FXML private TextField txtActualVisible;
    @FXML private Button btnVerActual;

    @FXML private PasswordField txtNueva;
    @FXML private TextField txtNuevaVisible;
    @FXML private Button btnVerNueva;

    @FXML private PasswordField txtConfirmar;
    @FXML private TextField txtConfirmarVisible;
    @FXML private Button btnVerConfirmar;

    @FXML private Label lblMensaje;
    @FXML private Button btnGuardar;

    private final AuthService authService = new AuthService();
    private boolean cambiado = false;

    @FXML
    private void initialize() {
        lblAviso.setText("La contraseña debe tener mínimo " + Claves.LONGITUD_MINIMA + " caracteres.");
        Ventanas.configurarVisibilidadPassword(txtActual, txtActualVisible, btnVerActual);
        Ventanas.configurarVisibilidadPassword(txtNueva, txtNuevaVisible, btnVerNueva);
        Ventanas.configurarVisibilidadPassword(txtConfirmar, txtConfirmarVisible, btnVerConfirmar);
    }

    /** Si es obligatorio (primer ingreso), se muestra un aviso distinto. */
    public void setObligatorio(boolean obligatorio) {
        if (obligatorio) {
            lblAviso.setText("Por seguridad debe definir una nueva contraseña antes de continuar.");
        }
    }

    public boolean isCambiado() {
        return cambiado;
    }

    @FXML
    private void guardar() {
        if (!txtNueva.getText().equals(txtConfirmar.getText())) {
            mostrarError("La confirmación no coincide con la nueva contraseña.");
            return;
        }
        try {
            authService.cambiarPassword(txtActual.getText(), txtNueva.getText());
            cambiado = true;
            Alertas.info("Contraseña", "La contraseña se cambió correctamente.");
            Ventanas.cerrar(btnGuardar);
        } catch (NegocioException e) {
            mostrarError(e.getMessage());
        } catch (SQLException e) {
            mostrarError("Error de base de datos: " + e.getMessage());
        }
    }

    @FXML
    private void cancelar() {
        Ventanas.cerrar(btnGuardar);
    }

    private void mostrarError(String mensaje) {
        lblMensaje.setText(mensaje);
        lblMensaje.setVisible(true);
        lblMensaje.setManaged(true);
        Ventanas.ajustarTamano(lblMensaje);
    }
}
