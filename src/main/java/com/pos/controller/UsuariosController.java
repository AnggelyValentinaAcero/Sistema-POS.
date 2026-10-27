package com.pos.controller;

import com.pos.app.Ventanas;
import com.pos.dto.UsuarioDTO;
import com.pos.service.NegocioException;
import com.pos.service.UsuarioService;
import com.pos.util.Alertas;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;

import java.sql.SQLException;

/**
 * Controlador del módulo de administración de usuarios.
 */
public class UsuariosController {

    @FXML private TableView<UsuarioDTO> tabla;
    @FXML private TableColumn<UsuarioDTO, String> colUsuario;
    @FXML private TableColumn<UsuarioDTO, String> colNombre;
    @FXML private TableColumn<UsuarioDTO, String> colRol;
    @FXML private TableColumn<UsuarioDTO, String> colEstado;
    @FXML private TableColumn<UsuarioDTO, String> colUltimoAcceso;

    private final UsuarioService usuarioService = new UsuarioService();

    @FXML
    private void initialize() {
        // PropertyValueFactory busca el getter con ese nombre en UsuarioDTO
        // (por ejemplo "nombreUsuario" -> getNombreUsuario())
        colUsuario.setCellValueFactory(new PropertyValueFactory<>("nombreUsuario"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombreCompleto"));
        colRol.setCellValueFactory(new PropertyValueFactory<>("rol"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        colUltimoAcceso.setCellValueFactory(new PropertyValueFactory<>("ultimoAccesoTexto"));
        cargarUsuarios();
    }

    @FXML
    private void cargarUsuarios() {
        try {
            tabla.getItems().setAll(usuarioService.listar());
        } catch (NegocioException e) {
            Alertas.error(e.getMessage());
        } catch (SQLException e) {
            Alertas.error("Error al cargar los usuarios: " + e.getMessage());
        }
    }

    @FXML
    private void nuevo() {
        abrirFormulario(null);
    }

    @FXML
    private void editar() {
        UsuarioDTO seleccionado = tabla.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            Alertas.error("Seleccione un usuario de la tabla.");
            return;
        }
        abrirFormulario(seleccionado);
    }

    // doble clic sobre una fila = editar
    @FXML
    private void clicTabla(MouseEvent evento) {
        if (evento.getClickCount() == 2 && tabla.getSelectionModel().getSelectedItem() != null) {
            editar();
        }
    }

    @FXML
    private void restablecerPassword() {
        UsuarioDTO seleccionado = tabla.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            Alertas.error("Seleccione un usuario de la tabla.");
            return;
        }
        if (!Alertas.confirmar("¿Generar una contraseña temporal para '" + seleccionado.getNombreUsuario() + "'?")) {
            return;
        }
        try {
            String temporal = usuarioService.restablecerPassword(seleccionado.getId());
            Alertas.info("Contraseña temporal", "La contraseña temporal de '" + seleccionado.getNombreUsuario()
                    + "' es:\n\n" + temporal + "\n\nEl usuario deberá cambiarla al ingresar.");
        } catch (NegocioException e) {
            Alertas.error(e.getMessage());
        } catch (SQLException e) {
            Alertas.error("Error al restablecer la contraseña: " + e.getMessage());
        }
    }

    private void abrirFormulario(UsuarioDTO usuario) {
        FXMLLoader loader = Ventanas.cargar("/fxml/usuario_form.fxml");
        UsuarioFormController formulario = loader.getController();
        formulario.setUsuario(usuario);

        String titulo = (usuario == null) ? "Nuevo usuario" : "Editar usuario";
        Ventanas.mostrarModal(loader, titulo);

        if (formulario.isGuardado()) {
            cargarUsuarios();
        }
    }
}
