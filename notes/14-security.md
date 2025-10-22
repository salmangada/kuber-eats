# Kubernetes Security

## Overview

Kubernetes security involves protecting clusters, applications, and data across multiple layers. Security should be implemented at every level of the stack.

**Security Layers:**
1. Container security
2. Pod security
3. Network security
4. API server security
5. Secrets management
6. RBAC (Role-Based Access Control)
7. Cluster infrastructure security

## Authentication and Authorization

### Authentication

How users and services prove their identity to the API server.

**Authentication Methods:**
- X.509 Client Certificates
- Bearer Tokens
- Service Account Tokens
- OpenID Connect (OIDC)
- Webhook Token Authentication

```bash
# View current context
kubectl config view

# Get current user
kubectl config current-context

# View authentication info
kubectl config view --minify
```

### Authorization

What authenticated users are allowed to do.

**Authorization Modes:**
- **RBAC** (Role-Based Access Control) - Recommended
- **ABAC** (Attribute-Based Access Control)
- **Node** (Special authorization for kubelet)
- **Webhook** (External authorization)

```bash
# Check authorization mode
kubectl cluster-info dump | grep authorization-mode
```

## RBAC (Role-Based Access Control)

### Core RBAC Components

1. **Role**: Permissions within a namespace
2. **ClusterRole**: Cluster-wide permissions
3. **RoleBinding**: Grant Role to users/groups in namespace
4. **ClusterRoleBinding**: Grant ClusterRole cluster-wide

### Creating Roles

```yaml
# Role: Namespace-scoped permissions
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: pod-reader
  namespace: default
rules:
- apiGroups: [""]
  resources: ["pods"]
  verbs: ["get", "list", "watch"]
- apiGroups: [""]
  resources: ["pods/log"]
  verbs: ["get"]
```

```yaml
# ClusterRole: Cluster-wide permissions
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: cluster-admin-custom
rules:
- apiGroups: ["*"]
  resources: ["*"]
  verbs: ["*"]
- nonResourceURLs: ["*"]
  verbs: ["*"]
```

### Role Bindings

```yaml
# RoleBinding: Bind role to user in namespace
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: pod-reader-binding
  namespace: default
subjects:
- kind: User
  name: jane
  apiGroup: rbac.authorization.k8s.io
- kind: ServiceAccount
  name: my-service-account
  namespace: default
roleRef:
  kind: Role
  name: pod-reader
  apiGroup: rbac.authorization.k8s.io
```

```yaml
# ClusterRoleBinding: Bind cluster role cluster-wide
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: cluster-reader-binding
subjects:
- kind: Group
  name: developers
  apiGroup: rbac.authorization.k8s.io
roleRef:
  kind: ClusterRole
  name: view
  apiGroup: rbac.authorization.k8s.io
```

### Common RBAC Verbs

```yaml
rules:
- apiGroups: [""]
  resources: ["pods"]
  verbs:
  - get       # Read single resource
  - list      # List all resources
  - watch     # Watch for changes
  - create    # Create new resource
  - update    # Update existing resource
  - patch     # Partially update resource
  - delete    # Delete resource
  - deletecollection  # Delete multiple resources
```

### RBAC Examples

#### Developer Role

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: developer
  namespace: development
rules:
# Full access to pods
- apiGroups: [""]
  resources: ["pods", "pods/log", "pods/exec"]
  verbs: ["*"]
# Full access to deployments
- apiGroups: ["apps"]
  resources: ["deployments", "replicasets"]
  verbs: ["*"]
# Read-only access to services
- apiGroups: [""]
  resources: ["services"]
  verbs: ["get", "list", "watch"]
# Access to configmaps and secrets
- apiGroups: [""]
  resources: ["configmaps", "secrets"]
  verbs: ["get", "list", "create", "update"]
```

#### Read-Only User

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: read-only
rules:
- apiGroups: [""]
  resources: ["pods", "services", "configmaps"]
  verbs: ["get", "list", "watch"]
- apiGroups: ["apps"]
  resources: ["deployments", "replicasets", "statefulsets"]
  verbs: ["get", "list", "watch"]
```

#### Namespace Admin

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: namespace-admin
  namespace: my-namespace
subjects:
- kind: User
  name: admin-user
  apiGroup: rbac.authorization.k8s.io
roleRef:
  kind: ClusterRole
  name: admin  # Built-in admin role
  apiGroup: rbac.authorization.k8s.io
```

### Testing RBAC

```bash
# Check if you can perform an action
kubectl auth can-i create pods
kubectl auth can-i delete deployments

# Check as another user
kubectl auth can-i create pods --as=jane
kubectl auth can-i delete pods --as=system:serviceaccount:default:my-sa

# Check in specific namespace
kubectl auth can-i create pods --namespace=production --as=jane

# List all permissions for current user
kubectl auth can-i --list

# List all permissions as another user
kubectl auth can-i --list --as=jane --namespace=development
```

### Managing RBAC

```bash
# Create role
kubectl create role pod-reader --verb=get,list,watch --resource=pods

# Create cluster role
kubectl create clusterrole node-reader --verb=get,list --resource=nodes

# Create role binding
kubectl create rolebinding pod-reader-binding --role=pod-reader --user=jane

# Create cluster role binding
kubectl create clusterrolebinding cluster-admin-binding --clusterrole=cluster-admin --user=admin

# Bind to service account
kubectl create rolebinding my-binding --role=pod-reader --serviceaccount=default:my-sa

# View roles
kubectl get roles
kubectl get clusterroles

# View bindings
kubectl get rolebindings
kubectl get clusterrolebindings

# Describe role
kubectl describe role pod-reader
kubectl describe clusterrole cluster-admin
```

## Pod Security

### Pod Security Standards (PSS)

Three policies for pod security:

1. **Privileged**: Unrestricted (no restrictions)
2. **Baseline**: Minimally restrictive (prevents known privilege escalations)
3. **Restricted**: Heavily restricted (hardened)

### Pod Security Admission

```yaml
# Apply to namespace
apiVersion: v1
kind: Namespace
metadata:
  name: production
  labels:
    pod-security.kubernetes.io/enforce: restricted
    pod-security.kubernetes.io/audit: restricted
    pod-security.kubernetes.io/warn: restricted
```

### Security Context

Control security settings at pod and container level.

#### Pod-Level Security Context

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: secure-pod
spec:
  securityContext:
    runAsUser: 1000          # Run as specific user
    runAsGroup: 3000         # Run as specific group
    fsGroup: 2000            # File system group
    runAsNonRoot: true       # Must not run as root
    seccompProfile:
      type: RuntimeDefault   # Seccomp profile
  containers:
  - name: app
    image: myapp:1.0
```

#### Container-Level Security Context

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: security-context-demo
spec:
  containers:
  - name: secure-container
    image: nginx
    securityContext:
      runAsUser: 1000
      runAsNonRoot: true
      readOnlyRootFilesystem: true
      allowPrivilegeEscalation: false
      capabilities:
        drop:
        - ALL
        add:
        - NET_BIND_SERVICE
    volumeMounts:
    - name: temp
      mountPath: /tmp
    - name: cache
      mountPath: /var/cache/nginx
  volumes:
  - name: temp
    emptyDir: {}
  - name: cache
    emptyDir: {}
```

### Capabilities

Linux capabilities for fine-grained permissions.

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: capabilities-demo
spec:
  containers:
  - name: app
    image: myapp:1.0
    securityContext:
      capabilities:
        # Drop all capabilities
        drop:
        - ALL
        # Add only required capabilities
        add:
        - NET_BIND_SERVICE  # Bind to ports < 1024
        - CHOWN             # Change file ownership
```

### Read-Only Root Filesystem

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: readonly-fs-pod
spec:
  containers:
  - name: app
    image: myapp:1.0
    securityContext:
      readOnlyRootFilesystem: true
    volumeMounts:
    # Mount writable volumes where needed
    - name: tmp
      mountPath: /tmp
    - name: logs
      mountPath: /var/log
  volumes:
  - name: tmp
    emptyDir: {}
  - name: logs
    emptyDir: {}
```

### Prevent Privilege Escalation

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: no-privilege-escalation
spec:
  containers:
  - name: app
    image: myapp:1.0
    securityContext:
      allowPrivilegeEscalation: false
      runAsNonRoot: true
      runAsUser: 1000
```

## Network Security

### Network Policies

Control traffic between pods.

#### Deny All Traffic

```yaml
# Deny all ingress
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: deny-all-ingress
  namespace: default
spec:
  podSelector: {}
  policyTypes:
  - Ingress
---
# Deny all egress
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: deny-all-egress
  namespace: default
spec:
  podSelector: {}
  policyTypes:
  - Egress
```

#### Allow Specific Traffic

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: allow-frontend-to-backend
  namespace: production
spec:
  podSelector:
    matchLabels:
      app: backend
  policyTypes:
  - Ingress
  - Egress
  ingress:
  # Allow from frontend
  - from:
    - podSelector:
        matchLabels:
          app: frontend
    ports:
    - protocol: TCP
      port: 8080
  egress:
  # Allow to database
  - to:
    - podSelector:
        matchLabels:
          app: database
    ports:
    - protocol: TCP
      port: 5432
  # Allow DNS
  - to:
    - namespaceSelector: {}
      podSelector:
        matchLabels:
          k8s-app: kube-dns
    ports:
    - protocol: UDP
      port: 53
```

### Secure Services

```yaml
# Internal service only
apiVersion: v1
kind: Service
metadata:
  name: internal-service
  annotations:
    # Cloud-specific: internal load balancer
    service.beta.kubernetes.io/aws-load-balancer-internal: "true"
spec:
  type: ClusterIP  # Not exposed externally
  selector:
    app: internal-app
  ports:
  - port: 80
    targetPort: 8080
```

## Secrets Management

### Creating Secure Secrets

```bash
# Create secret from literal (avoid in production)
kubectl create secret generic db-secret --from-literal=password=SecureP@ssw0rd

# Create from file
echo -n 'SecureP@ssw0rd' > password.txt
kubectl create secret generic db-secret --from-file=password=password.txt
rm password.txt  # Remove file immediately

# Create TLS secret
kubectl create secret tls tls-secret --cert=server.crt --key=server.key
```

### Using Secrets Securely

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: secure-app
spec:
  containers:
  - name: app
    image: myapp:1.0
    env:
    - name: DB_PASSWORD
      valueFrom:
        secretKeyRef:
          name: db-secret
          key: password
    volumeMounts:
    - name: tls-certs
      mountPath: /etc/tls
      readOnly: true  # Mount as read-only
  volumes:
  - name: tls-certs
    secret:
      secretName: tls-secret
      defaultMode: 0400  # Restrictive permissions
```

### Encrypt Secrets at Rest

```yaml
# EncryptionConfiguration
apiVersion: apiserver.config.k8s.io/v1
kind: EncryptionConfiguration
resources:
- resources:
  - secrets
  providers:
  - aescbc:
      keys:
      - name: key1
        secret: <base64-encoded-32-byte-key>
  - identity: {}  # Fallback to unencrypted
```

Apply to API server:
```bash
# Edit kube-apiserver manifest
# Add: --encryption-provider-config=/path/to/encryption-config.yaml
```

### External Secret Management

**Sealed Secrets:**

```bash
# Install sealed-secrets controller
kubectl apply -f https://github.com/bitnami-labs/sealed-secrets/releases/download/v0.24.0/controller.yaml

# Install kubeseal CLI
wget https://github.com/bitnami-labs/sealed-secrets/releases/download/v0.24.0/kubeseal-linux-amd64
sudo install -m 755 kubeseal-linux-amd64 /usr/local/bin/kubeseal

# Create sealed secret
kubectl create secret generic my-secret --dry-run=client --from-literal=password=secret -o yaml | \
  kubeseal -o yaml > sealed-secret.yaml

# Apply sealed secret
kubectl apply -f sealed-secret.yaml
```

## Image Security

### Use Trusted Images

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: trusted-image-pod
spec:
  containers:
  - name: app
    image: registry.company.com/myapp:v1.2.3  # Use specific version tag
    imagePullPolicy: Always  # Always pull to get updates
  imagePullSecrets:
  - name: registry-credentials
```

### Image Pull Secrets

```bash
# Create docker registry secret
kubectl create secret docker-registry regcred \
  --docker-server=registry.company.com \
  --docker-username=user \
  --docker-password=password \
  --docker-email=user@company.com
```

```yaml
# Use in pod
apiVersion: v1
kind: Pod
metadata:
  name: private-image-pod
spec:
  containers:
  - name: app
    image: registry.company.com/private/myapp:1.0
  imagePullSecrets:
  - name: regcred
```

### Image Vulnerability Scanning

```bash
# Use trivy to scan images
trivy image nginx:latest

# Integrate with CI/CD
# Fail build if high/critical vulnerabilities found
```

## Admission Controllers

Control what can be created in the cluster.

### Common Admission Controllers

```bash
# Enable admission controllers
kube-apiserver --enable-admission-plugins=NodeRestriction,PodSecurityPolicy,NamespaceLifecycle
```

**Key Admission Controllers:**
- **PodSecurityPolicy**: Deprecated, use Pod Security Admission
- **LimitRanger**: Enforce resource limits
- **ResourceQuota**: Enforce resource quotas
- **NodeRestriction**: Restrict kubelet permissions
- **AlwaysPullImages**: Force image pull
- **DenyEscalatingExec**: Prevent exec into privileged containers

### Validating Webhooks

```yaml
apiVersion: admissionregistration.k8s.io/v1
kind: ValidatingWebhookConfiguration
metadata:
  name: validation-webhook
webhooks:
- name: validate.example.com
  rules:
  - apiGroups: [""]
    apiVersions: ["v1"]
    operations: ["CREATE", "UPDATE"]
    resources: ["pods"]
  clientConfig:
    service:
      name: validation-service
      namespace: default
      path: "/validate"
  admissionReviewVersions: ["v1"]
  sideEffects: None
```

## Audit Logging

Track all API server requests.

### Audit Policy

```yaml
apiVersion: audit.k8s.io/v1
kind: Policy
rules:
# Log all requests at Metadata level
- level: Metadata
  omitStages:
  - RequestReceived

# Log pod changes at Request level
- level: Request
  resources:
  - group: ""
    resources: ["pods"]

# Log secret access at Metadata level
- level: Metadata
  resources:
  - group: ""
    resources: ["secrets"]

# Don't log read requests
- level: None
  verbs: ["get", "list", "watch"]
```

Enable audit logging:
```bash
# Add to kube-apiserver
--audit-policy-file=/etc/kubernetes/audit-policy.yaml
--audit-log-path=/var/log/kubernetes/audit.log
--audit-log-maxage=30
--audit-log-maxbackup=10
--audit-log-maxsize=100
```

## Security Best Practices

### 1. Cluster Configuration

```bash
# Disable anonymous auth
--anonymous-auth=false

# Enable RBAC
--authorization-mode=RBAC

# Secure etcd
--etcd-cafile=/path/to/ca.crt
--etcd-certfile=/path/to/client.crt
--etcd-keyfile=/path/to/client.key

# Enable audit logging
--audit-policy-file=/path/to/policy.yaml
```

### 2. Pod Security Checklist

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: secure-pod-template
spec:
  # Pod security context
  securityContext:
    runAsNonRoot: true
    runAsUser: 1000
    fsGroup: 2000
    seccompProfile:
      type: RuntimeDefault

  containers:
  - name: app
    image: myapp:1.2.3  # Specific version, not latest
    imagePullPolicy: Always

    # Container security context
    securityContext:
      allowPrivilegeEscalation: false
      readOnlyRootFilesystem: true
      runAsNonRoot: true
      runAsUser: 1000
      capabilities:
        drop:
        - ALL

    # Resource limits
    resources:
      requests:
        memory: "128Mi"
        cpu: "250m"
      limits:
        memory: "256Mi"
        cpu: "500m"

    # Health checks
    livenessProbe:
      httpGet:
        path: /health
        port: 8080
    readinessProbe:
      httpGet:
        path: /ready
        port: 8080

  # Use secrets for sensitive data
  imagePullSecrets:
  - name: regcred
```

### 3. Network Security

- Enable Network Policies
- Use ClusterIP for internal services
- Implement zero-trust networking
- Encrypt traffic with service mesh

### 4. Secrets Management

- Never commit secrets to Git
- Use external secret managers (Vault, AWS Secrets Manager)
- Enable encryption at rest
- Use sealed secrets or similar
- Rotate secrets regularly
- Use short-lived credentials

### 5. RBAC Principles

- Principle of least privilege
- Use namespace-scoped roles when possible
- Avoid cluster-admin except for admins
- Regular RBAC audits
- Service accounts per application

## Security Scanning and Compliance

### Scan for Vulnerabilities

```bash
# Scan cluster with kube-bench (CIS benchmark)
kubectl apply -f https://raw.githubusercontent.com/aquasecurity/kube-bench/main/job.yaml
kubectl logs -f job/kube-bench

# Scan with kube-hunter
kubectl apply -f https://raw.githubusercontent.com/aquasecurity/kube-hunter/main/job.yaml

# Scan images with trivy
trivy image nginx:latest

# Runtime security with Falco
# Detect abnormal behavior at runtime
```

### Policy Enforcement

**OPA Gatekeeper:**

```bash
# Install Gatekeeper
kubectl apply -f https://raw.githubusercontent.com/open-policy-agent/gatekeeper/release-3.14/deploy/gatekeeper.yaml
```

```yaml
# Constraint Template
apiVersion: templates.gatekeeper.sh/v1beta1
kind: ConstraintTemplate
metadata:
  name: k8srequiredlabels
spec:
  crd:
    spec:
      names:
        kind: K8sRequiredLabels
      validation:
        openAPIV3Schema:
          properties:
            labels:
              type: array
              items: string
  targets:
  - target: admission.k8s.gatekeeper.sh
    rego: |
      package k8srequiredlabels
      violation[{"msg": msg}] {
        provided := {label | input.review.object.metadata.labels[label]}
        required := {label | label := input.parameters.labels[_]}
        missing := required - provided
        count(missing) > 0
        msg := sprintf("Missing required labels: %v", [missing])
      }
---
# Constraint
apiVersion: constraints.gatekeeper.sh/v1beta1
kind: K8sRequiredLabels
metadata:
  name: require-env-label
spec:
  match:
    kinds:
    - apiGroups: [""]
      kinds: ["Pod"]
  parameters:
    labels: ["env", "team"]
```

## Quick Reference

```bash
# RBAC
kubectl create role pod-reader --verb=get,list --resource=pods
kubectl create rolebinding my-binding --role=pod-reader --user=jane
kubectl auth can-i create pods
kubectl auth can-i delete deployments --as=jane

# Secrets
kubectl create secret generic my-secret --from-literal=password=secret
kubectl get secret my-secret -o jsonpath='{.data.password}' | base64 -d

# Network Policies
kubectl get networkpolicies
kubectl describe networkpolicy deny-all

# Security Context
# Check pod security context
kubectl get pod my-pod -o jsonpath='{.spec.securityContext}'

# Audit
# View audit logs
tail -f /var/log/kubernetes/audit.log
```

## Next Steps

- Implement Pod Security Standards across namespaces
- Set up external secret management (Vault)
- Enable audit logging
- Implement network policies
- Regular security scanning
- Set up RBAC properly
- Enable encryption at rest
- Implement service mesh for mTLS
