# AWS EKS Kubernetes Manifests - Zero Trust Rules (ENS Alto)

> **Scope:** `/infra` domain and all Kubernetes manifests across the ecosystem
> **Compliance:** ENS Alto, RGPD, AWS Well-Architected Framework (Security Pillar)
> **Authority:** These rules are MANDATORY for all agents writing or reviewing Kubernetes manifests, Helm charts, or infrastructure-as-code for the RehabiAPP production cluster. Violations are treated as critical security incidents.

---

## Network Policies (Zero Trust)

### MANDATORY Rules

1. **Default Deny:** Every namespace MUST have a default-deny NetworkPolicy for BOTH ingress and egress. No pod communicates with any other pod, service, or external endpoint unless an explicit NetworkPolicy allows it. This is the foundational Zero Trust principle.

   ```yaml
   # Required in every namespace
   apiVersion: networking.k8s.io/v1
   kind: NetworkPolicy
   metadata:
     name: default-deny-all
   spec:
     podSelector: {}
     policyTypes:
       - Ingress
       - Egress
   ```

2. **Per-Service Network Policies:** Each microservice MUST have its own dedicated NetworkPolicy allowing ONLY the specific ports and source/destination pods it requires. Policies must reference pods by label selectors, not by IP address.

3. **Explicit External Egress:** Egress to external services (AWS RDS, S3, external APIs) MUST be explicitly allowed per-service with CIDR blocks or FQDN-based policies (using Calico or Cilium network policy extensions). No blanket external access.

4. **Namespace Isolation:** Separate namespaces are MANDATORY for: `rehabiapp-api`, `rehabiapp-mobile`, `rehabiapp-data`, `rehabiapp-games`, `rehabiapp-monitoring`. No co-mingling of services across namespaces. Cross-namespace communication requires explicit NetworkPolicies referencing the `namespaceSelector`.

### FORBIDDEN Practices

1. **Wildcard Allow Rules:** NetworkPolicies with empty `podSelector` (match-all) in allow rules are FORBIDDEN. Every allow rule must target specific pod labels. A match-all allow rule defeats the purpose of Zero Trust.

---

## Pod Security

### MANDATORY Rules

1. **Non-Root Execution:** All pods MUST run as non-root. Set `runAsNonRoot: true` and `runAsUser` to a UID >= 1000 in the pod or container security context.

2. **Read-Only Root Filesystem:** All containers MUST use `readOnlyRootFilesystem: true`. Writable paths required by the application (temp files, caches) must use explicit `emptyDir` or `tmpfs` volume mounts with size limits.

3. **Capability Dropping:** Drop ALL Linux capabilities by default and add back only what is strictly needed. The baseline security context for every container:
   ```yaml
   securityContext:
     capabilities:
       drop: ["ALL"]
       # add: [] -- only if strictly required, with documented justification
   ```

4. **Resource Limits:** CPU and memory resource limits are MANDATORY on every container. No unbounded resource consumption. Both `requests` and `limits` must be set. Limits must be based on load testing data, not arbitrary values.

5. **Health Probes:** Liveness and readiness probes are MANDATORY on every container. Each service must expose a health check endpoint. Liveness probes detect deadlocked processes; readiness probes prevent traffic routing to unready pods. Startup probes should be used for slow-starting applications to avoid premature liveness failures.

### FORBIDDEN Practices

1. **Privileged Containers:** `privileged: true` is FORBIDDEN under any circumstance in production. No exception. No justification is sufficient.

2. **Host Namespace Access:** `hostNetwork`, `hostPID`, and `hostIPC` are FORBIDDEN in any production pod spec. These break container isolation and expose the host kernel to container workloads.

---

## Storage and Encryption

### MANDATORY Rules

1. **EBS Encryption:** All EBS volumes backing PersistentVolumes MUST use `encrypted: true` with AWS KMS customer-managed keys (CMK). Default AWS-managed keys do not meet ENS Alto key management requirements. The CMK must have a rotation policy.

2. **StorageClass Configuration:** The StorageClass used for production volumes MUST specify `encrypted: "true"` and `kmsKeyId` parameters pointing to the customer-managed CMK.

3. **EFS Encryption:** If EFS volumes are used, encryption at rest AND in transit MUST be enabled. EFS mount targets must use the `tls` mount option.

4. **Secrets Management:** Secrets MUST be managed via AWS Secrets Manager with the CSI Secrets Store Driver (secrets-store.csi.k8s.io). Secrets are mounted as volumes, not injected as environment variables.

### FORBIDDEN Practices

1. **emptyDir for Persistent Data:** `emptyDir` volumes for any data that must survive pod restarts are FORBIDDEN. Use PersistentVolumeClaims with appropriate StorageClass.

2. **Kubernetes Native Secrets for Credentials:** Kubernetes native Secrets (stored in etcd) are FORBIDDEN for sensitive credentials (database passwords, API keys, encryption keys). Kubernetes Secrets are base64-encoded, not encrypted, and are insufficient for ENS Alto compliance. Use AWS Secrets Manager exclusively.

---

## RBAC and Service Accounts

### MANDATORY Rules

1. **Dedicated Service Accounts:** Each microservice MUST have its own Kubernetes ServiceAccount. No sharing of ServiceAccounts between deployments. This enables fine-grained audit trails and least-privilege access.

2. **IRSA for AWS Access:** IAM Roles for Service Accounts (IRSA) is MANDATORY for all AWS API access from within the cluster. The ServiceAccount is annotated with the IAM role ARN, and pods assume the role via OIDC federation. No AWS access keys are stored in the cluster.

3. **PodDisruptionBudgets:** PodDisruptionBudgets (PDBs) are MANDATORY for all production deployments with `minAvailable >= 1`. This ensures service availability during node maintenance, cluster upgrades, and voluntary disruptions.

### FORBIDDEN Practices

1. **ClusterRole Bindings for Applications:** ClusterRole bindings for application workloads are FORBIDDEN. Use namespace-scoped RoleBindings only. Application pods must not have cluster-wide permissions. ClusterRoles are reserved for infrastructure operators and monitoring systems.

---

## Observability

### MANDATORY Rules

1. **Structured JSON Logs:** All pods MUST export structured JSON logs to stdout. No file-based logging inside containers. Log aggregation systems (FluentBit, CloudWatch) consume stdout/stderr. JSON structure must include at minimum: timestamp, level, service name, correlation ID, and message.

2. **Prometheus Metrics:** Every service MUST expose a Prometheus metrics endpoint at `/metrics`. Metrics must include at minimum: request count, request latency histogram, error rate, and active connections. Custom business metrics (e.g., appointments processed, telemetry sessions ingested) are encouraged.

3. **Distributed Tracing:** OpenTelemetry sidecar or SDK integration is MANDATORY for distributed tracing across all services. Trace context must propagate across HTTP boundaries using W3C Trace Context headers. This enables end-to-end request tracing from mobile frontend through BFF, API, and data pipeline.

---

## Compliance

### MANDATORY Rules

1. **Private Container Registry:** All container images MUST come from a private Amazon ECR registry. Public Docker Hub images are FORBIDDEN in production. Base images must be pulled from Docker Hub into ECR and scanned before use.

2. **Immutable Image Tags:** Image tags in deployment manifests MUST be immutable digests (`sha256:...`), not mutable tags like `latest`, `stable`, or version tags that can be overwritten. Digest-based references guarantee reproducible deployments and prevent supply chain attacks via tag mutation.

3. **Pod Security Standards:** Pod Security Standards MUST be enforced at namespace level via the PodSecurity admission controller with level set to `restricted`. This is the most restrictive built-in policy level and aligns with ENS Alto requirements.

4. **Mutual TLS (mTLS):** All inter-service communication within the cluster MUST use mTLS. Implement via service mesh (Istio or Linkerd) or native Cilium encryption. Unencrypted pod-to-pod traffic is FORBIDDEN in production, even within the same namespace. mTLS provides both encryption and mutual authentication between services.
