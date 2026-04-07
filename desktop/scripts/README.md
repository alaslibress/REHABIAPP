# Scripts de desarrollo — desktop SGE

## reseed-dev.sql

Borra todos los sanitarios y pacientes actuales y crea un seed minimo
para desarrollo local. **NO USAR EN PRODUCCION.**

### Aplicar

```bash
docker exec -i rehabiapp-db psql -U admin -d rehabiapp \
  < /home/alaslibres/DAM/RehabiAPP/desktop/scripts/reseed-dev.sql
```

### Credenciales generadas

| DNI | Contrasena | Rol | Tipo |
|-----|------------|-----|------|
| admin0000 | admin | SPECIALIST | Sanitario administrador |
| 00000001R | medico1234 | SPECIALIST | Sanitario medico ejemplo (Carlos Garcia Lopez) |
| 00000002W | enfermero1234 | NURSE | Sanitario enfermero ejemplo (Lucia Martinez Ruiz) |
| 00000003A | n/a (no hace login) | n/a | Paciente ejemplo (Pedro Sanchez Gomez) |

Las contrasenas se almacenan como hash BCrypt cost 12.
Las contrasenas en plano de esta tabla son SOLO para desarrollo local.

### Notas tecnicas

- El paciente tiene los campos clinicos cifrados (alergias, antecedentes,
  medicacion_actual) en NULL. Para rellenarlos, usar el API:
  `POST /api/pacientes` o `PUT /api/pacientes/{dni}` desde la UI.
- Si se necesita volver a correr el script, es idempotente en localidad/CP
  pero borra y recrea sanitarios y pacientes. Ejecutar con cuidado.
