Este es el proyecto de DI nombrado RehabiAPP

Este documento TXT es el que aclara el funcionamiento de la Base de datos y la interfaz.

La base de datos es PostgresSQL, tenemos un archivo .properties que gestiona el acceso a la base de datos y la conexión con JDBC
El fichero exportado es un script SQL que incluye indices para cuando realicemos busquedas en el buscador de la interfaz y comentarios
en las tablas creadas.

Aunque en el script se visualicen diferentes tablas, son tablas creadas con el objetivo de evitar redundancia
de datos creadas en la 3FN de la normalización. En total el número de tablas reales son 2 más la tabla de la relación N:M (Citas)

Interfaz:

Esta es la interfaz desarrollada en JAVAFX usando como IDE IntellijIDEA, todas las ventanas se han creado con SceneBuilder.

=====================================================
NOTAS IMPORTANTES:
=====================================================
--
Credenciales por defecto:
Usuario: ADMIN0000
Contraseña: admin
--
Se recomienda cambiar la contraseña despues del primer inicio de sesion
--
Los cargos disponibles son:
- Especialista: Acceso completo (CRUD pacientes, sanitarios y citas)
- Enfermero: Solo lectura + gestion de citas
--
Datos como enfermero para inicio de sesión: (en caso de no exportarse bien el usuario crear uno desde el usuario administrador)
- DNI: 77380273V
- contraseña: admin
=====================================================
