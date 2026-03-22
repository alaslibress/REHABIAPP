---
name: rehabi-app-ecosystem-manager
description: Global guidelines for managing a complex healthcare monorepo focused on gamification, data engineering, and microservices.
author: alaslibress
tags: [monorepo, healthcare, data-engineering, microservices, ai-integration]
---

# Skill: RehabiAPP Ecosystem Manager

## Overview
This skill provides the foundational rules for operating within a multi-technology monorepo. It ensures that any AI agent respects strict architectural boundaries, security protocols, and data-driven development principles.

## Global Principles
1. **Zero-Crossing Boundaries:** Frontend clients (Desktop, Mobile, Unity) must never communicate with each other directly. All interactions must route through the central REST API.
2. **Data-Centric Design:** Every module must generate, process, or store data with Data Engineering and Data Analytics as the ultimate goal (e.g., structured telemetry, clean schemas).
3. **Security by Default:** Medical and personal data is highly sensitive. Enforce strict environment variable usage, `bcrypt` for secrets, and never hardcode credentials.
4. **Containerization Readiness:** All services must be developed with Docker/Kubernetes deployment in mind (AWS architecture).

## Agent Behavior
- Always check the local `CLAUDE.md` within a sub-directory before modifying code.
- Do not apply rules from one technology stack to another.
- Prioritize clean architecture, testability, and efficient resource usage.
