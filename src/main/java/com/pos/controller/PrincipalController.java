package com.pos.controller;

import com.pos.app.Ventanas;
import com.pos.config.ConfiguracionLocal;
import com.pos.config.Conexion;
import com.pos.dto.UsuarioDTO;
import com.pos.service.ConfiguracionService;
import com.pos.util.Textos;
import com.pos.util.Permiso;
import com.pos.util.Sesion;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.sql.SQLException;

/**
 * Ventana principal. A la izquierda muestra un menú con los módulos que
 * el rol del usuario tiene permitidos; al centro se carga el módulo elegido.
 */
public class PrincipalController {

    @FXML private VBox menu;
    @FXML private StackPane contenido;
    @FXML private Label lblUsuario;
    @FXML private Label lblEstado;

    private Button botonSeleccionado;

    @FXML
    private void initialize() {
        UsuarioDTO usuario = Sesion.getUsuario();
        ConfiguracionLocal config = Conexion.getConfiguracion();
        lblUsuario.setText(usuario.getNombreCompleto() + " (" + usuario.getRol() + ")");
        lblEstado.setText("Base de datos: " + config.getBaseDatos() + " en " + config.getHost()
                + "   |   Terminal: " + Sesion.getTerminal());

        // símbolo de moneda configurado para el negocio
        try {
            Textos.setSimbolo(new ConfiguracionService().obtenerNegocio().getSimboloMoneda());
        } catch (SQLException e) {
            e.printStackTrace(); // si falla se usa "$"
        }

        // Cada botón solo se agrega si el usuario tiene el permiso correspondiente.
        // Los módulos que aún no existen (fxml = null) muestran en qué fase se harán.
        agregarOpcion("Punto de venta", Permiso.VENTAS_REALIZAR, "/fxml/punto_venta.fxml", null);
        agregarOpcion("Caja", Permiso.CAJA_OPERAR, "/fxml/caja.fxml", null);
        agregarOpcion("Historial de ventas", Permiso.VENTAS_HISTORIAL, "/fxml/historial_ventas.fxml", null);
        agregarOpcion("Productos", Permiso.PRODUCTOS_VER, "/fxml/productos.fxml", null);
        agregarOpcion("Categorías", Permiso.PRODUCTOS_GESTIONAR, "/fxml/categorias.fxml", null);
        agregarOpcion("Inventario", Permiso.INVENTARIO_GESTIONAR, "/fxml/inventario.fxml", null);
        agregarOpcion("Clientes", Permiso.CLIENTES_GESTIONAR, "/fxml/clientes.fxml", null);
        agregarOpcion("Proveedores", Permiso.PROVEEDORES_GESTIONAR, "/fxml/proveedores.fxml", null);
        agregarOpcion("Reportes", Permiso.REPORTES_VER, "/fxml/reportes.fxml", null);
        agregarOpcion("Usuarios", Permiso.USUARIOS_GESTIONAR, "/fxml/usuarios.fxml", null);
        agregarOpcion("Configuración", null, "/fxml/configuracion.fxml", null);
        agregarOpcion("Auditoría", Permiso.AUDITORIA_VER, "/fxml/auditoria.fxml", null);

        mostrarMensaje("Hola, " + usuario.getNombreCompleto(), "Seleccione un módulo en el menú de la izquierda.");
    }

    private void agregarOpcion(String titulo, Permiso permiso, String fxml, String fase) {
        if (permiso != null && !Sesion.tienePermiso(permiso)) {
            return;
        }
        Button boton = new Button(titulo);
        boton.getStyleClass().add("boton-menu");
        boton.setMaxWidth(Double.MAX_VALUE);
        boton.setOnAction(evento -> {
            marcarSeleccionado(boton);
            if (fxml != null) {
                FXMLLoader loader = Ventanas.cargar(fxml);
                Parent vista = loader.getRoot();
                contenido.getChildren().setAll(vista);
            } else {
                mostrarMensaje(titulo, "Este módulo se implementará en la " + fase + ".");
            }
        });
        menu.getChildren().add(boton);
    }

    private void marcarSeleccionado(Button boton) {
        if (botonSeleccionado != null) {
            botonSeleccionado.getStyleClass().remove("seleccionado");
        }
        boton.getStyleClass().add("seleccionado");
        botonSeleccionado = boton;
    }

    private void mostrarMensaje(String titulo, String texto) {
        Label lblTitulo = new Label(titulo);
        lblTitulo.getStyleClass().add("titulo");
        Label lblTexto = new Label(texto);
        lblTexto.getStyleClass().add("subtitulo");

        VBox caja = new VBox(8, lblTitulo, lblTexto);
        caja.setAlignment(Pos.CENTER);
        contenido.getChildren().setAll(caja);
    }
}
