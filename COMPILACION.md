# Cómo compilar el proyecto

Esto es lo que hago yo para compilar y correr el POS en Windows. No hace falta instalar Maven
porque el proyecto ya trae el wrapper (`mvnw.cmd`), que lo descarga solo la primera vez.

## Lo que necesitas

- JDK 21 o más nuevo (yo uso el 25). Para ver si lo tienes: `java -version`
- MySQL 8 con la base ya creada (eso está en `INSTALACION.md`)

## Pasos

1. Abrir una terminal (PowerShell o CMD) en la carpeta del proyecto.

2. Compilar:

   ```
   .\mvnw.cmd clean package
   ```

   La primera vez se demora porque baja las librerías. Si todo sale bien al final dice
   `BUILD SUCCESS` y queda el archivo `target\sistema-pos-0.1.0-SNAPSHOT.jar`.

3. Ejecutar. Se puede de dos formas:

   ```
   .\mvnw.cmd javafx:run
   ```

   o con el jar que se generó:

   ```
   java -jar target\sistema-pos-0.1.0-SNAPSHOT.jar
   ```

La primera vez que abre pide los datos de conexión a MySQL. Después se entra con `admin` / `admin`
y te obliga a cambiar la contraseña.

## Generar el .exe (opcional)

```
scripts\crear_ejecutable.bat
```

Queda en `dist\SistemaPOS\SistemaPOS.exe`. Esa carpeta trae su propio Java, así que se puede
copiar a otro PC sin instalar nada (solo hace falta MySQL). Si dice que no encuentra `jpackage`,
hay que configurar `JAVA_HOME` apuntando a la carpeta del JDK.

## Problemas que me pasaron

- **"JAVA_HOME not found"**: no está configurado `JAVA_HOME`. Se agrega en las variables de
  entorno de Windows con la ruta del JDK (por ejemplo `C:\Program Files\Java\jdk-25`) y se
  vuelve a abrir la terminal.
- **"JavaFX runtime components are missing"**: pasa si se ejecuta la clase `PosApplication`
  directo desde el IDE. Hay que ejecutar `Launcher` o usar los comandos de arriba.
- **Sale un aviso de "Unsupported JavaFX configuration"**: es normal, no afecta en nada.
- **Para correr solo las pruebas**: `.\mvnw.cmd test`
