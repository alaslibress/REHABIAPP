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
TESTING:
=====================================================
- Se realizó un proceso de refactorización mediante IA de bajo riesgo (no hace cambios en el código, únicamente arregla problemas de sintaxis o me notifica de bugs y problemas para que yo haga solución de errores.
- Se han realizado pruebas de testing funcionales ascendentes en todo momento y posterior a la implementación de algo nuevo al programa.
- Posterior a estas pruebas se ha implementado un proceso de testing de sistema también manual (SIN IA).
- Una vez realizado todos estos procesos de testing se realizó un proceso de testing automatizado con test de JUnit automatizados con IA (Claude)
- Todo el proceso y resultado del testing se documentó en un documento .md que generó la misma IA dando el resultado final.
- Posteriormente se realizó una prueba manual de funcionalidades, para comprobar que la IA no ha cambiado nada sin consentimiento.
- Se hizo una lista de sugerencias a tener en cuenta para la mejora de la UX, se implementó un sistema de caché para mejor compatibilidad con AWS
- Se implementó un sistema de paginas en las tablas para evitar saturar el programa en caso de tener muchos usuarios.
- Se realizó testing manual y con IA a las nuevas funcionalidades.
- Se realizó una prueba ALFA realizada por una persona ajena al programa