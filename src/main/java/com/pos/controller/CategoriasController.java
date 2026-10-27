package com.pos.controller;

import com.pos.dto.CategoriaDTO;
import com.pos.service.NegocioException;
import com.pos.service.ProductoService;
import com.pos.util.Alertas;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.SQLException;

/**
 * Módulo de categorías: tabla a la izquierda y formulario a la derecha.
 * Al seleccionar una fila se cargan sus datos en el formulario para editarla.
 */
public class CategoriasController {

    @FXML private TableView<CategoriaDTO> tabla;
    @FXML private TableColumn<CategoriaDTO, String> colNombre;
    @FXML private TableColumn<CategoriaDTO, String> colDescripcion;
    @FXML private TableColumn<CategoriaDTO, String> colEstado;
    @FXML private Label lblFormulario;
    @FXML private TextField txtNombre;
    @FXML private TextArea txtDescripcion;
    @FXML private CheckBox chkActiva;
    @FXML private Label lblMensaje;

    private final ProductoService productoService = new ProductoService();
    private CategoriaDTO seleccionada; // null = se está creando una nueva

    @FXML
    private void initialize() {
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colDescripcion.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));

        // cuando cambia la fila seleccionada se cargan sus datos en el formulario
        tabla.getSelectionModel().selectedItemProperty().addListener((obs, anterior, nueva) -> {
            if (nueva != null) {
                cargarEnFormulario(nueva);
            }
        });

        cargarCategorias();
    }

    private void cargarCategorias() {
        try {
            tabla.getItems().setAll(productoService.listarTodasLasCategorias());
        } catch (NegocioException e) {
            Alertas.error(e.getMessage());
        } catch (SQLException e) {
            Alertas.error("Error al cargar las categorías: " + e.getMessage());
        }
    }

    @FXML
    private void nueva() {
        seleccionada = null;
        tabla.getSelectionModel().clearSelection();
        lblFormulario.setText("Nueva categoría");
        txtNombre.clear();
        txtDescripcion.clear();
        chkActiva.setSelected(true);
        ocultarMensaje();
        txtNombre.requestFocus();
    }

    @FXML
    private void guardar() {
        CategoriaDTO categoria = new CategoriaDTO();
        if (seleccionada != null) {
            categoria.setId(seleccionada.getId());
        }
        categoria.setNombre(txtNombre.getText());
        categoria.setDescripcion(txtDescripcion.getText());
        categoria.setActivo(chkActiva.isSelected());

        try {
            productoService.guardarCategoria(categoria);
            cargarCategorias();
            nueva();
            mostrarMensaje("Categoría guardada.", true);
        } catch (NegocioException e) {
            mostrarMensaje(e.getMessage(), false);
        } catch (SQLException e) {
            mostrarMensaje("Error de base de datos: " + e.getMessage(), false);
        }
    }

    private void cargarEnFormulario(CategoriaDTO categoria) {
        seleccionada = categoria;
        lblFormulario.setText("Editar categoría");
        txtNombre.setText(categoria.getNombre());
        txtDescripcion.setText(categoria.getDescripcion());
        chkActiva.setSelected(categoria.isActivo());
        ocultarMensaje();
    }

    private void mostrarMensaje(String texto, boolean exito) {
        lblMensaje.setText(texto);
        lblMensaje.getStyleClass().setAll("label", exito ? "mensaje-exito" : "mensaje-error");
        lblMensaje.setVisible(true);
        lblMensaje.setManaged(true);
    }

    private void ocultarMensaje() {
        lblMensaje.setVisible(false);
        lblMensaje.setManaged(false);
    }
}
