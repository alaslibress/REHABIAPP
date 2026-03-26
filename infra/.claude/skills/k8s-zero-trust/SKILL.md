# Kubernetes YAML Manifest Rules for AWS EKS (ENS Alto -- Zero Trust)

This skill defines mandatory rules for all Kubernetes manifests deployed to AWS EKS in the RehabiAPP infrastructure. These rules enforce ENS Alto (Spain's National Security Framework - High Level), RGPD data residency requirements, and Zero Trust network architecture. All manifests must pass these rules before being applied to any cluster. Violations of MANDATORY rules are blocking -- manifests must not be applied until resolved.

---

## 1. Zero Trust Network Policies

- MANDATORY: Every namespace MUST include a default-deny NetworkPolicy for BOTH ingress AND egress as the first applied manifest. No pod communicates unless explicitly allowed:
  ```yaml
  # default-deny-all.yaml — Aplicar en CADA namespace
  apiVersion: networking.k8s.io/v1
  kind: NetworkPolicy
  metadata:
    name: default-deny-all
    namespace: rehabiapp-api  # Repetir para cada namespace
  spec:
    podSelector: {}  # Aplica a todos los pods del namespace
    policyTypes:
      - Ingress
      - Egress
  ```

- MANDATORY: After the default-deny policy, each microservice MUST have its own dedicated allow-list NetworkPolicy specifying ONLY the exact traffic it needs. Example for the API service:
  ```yaml
  # allow-api-traffic.yaml
  apiVersion: networking.k8s.io/v1
  kind: NetworkPolicy
  metadata:
    name: allow-api-ingress
    namespace: rehabiapp-api
  spec:
    podSelector:
      matchLabels:
        app: rehabiapp-api
        tier: backend
    policyTypes:
      - Ingress
      - Egress
    ingress:
      # Permitir trafico SOLO desde el ingress controller
      - from:
          - namespaceSelector:
              matchLabels:
                name: ingress-nginx
            podSelector:
              matchLabels:
                app: ingress-nginx
        ports:
          - protocol: TCP
            port: 8080
      # Permitir trafico desde el mobile backend (BFF)
      - from:
          - namespaceSelector:
              matchLabels:
                name: rehabiapp-mobile
            podSelector:
              matchLabels:
                app: mobile-bff
        ports:
          - protocol: TCP
            port: 8080
    egress:
      # Permitir conexion a PostgreSQL (AWS RDS)
      - to:
          - ipBlock:
              cidr: 10.0.100.0/24  # Subnet privada de RDS
        ports:
          - protocol: TCP
            port: 5432
      # Permitir DNS (obligatorio para resolucion de servicios)
      - to:
          - namespaceSelector: {}
            podSelector:
              matchLabels:
                k8s-app: kube-dns
        ports:
          - protocol: UDP
            port: 53
          - protocol: TCP
            port: 53
  ```

- MANDATORY: The allowed traffic topology for RehabiAPP is strictly:
  ```
  Internet -> Ingress Controller -> rehabiapp-api (port 8080)
  Internet -> Ingress Controller -> rehabiapp-mobile/mobile-bff (port 3000)
  mobile-bff -> rehabiapp-api (port 8080)  [BFF pattern]
  rehabiapp-api -> AWS RDS PostgreSQL (port 5432)
  rehabiapp-api -> rehabiapp-data (port 3001)
  rehabiapp-data -> MongoDB Atlas / DocumentDB (port 27017)
  rehabiapp-games (external) -> Ingress -> rehabiapp-api (port 8080)
  ALL pods -> kube-dns (port 53 UDP/TCP)
  ```
  Any traffic path NOT listed above is FORBIDDEN and must not have a NetworkPolicy allowing it.

- FORBIDDEN: NetworkPolicies with empty selectors in allow rules that effectively grant blanket access:
  ```yaml
  # PROHIBIDO — permite trafico desde cualquier pod en cualquier namespace
  ingress:
    - from:
        - namespaceSelector: {}

  # PROHIBIDO — permite todo el trafico de salida
  egress:
    - to: []
  ```

- MANDATORY: Separate namespaces for each domain with explicit labels:
  - `rehabiapp-api` (label: `name: rehabiapp-api`)
  - `rehabiapp-mobile` (label: `name: rehabiapp-mobile`)
  - `rehabiapp-data` (label: `name: rehabiapp-data`)
  - `rehabiapp-monitoring` (label: `name: rehabiapp-monitoring`)
  - `ingress-nginx` (label: `name: ingress-nginx`)

- MANDATORY: For egress to AWS managed services (RDS, S3, Secrets Manager), use CIDR-based rules targeting the VPC private subnets. If using Calico or Cilium, prefer FQDN-based policies for AWS endpoints.

- MANDATORY: All inter-service communication within the cluster MUST use mTLS. Implement via service mesh (Istio, Linkerd) or Cilium native encryption. Unencrypted pod-to-pod traffic is FORBIDDEN in production.

---

## 2. Secrets Management

- MANDATORY: Kubernetes native `Secret` objects with plaintext base64-encoded data are FORBIDDEN for any sensitive credential in production. Base64 is encoding, NOT encryption -- anyone with RBAC read access to Secrets sees plaintext.

- MANDATORY: Use the **AWS Secrets Manager CSI Driver** (`secrets-store-csi-driver` with AWS provider) to mount secrets directly from AWS Secrets Manager into pods as volumes:
  ```yaml
  # SecretProviderClass — mapea secretos de AWS Secrets Manager
  apiVersion: secrets-store.csi.x-k8s.io/v1
  kind: SecretProviderClass
  metadata:
    name: rehabiapp-api-secrets
    namespace: rehabiapp-api
  spec:
    provider: aws
    parameters:
      objects: |
        - objectName: "rehabiapp/api/db-credentials"
          objectType: "secretsmanager"
          objectAlias: "db-credentials"
        - objectName: "rehabiapp/api/encryption-key"
          objectType: "secretsmanager"
          objectAlias: "encryption-key"
        - objectName: "rehabiapp/api/jwt-signing-key"
          objectType: "secretsmanager"
          objectAlias: "jwt-signing-key"
  ```

  ```yaml
  # Deployment usando el CSI Driver para montar secretos
  apiVersion: apps/v1
  kind: Deployment
  metadata:
    name: rehabiapp-api
    namespace: rehabiapp-api
  spec:
    template:
      spec:
        serviceAccountName: rehabiapp-api-sa  # Con IRSA configurado
        containers:
          - name: api
            image: 123456789.dkr.ecr.eu-west-1.amazonaws.com/rehabiapp-api@sha256:abc123...
            volumeMounts:
              - name: secrets-store
                mountPath: "/mnt/secrets"
                readOnly: true
        volumes:
          - name: secrets-store
            csi:
              driver: secrets-store.csi.k8s.io
              readOnly: true
              volumeAttributes:
                secretProviderClass: "rehabiapp-api-secrets"
  ```

- MANDATORY: Alternative approved method -- **Sealed Secrets** (Bitnami) for GitOps workflows. SealedSecret resources are encrypted with a cluster-specific key and safe to commit to Git:
  ```yaml
  # SealedSecret — cifrado asimetricamente, seguro para Git
  apiVersion: bitnami.com/v1alpha1
  kind: SealedSecret
  metadata:
    name: api-db-credentials
    namespace: rehabiapp-api
  spec:
    encryptedData:
      DB_HOST: AgBy8hC...encrypted...==
      DB_PORT: AgCx9kD...encrypted...==
      DB_USERNAME: AgDz0lE...encrypted...==
      DB_PASSWORD: AgEa1mF...encrypted...==
  ```

- FORBIDDEN: Any of these patterns in production manifests:
  ```yaml
  # PROHIBIDO — secreto en texto plano (base64 no es cifrado)
  apiVersion: v1
  kind: Secret
  metadata:
    name: db-credentials
  type: Opaque
  data:
    password: cGFzc3dvcmQxMjM=  # "password123" en base64

  # PROHIBIDO — credenciales en variables de entorno hardcodeadas
  env:
    - name: DB_PASSWORD
      value: "password123"

  # PROHIBIDO — credenciales en ConfigMaps
  apiVersion: v1
  kind: ConfigMap
  data:
    DB_PASSWORD: "password123"
  ```

- FORBIDDEN: Storing secrets in Helm `values.yaml` files committed to the repository, even if the deployment uses templating.

- MANDATORY: All secrets in AWS Secrets Manager must:
  - Use AWS KMS Customer Managed Keys (CMK) for encryption, not default AWS-managed keys.
  - Be in an EU region (`eu-west-1`, `eu-south-2`, or `eu-central-1`) for RGPD compliance.
  - Have automatic rotation enabled (maximum 90-day rotation period for database credentials).
  - Have resource policies restricting access to specific IAM roles (least privilege).

- MANDATORY: Pods access AWS Secrets Manager via **IRSA** (IAM Roles for Service Accounts). No AWS access keys (`AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY`) stored anywhere in the cluster.

---

## 3. Pod Security

- MANDATORY: Every container in every production pod MUST have a strict `securityContext`:
  ```yaml
  apiVersion: apps/v1
  kind: Deployment
  metadata:
    name: rehabiapp-api
    namespace: rehabiapp-api
  spec:
    template:
      spec:
        # Contexto de seguridad a nivel de Pod
        securityContext:
          runAsNonRoot: true
          runAsUser: 1000
          runAsGroup: 1000
          fsGroup: 1000
          seccompProfile:
            type: RuntimeDefault
        containers:
          - name: api
            image: 123456789.dkr.ecr.eu-west-1.amazonaws.com/rehabiapp-api@sha256:abc123...
            # Contexto de seguridad a nivel de Container
            securityContext:
              allowPrivilegeEscalation: false
              readOnlyRootFilesystem: true
              runAsNonRoot: true
              runAsUser: 1000
              capabilities:
                drop:
                  - ALL
            # Limites de recursos OBLIGATORIOS
            resources:
              requests:
                cpu: "250m"
                memory: "512Mi"
              limits:
                cpu: "1000m"
                memory: "1Gi"
            # Probes de salud OBLIGATORIAS
            livenessProbe:
              httpGet:
                path: /actuator/health/liveness
                port: 8080
              initialDelaySeconds: 30
              periodSeconds: 10
              failureThreshold: 3
            readinessProbe:
              httpGet:
                path: /actuator/health/readiness
                port: 8080
              initialDelaySeconds: 15
              periodSeconds: 5
              failureThreshold: 3
            # Volumenes temporales para escritura
            volumeMounts:
              - name: tmp
                mountPath: /tmp
              - name: app-cache
                mountPath: /app/cache
        volumes:
          - name: tmp
            emptyDir:
              medium: Memory
              sizeLimit: "100Mi"
          - name: app-cache
            emptyDir:
              sizeLimit: "200Mi"
  ```

- MANDATORY: The following `securityContext` fields are REQUIRED on every container:
  | Field | Required Value | Reason |
  |-------|---------------|--------|
  | `runAsNonRoot` | `true` | Previene ejecucion como root |
  | `runAsUser` | `>= 1000` | UID no privilegiado |
  | `readOnlyRootFilesystem` | `true` | Inmutabilidad del sistema de archivos |
  | `allowPrivilegeEscalation` | `false` | Bloquea escalada de privilegios |
  | `capabilities.drop` | `["ALL"]` | Elimina todas las capabilities de Linux |
  | `seccompProfile.type` | `RuntimeDefault` | Filtro de syscalls por defecto |

- FORBIDDEN: Any of these in production manifests:
  - `privileged: true`
  - `hostNetwork: true`
  - `hostPID: true`
  - `hostIPC: true`
  - `capabilities.add` with `SYS_ADMIN`, `NET_ADMIN`, `NET_RAW` (unless justified and approved by security team)
  - Missing `resources.requests` or `resources.limits`
  - Missing `livenessProbe` or `readinessProbe`

- MANDATORY: Resource limits guidelines for RehabiAPP services:
  | Service | CPU Request | CPU Limit | Memory Request | Memory Limit |
  |---------|-------------|-----------|----------------|--------------|
  | rehabiapp-api | 250m | 1000m | 512Mi | 1Gi |
  | mobile-bff | 100m | 500m | 256Mi | 512Mi |
  | rehabiapp-data | 200m | 1000m | 512Mi | 1Gi |
  | monitoring agents | 50m | 200m | 128Mi | 256Mi |
  These are baseline values. Adjust based on load testing, but NEVER remove limits entirely.

- MANDATORY: `PodDisruptionBudget` for all production deployments:
  ```yaml
  apiVersion: policy/v1
  kind: PodDisruptionBudget
  metadata:
    name: rehabiapp-api-pdb
    namespace: rehabiapp-api
  spec:
    minAvailable: 1
    selector:
      matchLabels:
        app: rehabiapp-api
  ```

- MANDATORY: All container images MUST:
  - Come from a private Amazon ECR registry. Public Docker Hub images are FORBIDDEN in production.
  - Use immutable image references with SHA256 digests, NOT mutable tags: `image: 123456789.dkr.ecr.eu-west-1.amazonaws.com/rehabiapp-api@sha256:a1b2c3...` -- NEVER `image: rehabiapp-api:latest`.
  - Be scanned for vulnerabilities via ECR image scanning before deployment. Images with CRITICAL or HIGH CVEs must not be deployed.

- MANDATORY: PodSecurity Standards enforced at namespace level:
  ```yaml
  apiVersion: v1
  kind: Namespace
  metadata:
    name: rehabiapp-api
    labels:
      name: rehabiapp-api
      pod-security.kubernetes.io/enforce: restricted
      pod-security.kubernetes.io/audit: restricted
      pod-security.kubernetes.io/warn: restricted
  ```

- MANDATORY: Each microservice has its own Kubernetes `ServiceAccount`. No sharing of ServiceAccounts between Deployments:
  ```yaml
  apiVersion: v1
  kind: ServiceAccount
  metadata:
    name: rehabiapp-api-sa
    namespace: rehabiapp-api
    annotations:
      # IRSA — vincula el ServiceAccount a un IAM Role de AWS
      eks.amazonaws.com/role-arn: arn:aws:iam::ACCOUNT:role/rehabiapp-api-role
  ```

- MANDATORY: Structured JSON logging to stdout on all containers. File-based logging inside containers is FORBIDDEN. Use a centralized log aggregator (CloudWatch, Fluentd/Fluent Bit -> OpenSearch).

- MANDATORY: Prometheus metrics endpoint (`/metrics` or `/actuator/prometheus`) exposed on every service for monitoring.
