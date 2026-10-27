# Paquete `app`: arranque y ventanas

Es el punto de entrada de la aplicación. Aquí se decide qué pantalla se muestra primero y se
centraliza la carga de las vistas FXML.

```
Launcher.main()  →  PosApplication.start()  →  ¿hay conexión guardada y funciona?
                                                  ├─ No  → conexion.fxml
                                                  └─ Sí  → login.fxml
```

## Archivos

| Archivo | Qué hace |
|---|---|
| `Launcher.java` | Contiene el `main` del `.jar` y del `.exe`. Solo llama a `PosApplication.main()`. |
| `PosApplication.java` | Clase principal de JavaFX (`extends Application`). Al iniciar decide si mostrar la ventana de conexión o el login. |
| `Ventanas.java` | Métodos de ayuda para cargar archivos FXML, cambiar la vista de la ventana principal, abrir ventanas modales y cerrarlas. |

## Partes clave del código

### `Launcher`: ¿por qué existe?

```java
public class Launcher {
    public static void main(String[] args) {
        PosApplication.main(args);
    }
}
```

Cuando una aplicación JavaFX se empaqueta en un `.jar` con todas sus librerías, Java no permite
que la clase con `main` herede de `Application`. Si se ejecuta `PosApplication` directamente
aparece el error *"JavaFX runtime components are missing"*. `Launcher` es una clase normal que
solo hace de "puente".

### `PosApplication.start()`: qué ventana mostrar primero

```java
ConfiguracionLocal config = ConfiguracionLocal.cargar();
if (!config.isConexionGuardada()) {
    Ventanas.cambiarVista("/fxml/conexion.fxml");          // primera vez: pedir datos de MySQL
} else {
    try {
        Conexion.probar(config);                            // ¿MySQL responde?
        Conexion.setConfiguracion(config);
        Ventanas.cambiarVista("/fxml/login.fxml");
    } catch (SQLException e) {
        FXMLLoader loader = Ventanas.cambiarVista("/fxml/conexion.fxml");
        ConexionController controller = loader.getController();
        controller.mostrarError(Conexion.traducirError(e)); // explica qué falló
    }
}
```

### `Ventanas.cargar()`: cargar un FXML y obtener su controlador

```java
public static FXMLLoader cargar(String rutaFxml) {
    FXMLLoader loader = new FXMLLoader(Ventanas.class.getResource(rutaFxml));
    try {
        loader.load();
    } catch (IOException e) {
        throw new RuntimeException("No se pudo cargar la vista " + rutaFxml, e);
    }
    return loader;
}
```

Después de `load()` se pueden obtener la vista (`loader.getRoot()`) y su controlador
(`loader.getController()`). Así una pantalla le pasa datos a otra antes de mostrarla.

### Abrir un formulario modal y pasarle datos

Se usa en todos los formularios (usuarios, productos, clientes…):

```java
FXMLLoader loader = Ventanas.cargar("/fxml/usuario_form.fxml");
UsuarioFormController formulario = loader.getController();
formulario.setUsuario(usuario);                  // null = crear uno nuevo
Ventanas.mostrarModal(loader, "Editar usuario"); // espera hasta que se cierre
if (formulario.isGuardado()) {
    cargarUsuarios();                            // refresca la tabla
}
```

`mostrarModal` usa `Modality.WINDOW_MODAL` y `showAndWait()`: la ventana principal queda
bloqueada hasta cerrar el formulario, y el código continúa recién entonces.

### Otros métodos de `Ventanas`

| Método | Uso |
|---|---|
| `cambiarVista(ruta)` | Reemplaza todo el contenido de la ventana principal (login ↔ menú principal). |
| `ajustarTamano(nodo)` | Agranda la ventana cuando aparece un mensaje de error, para que no se corte. |
| `configurarVisibilidadPassword(...)` | Botón "ojo" para ver u ocultar una contraseña: une un `PasswordField` con un `TextField` mediante `bindBidirectional`. |
| `cerrar(nodo)` | Cierra la ventana donde está el botón (se usa en "Guardar" y "Cancelar" de los formularios). |
