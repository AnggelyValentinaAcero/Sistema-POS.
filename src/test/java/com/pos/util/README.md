# Pruebas: `util`

Pruebas unitarias con **JUnit 5** (sin base de datos). Se ejecutan con `.\mvnw.cmd test`.

## Archivos

| Archivo | Qué prueba |
|---|---|
| `ClavesTest.java` | El manejo de contraseñas de `Claves`: hash BCrypt, verificación, reglas mínimas y contraseñas temporales. |

## Pruebas

| Prueba | Qué comprueba |
|---|---|
| `hashVerificaSoloLaClaveCorrecta` | El hash no es la contraseña en texto, y solo la contraseña correcta lo verifica. |
| `sinHashNuncaVerifica` | Si no hay hash (usuario inexistente), la verificación da `false` sin error. |
| `validaLongitudYNombreUsuario` | Rechaza contraseñas de menos de 6 caracteres o iguales al nombre de usuario. |
| `temporalTieneOchoCaracteresSinAmbiguos` | La contraseña temporal tiene 8 caracteres y ninguno de `0 O 1 l I`. |

## Parte clave del código

```java
@Test
void hashVerificaSoloLaClaveCorrecta() {
    String hash = Claves.generarHash("secreto1");
    assertNotEquals("secreto1", hash);                   // nunca se guarda en texto plano
    assertTrue(Claves.verificar("secreto1", hash));      // la correcta verifica
    assertFalse(Claves.verificar("secreto2", hash));     // otra no
}

@Test
void temporalTieneOchoCaracteresSinAmbiguos() {
    String temporal = Claves.generarTemporal();
    assertEquals(8, temporal.length());
    assertTrue(temporal.chars().noneMatch(ch -> "0O1lI".indexOf(ch) >= 0));
}
```

Esta prueba tarda un poco más que las demás (~0.2 s) porque BCrypt es **lento a propósito**: así
un atacante que robara la base de datos tardaría muchísimo en probar contraseñas.
