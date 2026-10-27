package com.pos.app;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Region;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Métodos de ayuda para cargar las vistas FXML y abrir ventanas.
 */
public class Ventanas {

    private static final String ESTILOS = "/css/estilos.css";

    private static Stage ventanaPrincipal;

    public static void setVentanaPrincipal(Stage stage) {
        ventanaPrincipal = stage;
    }

    /**
     * Carga un archivo FXML. Después de cargarlo se puede obtener su controlador
     * con loader.getController().
     */
    public static FXMLLoader cargar(String rutaFxml) {
        FXMLLoader loader = new FXMLLoader(Ventanas.class.getResource(rutaFxml));
        try {
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException("No se pudo cargar la vista " + rutaFxml, e);
        }
        return loader;
    }

    /** Cambia el contenido de la ventana principal por otra vista. */
    public static FXMLLoader cambiarVista(String rutaFxml) {
        FXMLLoader loader = cargar(rutaFxml);
        Parent raiz = loader.getRoot();
        if (ventanaPrincipal.getScene() == null) {
            ventanaPrincipal.setScene(crearEscena(raiz));
        } else {
            ventanaPrincipal.getScene().setRoot(raiz);
        }
        return loader;
    }

    /**
     * Muestra una vista ya cargada como ventana modal: bloquea la ventana principal
     * hasta que se cierre. Su tamaño se adapta dinámicamente al contenido y permite
     * ser redimensionada manualmente por el usuario si lo requiere.
     */
    public static void mostrarModal(FXMLLoader loader, String titulo) {
        Stage dialogo = new Stage();
        dialogo.setTitle(titulo);
        dialogo.initOwner(ventanaPrincipal);
        dialogo.initModality(Modality.WINDOW_MODAL);
        dialogo.setResizable(true);

        Parent raiz = loader.getRoot();
        Scene escena = crearEscena(raiz);
        dialogo.setScene(escena);

        // Si el contenido crece (por ejemplo, al mostrar un mensaje de error),
        // la ventana adapta su altura para que no se corte nada.
        if (raiz instanceof Region) {
            Region region = (Region) raiz;
            region.heightProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && oldVal != null && newVal.doubleValue() > oldVal.doubleValue()) {
                    dialogo.sizeToScene();
                }
            });
        }

        // Asegura que la ventana no se reduzca por debajo de su tamaño inicial
        dialogo.setOnShown(e -> {
            dialogo.setMinWidth(dialogo.getWidth());
            dialogo.setMinHeight(dialogo.getHeight());
        });

        dialogo.showAndWait();
    }

    /**
     * Ajusta el tamaño de la ventana al contenido actual de su escena.
     * Útil al mostrar u ocultar mensajes dinámicos de error.
     */
    public static void ajustarTamano(Node nodo) {
        if (nodo != null && nodo.getScene() != null && nodo.getScene().getWindow() instanceof Stage) {
            Stage stage = (Stage) nodo.getScene().getWindow();
            stage.sizeToScene();
        }
    }

    /**
     * Sincroniza un PasswordField con un TextField visible y asocia el botón
     * de ojo para alternar entre ver y ocultar la contraseña.
     */
    public static void configurarVisibilidadPassword(PasswordField pass, TextField visible, Button boton) {
        visible.textProperty().bindBidirectional(pass.textProperty());
        boton.setTooltip(new Tooltip("Mostrar contraseña"));
        boton.setOnAction(e -> {
            boolean mostrar = !visible.isVisible();
            visible.setVisible(mostrar);
            visible.setManaged(mostrar);
            pass.setVisible(!mostrar);
            pass.setManaged(!mostrar);
            boton.setText(mostrar ? "🙈" : "👁");
            boton.setTooltip(new Tooltip(mostrar ? "Ocultar contraseña" : "Mostrar contraseña"));
            if (mostrar) {
                visible.requestFocus();
                visible.positionCaret(visible.getText().length());
            } else {
                pass.requestFocus();
                pass.positionCaret(pass.getText().length());
            }
        });
    }

    /** Cierra la ventana en la que está el nodo (botón, campo, etc). */
    public static void cerrar(Node nodo) {
        Stage stage = (Stage) nodo.getScene().getWindow();
        stage.close();
    }

    private static Scene crearEscena(Parent raiz) {
        Scene escena = new Scene(raiz);
        escena.getStylesheets().add(Ventanas.class.getResource(ESTILOS).toExternalForm());
        return escena;
    }
}
