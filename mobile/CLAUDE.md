# CLAUDE.md - RehabiAPP Mobile (Patient App)

> **File:** `/mobile/CLAUDE.md`
> **Agent:** Agent 2 (Mobile Developer)
> **Role:** Thinker (Opus) + Doer (Sonnet)

---

## 1. PROJECT DEFINITION

This directory contains the patient-facing mobile application for rehabilitation tracking and appointment management. Built with React Native and Expo for cross-platform deployment (Android and iOS).

Patients use this app to view their clinical profile, assigned treatments filtered by progression level, game session history, and to schedule appointments. The app also integrates an AI-powered WhatsApp chatbot for automated appointment booking.

The UI must be highly accessible for all age groups, including elderly patients with reduced mobility or vision.

---

## 2. OPERATING RULES

1. **Global context:** Read and respect the root `/CLAUDE.md` before any cross-domain decision. This local file takes precedence for mobile-specific decisions only.

2. **Skills are mandatory:** Before any architectural change or implementation, read and follow the manuals in `.claude/skills/` of this directory. Skills override default behavior.

3. **Maintain this file:** When you complete a task, change `[ ]` to `[x]`. Remove resolved items that no longer provide useful context.

4. **No direct database access:** This app NEVER connects to PostgreSQL or MongoDB directly. All data is fetched from and sent to the Spring Boot REST API (/api). No exceptions.

5. **Accessibility first:** Large touch targets (minimum 48x48dp), clear color contrast (WCAG AA minimum), readable font sizes, simple navigation. The interface must be usable by patients of all ages without training.

6. **State management:** Use Context API or Zustand. Avoid Redux unless strictly necessary for a specific complex flow.

---

## 3. LOCAL STACK

- React Native, Expo, TypeScript.
- Axios or Fetch (API communication).
- Context API or Zustand (state management).
- Expo Router or React Navigation (navigation).

### Build commands

```
npx expo start            # Start development server
npx expo start --android  # Start on Android
npx expo start --ios      # Start on iOS
npm test                  # Run tests
```

---

## 4. ARCHITECTURE

```
src/
    |-- components/     Reusable UI components (buttons, cards, inputs)
    |-- screens/        Screen components (Login, Dashboard, Profile, Appointments)
    |-- hooks/          Custom React hooks
    |-- services/       API service layer (all fetch/axios calls)
    |-- context/        Global state (auth, user session, theme)
    |-- utils/          Helpers, formatters, validators
    |-- types/          TypeScript type definitions
    |-- assets/         Images, fonts, icons
```

All API calls go through the services/ layer. Screens never call fetch/axios directly.

---

## 5. IMPLEMENTATION CHECKLIST

### Phase 1: Project setup

- [ ] Initialize Expo project with TypeScript.
- [ ] Define folder structure (components, screens, hooks, services, context, utils, types).
- [ ] Configure navigation (tab-based with bottom navigation).
- [ ] Set up theming (light/dark mode, accessible color palette).
- [ ] Configure Axios instance with base URL and JWT interceptor.

### Phase 2: Authentication and shell

- [ ] Login screen (DNI + password, connecting to /api JWT endpoint).
- [ ] Secure token storage (Expo SecureStore).
- [ ] Auto-logout on token expiration.
- [ ] Main navigation shell (Dashboard, Treatments, Appointments, Profile tabs).
- [ ] Pull-to-refresh and loading states across all screens.

### Phase 3: Patient dashboard

- [ ] Patient profile screen (personal data, clinical summary).
- [ ] Assigned disabilities with current progression level display.
- [ ] Treatment list filtered by matching progression level and visibility flag.
- [ ] Game session history with progress charts.
- [ ] Appointment list (upcoming and past).

### Phase 4: Advanced features

- [ ] Appointment booking screen (date/time picker, practitioner selection).
- [ ] AI WhatsApp chatbot integration for automated appointment booking.
- [ ] Push notifications for appointment reminders.
- [ ] Offline-first caching strategy for critical patient data.

---

*This file is the single source of truth for the mobile domain. Update it as tasks are completed.*

## Memory

You have access to Engram persistent memory via MCP tools (mem_save, mem_search, mem_session_summary, etc.).
- Save proactively after significant work — don't wait to be asked.
- After any compaction or context reset, call `mem_context` to recover session state before continuing.
