---
name: springboot4-mongodb
description: Mandatory guidelines for the data pipeline with Spring Boot 4.0.5, Java 24, and MongoDB in the /data domain of RehabiAPP.
author: alaslibres
tags: [spring-boot-4, java24, mongodb, csfle, aggregation, privacy]
---

# Skill: Spring Boot 4 + MongoDB — Data Pipeline

> **Scope:** `/data` (Agent 1)
> **Overrides:** Any older community skills for this domain

---

## Environment

- You MUST use **Spring Boot 4.0.5** and **Java 24** for all data pipeline logic.
- Use Spring Data MongoDB as the primary data access layer. Leverage Java 24 features (records, sealed interfaces, pattern matching) for clean domain modeling and pipeline stage construction.
- All MongoDB driver and Spring Data MongoDB versions must be compatible with Spring Boot 4.0.5's dependency management. Do not override managed versions without explicit justification.

## Security

- **Client-Side Field Level Encryption (CSFLE) is mandatory** for all patient data stored in MongoDB.
- CSFLE must be configured at the MongoDB driver level using the `mongocrypt` library so that sensitive fields (patient identifiers, clinical metrics, session health data) are encrypted before leaving the application and decrypted only upon retrieval by authorized services.
- Encryption keys must be managed through a Key Management Service (AWS KMS or a local master key for development). The data encryption key (DEK) and key encryption key (KEK) must never be stored in the same location.
- Define a JSON Schema for each collection that specifies which fields require encryption and the encryption algorithm:
  - `AEAD_AES_256_CBC_HMAC_SHA_512-Deterministic` for queryable fields.
  - `AEAD_AES_256_CBC_HMAC_SHA_512-Random` for non-queryable sensitive fields.

## Analytics

- **All statistical computations must use MongoDB Aggregation Pipelines** executed server-side. Do not load raw documents into application memory for processing.
- Design pipelines with efficient stage ordering: `$match` and `$project` stages first to reduce the working set before `$group`, `$sort`, or `$lookup` stages.
- For high-volume telemetry data (game session metrics), use `$bucket` or `$bucketAuto` for histogram generation and `$accumulator` for custom statistical functions.
- Create appropriate indexes to support aggregation pipeline performance (`$match` fields, `$sort` keys, `$lookup` foreign fields).
- Use `allowDiskUse(true)` only as a last resort for pipelines exceeding the 100 MB memory limit. Prefer pipeline optimization first.

## Privacy

- **Anonymized views or projections are required** whenever data is exposed for analytics, reporting, or external consumption.
- Implement MongoDB projections that explicitly exclude Personally Identifiable Information (PII): patient names, social security numbers, contact information, and any direct identifiers.
- For analytics endpoints, create dedicated DTOs that contain only anonymized or aggregated data. Never expose raw patient documents through any API response.
- When patient-level granularity is needed for analytics, replace direct identifiers with opaque pseudonymized tokens (one-way hash of patient ID + salt). The salt must be stored separately from the data and rotated periodically.
- Log access to non-anonymized patient data in the audit trail.
