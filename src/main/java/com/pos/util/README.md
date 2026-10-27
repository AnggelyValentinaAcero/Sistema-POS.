# Paquete `util`: utilidades compartidas

Clases pequeñas que usan varias capas: la sesión del usuario, los permisos, las contraseñas, los
mensajes en pantalla y el manejo de textos.

## Archivos

| Archivo | Qué hace | Lo usan |
|---|---|---|
| `Sesion.java` | Guarda quién inició sesión en esta caja, sus permisos y el nombre de la terminal. | Services y controllers |
| `Permiso.java` | `enum` con los 13 permisos del sistema; los mismos códigos que la tabla `permisos`. | Services (verificar) y `PrincipalController` (menú) |
| `Claves.java` | Contraseñas con BCrypt: generar el hash, verificar, validar la longitud y generar contraseñas temporales. | `AuthService`, `UsuarioService` |
| `Alertas.java` | Atajos para mostrar ventanas de información, error y confirmación. | Controllers |
| `Textos.java` | Formato de montos (`$ 1,234.50`) y limpieza y validación de textos ingresados. | Services y controllers |

## Partes clave del código

### `Sesion`: el usuario conectado

```java
public class Sesion {
    private static UsuarioDTO usuario;
    private static List<Permiso> permisos = new ArrayList<>();
    private static String terminal;

    public static boolean tienePermiso(Permiso permiso) {
        return permisos.contains(permiso);
    }
    ...
}
```

- Los atributos son `static` porque en una caja hay **una sola sesión abierta a la vez**:
  cualquier clase puede preguntar `Sesion.getUsuario()` sin pasarse el objeto de mano en mano.
- `AuthService.iniciarSesion()` la llena al hacer login y `cerrarSesion()` la vacía.

### `Permiso` + `Sesion`: el control de acceso

Cada service empieza verificando el permiso:

```java
if (!Sesion.tienePermiso(Permiso.USUARIOS_GESTIONAR)) {
    throw new NegocioException("No tiene permiso para administrar usuarios.");
}
```

Hay un **doble control**: el menú oculta los módulos sin permiso (`PrincipalController`), y aunque
alguien llegara a una pantalla, el service no lo deja operar.

### `Claves`: contraseñas seguras

```java
public static String generarHash(String clave) {
    return BCrypt.hashpw(clave, BCrypt.gensalt());   // gensalt() genera una "sal" aleatoria
}

public static boolean verificar(String clave, String hash) {
    if (clave == null || hash == null) {
        return false;
    }
    return BCrypt.checkpw(clave, hash);
}
```

- Las contraseñas **nunca** se guardan en texto: se guarda su hash BCrypt
  (ej: `$2a$10$uh.2XloKyTzK...`), que no se puede revertir.
- Para validar el login, `checkpw` vuelve a calcular el hash de lo que escribió el usuario y lo
  compara.
- `generarTemporal()` usa `SecureRandom` y letras sin caracteres que se confunden (0/O, 1/l/I),
  para las contraseñas de "Restablecer contraseña".

### `Textos.limpiar()`: textos vacíos → `null`

```java
public static String limpiar(String texto) {
    if (texto == null) {
        return null;
    }
    texto = texto.trim();
    if (texto.isEmpty()) {
        return null;
    }
    return texto;
}
```

El documento del cliente es `UNIQUE` en MySQL. Si se guardara `""`, el segundo cliente sin
documento chocaría con el primero. `NULL` no cuenta para `UNIQUE`, por eso los textos vacíos se
convierten a `null` antes de guardar.

### `Textos.moneda()`

```java
DecimalFormat formato = new DecimalFormat("#,##0.00");
return simbolo + " " + formato.format(valor);   // 1234.5 → "$ 1,234.50"
```

El símbolo se carga de la configuración del negocio al iniciar sesión (`Textos.setSimbolo`).
