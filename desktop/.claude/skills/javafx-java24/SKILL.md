---
name: javafx-java24
description: Mandatory guidelines for desktop development with Java 24 and JavaFX 23 in the /desktop domain of RehabiAPP.
author: alaslibres
tags: [java24, javafx23, desktop, jasperreports, mvc]
---

# Skill: JavaFX + Java 24 — Desktop Development

> **Scope:** `/desktop` (Agent 3)
> **Overrides:** Any older community skills for this domain

---

## Environment

- You MUST write code exclusively for **Java 24** and **JavaFX 23**.
- The project uses **JasperReports 7.0.1** for report generation. The strict module encapsulation of the Java module system requires `--add-opens` JVM flags for reflection-based libraries (JasperReports, ControlsFX, CalendarFX, etc.).
- The `module-info.java` file and Gradle JVM arguments must account for all necessary opens directives. Never disable the module system to silence warnings.

## Architecture

- Enforce a **strict MVC pattern** with complete separation of concerns:
  - **`.fxml` files**: define the view layout (SceneBuilder-compatible). No logic, only bindings and event handler references.
  - **Controllers**: handle UI events, bind data to views, and delegate all business logic to services. Never access the database directly.
  - **Services**: encapsulate business logic and orchestrate DAO calls. They are the only layer that coordinates transactions and enforces business rules.
  - **DAOs**: handle all database access via `PreparedStatement`. No raw SQL in controllers or services.
- Each layer resides in its own package (`controller`, `service`, `dao`, `model`).

## Thread Management

- **Never block the JavaFX Application Thread.** Any I/O operation (database queries, file operations, API calls, report generation) MUST run on a background thread.
- Use `javafx.concurrent.Task<T>` or `javafx.concurrent.Service<T>` to manage background work with lifecycle callbacks (`setOnSucceeded`, `setOnFailed`).
- All UI updates triggered from background threads MUST be wrapped in `Platform.runLater()`.
- Display loading indicators while tasks are in progress. Disable interactive controls to prevent duplicate submissions.

## Styling

- Use **JavaFX-specific CSS** with `-fx-` prefixed properties exclusively. Do not use standard CSS properties not recognized by the JavaFX CSS engine.
- Maintain dual-theme support (light and dark) via separate `.css` stylesheets loaded at runtime.
- Style classes defined in CSS must match `styleClass` attributes in FXML files. Avoid inline styles in Java code except for dynamic, data-driven visual changes.
