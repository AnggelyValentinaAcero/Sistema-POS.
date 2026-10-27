package com.pos.controller;

import com.pos.app.Ventanas;
import com.pos.dto.RolDTO;
import com.pos.dto.UsuarioDTO;
import com.pos.service.NegocioException;
import com.pos.service.UsuarioService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.sql.SQLException;

/**
 * Formulario para crear o editar un usuario.
 * Si usuario es null se está creando uno nuevo; si no, se está editando.
 */
public class UsuarioFormController {

    @FXML private TextField txtUsuario;
    @FXML private TextField txtNombre;
    @FXML private ComboBox<RolDTO> cmbRol;
    @FXML private VBox panelPassword;
    @FXML private PasswordField txtPassword;
    @FXML private TextField txtPasswordVisible;
    @FXML private Button btnVerPassword;
    @FXML private PasswordField txtConfirmar;
    @FXML private TextField txtConfirmarVisible;
    @FXML private Button btnVerConfirmar;
    @FXML private CheckBox chkActivo;
    @FXML private Label lblMensaje;
    @FXML private Button btnGuardar;

    private final UsuarioService usuarioService = new UsuarioService();
    private UsuarioDTO usuario;
    private boolean guardado = false;

    @FXML
    private void initialize() {
        Ventanas.configurarVisibilidadPassword(txtPassword, txtPasswordVisible, btnVerPassword);
        Ventanas.configurarVisibilidadPassword(txtConfirmar, txtConfirmarVisible, btnVerConfirmar);

        try {
            cmbRol.getItems().setAll(usuarioService.listarRoles());
        } catch (NegocioException | SQLException e) {
            mostrarError("No se pudieron cargar los roles: " + e.getMessage());
        }
    }

    public void setUsuario(UsuarioDTO usuario) {
        this.usuario = usuario;

        if (usuario == null) {
            // usuario nuevo: siempre se crea activo
            chkActivo.setSelected(true);
            chkActivo.setDisable(true);
        } else {
            // edición: el nombre de usuario no se cambia y la contraseña
            // se maneja con "Restablecer contraseña"
            txtUsuario.setText(usuario.getNombreUsuario());
            txtUsuario.setDisable(true);
            txtNombre.setText(usuario.getNombreCompleto());
            chkActivo.setSelected(usuario.isActivo());
            panelPassword.setVisible(false);
            panelPassword.setManaged(false);

            for (RolDTO rol : cmbRol.getItems()) {
                if (rol.getId() == usuario.getRolId()) {
                    cmbRol.setValue(rol);
                }
            }
        }
    }

    public boolean isGuardado() {
        return guardado;
    }

    @FXML
    private void guardar() {
        if (cmbRol.getValue() == null) {
            mostrarError("Seleccione un rol.");
            return;
        }

        try {
            if (usuario == null) {
                if (!txtPassword.getText().equals(txtConfirmar.getText())) {
                    mostrarError("La confirmación no coincide con la contraseña.");
                    return;
                }
                UsuarioDTO nuevo = new UsuarioDTO();
                nuevo.setNombreUsuario(txtUsuario.getText().trim());
                nuevo.setNombreCompleto(txtNombre.getText().trim());
                nuevo.setRolId(cmbRol.getValue().getId());
                usuarioService.crear(nuevo, txtPassword.getText());
            } else {
                // objeto nuevo para no modificar el usuario de la tabla si el guardado falla
                UsuarioDTO editado = new UsuarioDTO();
                editado.setId(usuario.getId());
                editado.setNombreCompleto(txtNombre.getText().trim());
                editado.setRolId(cmbRol.getValue().getId());
                editado.setActivo(chkActivo.isSelected());
                usuarioService.actualizar(editado);
            }
            guardado = true;
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
