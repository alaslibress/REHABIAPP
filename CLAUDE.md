# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

RehabiAPP is a JavaFX-based medical rehabilitation management system (Sistema de Gestion de Expedientes) that manages patients (pacientes), health professionals (sanitarios), and appointments (citas).

## Build Commands

```bash
# Build the project
gradle build

# Run the application
gradle run

# Clean build artifacts
gradle clean
```

## Architecture

### Package Structure

- **com.javafx.Clases** - Core domain classes (Main, Paciente, Sanitario, Cita, SesionUsuario, ConexionBD, VentanaUtil)
- **com.javafx.Interface** - JavaFX controllers for each window/view
- **com.javafx.DAO** - Data Access Objects for database operations (PacienteDAO, SanitarioDAO, CitaDAO)
- **src/main/resources** - FXML files, CSS themes, and images

### Key Components

1. **Main.java** - Application entry point, loads SGEInicioSesion.fxml and applies CSS configuration
2. **SesionUsuario** - Singleton managing the logged-in user session and permissions
3. **ConexionBD** - Database connection handler for PostgreSQL
4. **controladorVentanaOpciones** - Centralized CSS theme management (light/dark mode + font size)
5. **VentanaUtil** - Utility class for showing informative and question dialogs

### Database

- PostgreSQL database with tables for sanitarios, pacientes, and citas
- Connection configuration in `config.properties`
- JDBC driver: postgresql:42.6.0

### Permission System

- **Especialistas** (Specialists) - Full CRUD access to patients, sanitarios, and appointments
- **Enfermeros** (Nurses) - Read-only access to patients, no access to sanitarios management

### CSS Theme System

The application supports two themes managed by controladorVentanaOpciones:
- **tema_claro.css** - Light theme (default)
- **tema_oscuro.css** - Dark theme

**CRITICAL**: When opening modal windows or dialogs, ALWAYS apply CSS using:
```java
Scene scene = new Scene(root);
controladorVentanaOpciones.aplicarConfiguracionAScene(scene);
```

This ensures the user's saved theme (dark/light) and font size preferences are applied to ALL windows, including popups.

### Window Opening Pattern

All modal windows follow this pattern:
```java
FXMLLoader loader = new FXMLLoader(getClass().getResource("/VentanaName.fxml"));
Parent root = loader.load();

// Get controller if needed
ControllerClass controlador = loader.getController();

// Create scene and APPLY CSS
Scene scene = new Scene(root);
controladorVentanaOpciones.aplicarConfiguracionAScene(scene);

// Show modal
Stage stage = new Stage();
stage.setTitle("Title");
stage.setScene(scene);
stage.initModality(Modality.APPLICATION_MODAL);
stage.setResizable(false);
stage.showAndWait();
```

### Filter Windows

Filter windows (VentanaFiltroPacientes, VentanaFiltroSanitarios) allow users to:
- Filter by multiple criteria (age, protesis, cargo, etc.)
- Sort results by different fields
- Apply filters or reset to defaults

The filter windows return a FiltrosPaciente or FiltrosSanitario object containing the selected criteria, which is then used to filter the TableView data using Java streams.

## Common Workflows

### Adding a New Window

1. Create FXML file in `src/main/resources/`
2. Create controller in `com.javafx.Interface` package
3. Use controladorVentanaOpciones.aplicarConfiguracionAScene() when opening the window
4. Follow the modal window pattern shown above

### Database Operations

1. Use appropriate DAO class (PacienteDAO, SanitarioDAO, CitaDAO)
2. DAOs handle connection management via ConexionBD
3. All operations return boolean for success/failure or List<T> for queries
4. Use VentanaUtil to show success/error messages to user

### Permission Checks

Check user permissions using SesionUsuario:
```java
SesionUsuario sesion = SesionUsuario.getInstancia();
if (sesion.esEspecialista()) {
    // Full access
} else {
    // Limited access
}
```