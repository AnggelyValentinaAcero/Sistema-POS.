# Sistema-POS (Point of Sale).
##Presentación del proyecto

### Descripción general

El Sistema POS (Point of Sale) es una aplicación orientada a la automatización, centralización y auditoría de las operaciones comerciales de pequeños y medianos negocios, como minimarkets, ferreterías y farmacias.

El proyecto surge como respuesta a problemas asociados a la gestión manual o semi-manual de las ventas y el inventario, tales como errores en el cálculo de cobros, descontrol del stock y dificultades para realizar seguimiento a las operaciones.

El sistema busca centralizar la información de las ventas y el inventario en una base de datos, permitiendo mantener información confiable y facilitar la toma de decisiones basada en datos.

---

## Objetivo general

Desarrollar un sistema POS que permita automatizar y centralizar las operaciones de venta e inventario, garantizando la integridad de los datos y proporcionando información útil para el control y análisis del negocio.

---

##  Objetivos específicos

- Agilizar el proceso de registro y cobro de las ventas.
- Reducir errores humanos en el cálculo de precios y cambio.
- Actualizar automáticamente el inventario al registrar una venta.
- Mantener un registro confiable de las transacciones realizadas.
- Facilitar la consulta de información relacionada con las ventas.
- Proporcionar métricas e información que apoyen la toma de decisiones.
- Mantener la integridad y consistencia de la información almacenada.

---

##  Problemas que busca solucionar

El sistema está orientado a solucionar principalmente:

- Errores de cálculo durante el cobro.
- Descontrol del inventario.
- Dificultad para rastrear las operaciones.
- Manipulación o inconsistencias en la información.
- Falta de información para analizar el comportamiento de las ventas.

---

##  Funcionalidades principales

Entre las funcionalidades propuestas para el sistema se encuentran:

- Registro de productos.
- Gestión y consulta del inventario.
- Registro de ventas.
- Gestión del detalle de las ventas.
- Actualización automática del inventario.
- Registro del operador responsable de la transacción.
- Consulta de información histórica de ventas.
- Generación de métricas para apoyar la toma de decisiones.

---

##  Base de datos

La aplicación utilizará **MySQL** como sistema de gestión de base de datos.

La base de datos será diseñada aplicando reglas de normalización hasta tercera forma normal (3NF), con el objetivo de reducir redundancias y mantener la integridad referencial.

El sistema utilizará una estructura Maestro-Detalle para las transacciones de venta:

- **Maestro:** almacena información general de la venta.
- **Detalle:** almacena cada uno de los artículos incluidos en la venta.

El precio unitario histórico será almacenado en el detalle de la venta para conservar el valor utilizado en el momento de la transacción.

---

##  Arquitectura del sistema

El proyecto utilizará una arquitectura multicapa.

La solución contempla:

- **Capa de presentación**, basada en el patrón MVC.
- **Capa de aplicación**, desarrollada en Java.
- **Capa de persistencia**, utilizando DAO y DTO.
- **Capa de datos**, implementada con MySQL.

La comunicación entre Java y MySQL se realizará mediante JDBC.

---

##  Tecnologías

- **Lenguaje:** Java
- **Base de datos:** MySQL
- **Conectividad:** JDBC
- **Patrón de presentación:** MVC
- **Persistencia:** DAO
- **Transferencia de datos:** DTO
- **Control de versiones:** Git y GitHub

---

##  Seguridad e integridad de datos

El sistema tendrá como prioridad la integridad y consistencia de las transacciones.

Para el acceso a la base de datos se utilizarán consultas parametrizadas mediante `PreparedStatement`, con el objetivo de evitar la concatenación directa de consultas SQL.

Las operaciones relacionadas con una venta deberán ejecutarse como una única transacción, garantizando las propiedades ACID.

---

##  Control de transacciones

El registro de una venta contempla como una operación conjunta:

1. Inserción de la información de la venta.
2. Inserción de las líneas de detalle.
3. Actualización del inventario.

Estas operaciones deberán completarse correctamente para que los cambios sean almacenados.

En caso de producirse un error durante el proceso, la transacción deberá revertirse para evitar información inconsistente.

---
