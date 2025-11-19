Este es el proyecto de DI nombrado RehabiAPP

Este docuemtno TXT es el que aclara el funcionamiento de la Base de datos y la interfaz.

La base de datos es PostgresSQL, tenemos un archivo .properties que gestiona el acceso a la base de datos y la conexión con JDBC
El fichero exportado es un script SQL que incluye indices para cuando realicemos busquedas en el buscador de la interfaz y comentarios
en las tablas creadas.

Aunque en el script se visualicen diferentes tablas, son tablas creadas con el objetivo de evitar redundancia
de datos creadas en la 3FN de la normalización. En total el número de tablas reales son 2 más la tabla de la relación N:M (Citas)

Interfaz:

Esta es la interfaz desarrollada en JAVAFX usando como IDE IntellijIDEA, todas las ventanas se han creado con SceneBuilder.
