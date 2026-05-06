# CLAUDE.md - RehabiAPP Mobile Frontend

> **File:** `/mobile/frontend/CLAUDE.md`
> **Agent:** Agent 2 (Mobile Developer)
> **Role:** Thinker (Opus) + Doer (Sonnet)
> **Scope:** Aplicacion movil del paciente (React Native + Expo).

---

## 1. PROJECT DEFINITION

Este directorio contiene la aplicacion movil orientada al paciente de RehabiAPP. Es la interfaz que el paciente usa para consultar su perfil clinico, sus tratamientos filtrados por nivel de progresion, su historial de juegos terapeuticos y sus citas, asi como para interactuar con el chatbot de IA cuando este disponible.

La interfaz debe ser altamente accesible para todos los rangos de edad, incluidos pacientes mayores con movilidad o vision reducida.

---

## 2. OPERATING RULES

1. **Contexto global:** leer y respetar `/CLAUDE.md` y `/mobile/CLAUDE.md` antes de cualquier decision que afecte al dominio movil.

2. **Aislamiento estricto:** el frontend NUNCA llama directamente al API central (`/api`). Todo el trafico sale hacia `/mobile/backend` (BFF) mediante GraphQL sobre HTTPS. La regla esta reforzada por NetworkPolicy en Kubernetes.

3. **Sin acceso directo a base de datos:** el frontend no toca PostgreSQL ni MongoDB. Cualquier persistencia se delega al BFF, que a su vez delega en el API Java.

4. **Plan de implementacion:** la planificacion detallada de fases vive en `/mobile/frontend/PLAN.md`. Cada Doer debe leerlo antes de tocar codigo.

5. **Estilo:** literales en castellano sin diacriticos en strings nuevos, sin emojis, sin reformateos masivos. Codigo profesional y legible al nivel de un ingeniero junior de datos.

6. **Accesibilidad primero:** objetivos tactiles minimos de 48x48dp, contraste WCAG AA, tipografia legible y navegacion simple.

---

## 3. LOCAL STACK

- React Native + Expo (SDK 54), TypeScript 5.9.
- Apollo Client para GraphQL contra el BFF.
- Zustand como store global (`authStore`, `userStore`, `appointmentsStore`, `bootstrapStore`, `errorStore`).
- Expo Router (file-based) con dos grupos de rutas: `(auth)` y `(tabs)` (7 pestanas).
- NativeWind para estilos basados en Tailwind con soporte light/dark.
- Expo SecureStore para persistir el token JWT del BFF.

```
npx expo start            # Servidor de desarrollo
npx expo start --android  # Lanzar en Android
npx expo start --ios      # Lanzar en iOS
npm test                  # Tests
```

---

## 4. COMMUNICATION CONTRACT

```
Mobile App (este modulo)
    |
    | GraphQL sobre HTTPS (Apollo Client -> Apollo Server)
    | Authorization: Bearer <JWT BFF>
    | X-Timezone para el saludo contextual
    v
BFF Node.js (`/mobile/backend`)
    |
    | REST sobre HTTPS (Bearer <JWT Java>)
    v
API Java (`/api`) -> PostgreSQL / MongoDB (via `/data`)
```

El frontend solo conoce el endpoint del BFF. Los detalles del API central son opacos.

---

## 5. NOTAS DE SPRINT

### Sprint Progreso (2026-04-27)

Sin trabajo de frontend en este sprint. El PDF de tratamiento (`treatmentDocument` query) y los datos de progreso clinico pasan a estar disponibles cuando `/api` Phase 8 entre en produccion. El frontend ya integra el endpoint `GET /api/pacientes/{dni}/tratamientos/{cod}/documento` a traves del BFF (`/mobile/backend`); no se requiere ningun cambio adicional hasta que el backend lo entregue.

---

*Este fichero es la fuente de verdad para el dominio del frontend movil. Para la planificacion operativa por fases, consultar `/mobile/frontend/PLAN.md`.*

## Memory

You have access to Engram persistent memory via MCP tools (mem_save, mem_search, mem_session_summary, etc.).
- Save proactively after significant work — don't wait to be asked.
- After any compaction or context reset, call `mem_context` to recover session state before continuing.
