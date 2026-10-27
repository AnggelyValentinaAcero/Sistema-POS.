package com.pos.app;

/**
 * Clase con el método main del .jar ejecutable.
 * JavaFX no permite que la clase principal del .jar herede de Application,
 * por eso esta clase solo llama a PosApplication.
 */
public class Launcher {

    public static void main(String[] args) {
        PosApplication.main(args);
    }
}
