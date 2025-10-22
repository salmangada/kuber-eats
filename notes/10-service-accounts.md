# Service Accounts

## What is a Service Account?

A Service Account provides an identity for processes running in a Pod. It allows pods to authenticate with the Kubernetes API server and access cluster resources.

**Key Concepts:**
- Identity for pods (not for humans)
- Used for API authentication
- Associated with RBAC permissions
- Automatically mounted in pods
- Each namespace has a default service account

## Service Accounts vs User Accounts

| Service Account | User Account |
|----------------|--------------|
| For pods/applications | For humans |
| Namespaced | Cluster-wide |
| Managed by Kubernetes | Managed externally |
| Created via kubectl/YAML | Managed by admin |
| Used with RBAC | Used with RBAC |

## Default Service Account

Every namespace automatically gets a `default` service account:

```bash
# View default service account
kubectl get serviceaccount
kubectl get sa  # Short form

# Output:
# NAME      SECRETS   AGE
# default   0         10d

# Describe it
kubectl describe sa default
```

### How It Works

When a pod is created without specifying a service account:
1. The `default` service account is automatically assigned
2. A token is mounted at `/var/run/secrets/kubernetes.io/serviceaccount/`
3. Pod can use this token to communicate with API server

## Creating Service Accounts

### Imperative Method

```bash
# Create service account
kubectl create serviceaccount my-sa
kubectl create sa app-sa  # Using short form

# Create in specific namespace
kubectl create sa my-sa -n dev

# Generate YAML
kubectl create sa my-sa --dry-run=client -o yaml > serviceaccount.yaml
```

### Declarative Method

```yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: my-service-account
  namespace: default
  labels:
    app: myapp
```

```bash
kubectl apply -f serviceaccount.yaml
```

## Using Service Accounts in Pods

### Assign Service Account to Pod

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: my-pod
spec:
  serviceAccountName: my-service-account
  containers:
  - name: app
    image: myapp:1.0
```

### Assign to Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: my-deployment
spec:
  replicas: 3
  selector:
    matchLabels:
      app: myapp
  template:
    metadata:
      labels:
        app: myapp
    spec:
      serviceAccountName: my-service-account
      containers:
      - name: app
        image: myapp:1.0
```

### Disable Automatic Token Mounting

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: my-pod
spec:
  serviceAccountName: my-service-account
  automountServiceAccountToken: false  # Disable token mounting
  containers:
  - name: app
    image: myapp:1.0
```

## Service Account Tokens

### Token Location in Pod

```bash
# Inside a pod, token is mounted at:
# /var/run/secrets/kubernetes.io/serviceaccount/

# View token
kubectl exec my-pod -- cat /var/run/secrets/kubernetes.io/serviceaccount/token

# View CA certificate
kubectl exec my-pod -- cat /var/run/secrets/kubernetes.io/serviceaccount/ca.crt

# View namespace
kubectl exec my-pod -- cat /var/run/secrets/kubernetes.io/serviceaccount/namespace
```

### Using Token to Access API

```bash
# From inside pod
TOKEN=$(cat /var/run/secrets/kubernetes.io/serviceaccount/token)
APISERVER=https://kubernetes.default.svc
CACERT=/var/run/secrets/kubernetes.io/serviceaccount/ca.crt

# Make API call
curl --cacert $CACERT --header "Authorization: Bearer $TOKEN" $APISERVER/api
```

## RBAC with Service Accounts

Service accounts need permissions to access cluster resources. Use RBAC to grant permissions.

### Role and RoleBinding (Namespace-scoped)

```yaml
# Role: Define permissions
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: pod-reader
  namespace: default
rules:
- apiGroups: [""]
  resources: ["pods"]
  verbs: ["get", "list", "watch"]
---
# RoleBinding: Bind role to service account
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: pod-reader-binding
  namespace: default
subjects:
- kind: ServiceAccount
  name: my-service-account
  namespace: default
roleRef:
  kind: Role
  name: pod-reader
  apiGroup: rbac.authorization.k8s.io
```

### ClusterRole and ClusterRoleBinding (Cluster-scoped)

```yaml
# ClusterRole: Cluster-wide permissions
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: node-reader
rules:
- apiGroups: [""]
  resources: ["nodes"]
  verbs: ["get", "list"]
---
# ClusterRoleBinding: Bind to service account
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: node-reader-binding
subjects:
- kind: ServiceAccount
  name: my-service-account
  namespace: default
roleRef:
  kind: ClusterRole
  name: node-reader
  apiGroup: rbac.authorization.k8s.io
```

### Creating with kubectl

```bash
# Create Role
kubectl create role pod-reader --verb=get,list,watch --resource=pods

# Create RoleBinding
kubectl create rolebinding pod-reader-binding --role=pod-reader --serviceaccount=default:my-service-account

# Create ClusterRole
kubectl create clusterrole node-reader --verb=get,list --resource=nodes

# Create ClusterRoleBinding
kubectl create clusterrolebinding node-reader-binding --clusterrole=node-reader --serviceaccount=default:my-service-account
```

## Complete Example: Pod with ServiceAccount and RBAC

```yaml
# 1. Create Service Account
apiVersion: v1
kind: ServiceAccount
metadata:
  name: app-sa
  namespace: default
---
# 2. Create Role (permissions to read pods and services)
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: app-role
  namespace: default
rules:
- apiGroups: [""]
  resources: ["pods", "services"]
  verbs: ["get", "list", "watch"]
- apiGroups: ["apps"]
  resources: ["deployments"]
  verbs: ["get", "list"]
---
# 3. Bind Role to ServiceAccount
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: app-role-binding
  namespace: default
subjects:
- kind: ServiceAccount
  name: app-sa
  namespace: default
roleRef:
  kind: Role
  name: app-role
  apiGroup: rbac.authorization.k8s.io
---
# 4. Create Pod using ServiceAccount
apiVersion: v1
kind: Pod
metadata:
  name: app-pod
  namespace: default
spec:
  serviceAccountName: app-sa
  containers:
  - name: app
    image: myapp:1.0
    command: ["sh", "-c", "sleep 3600"]
```

```bash
# Apply configuration
kubectl apply -f app-with-sa.yaml

# Verify pod is using the service account
kubectl describe pod app-pod | grep "Service Account"

# Test API access from inside pod
kubectl exec -it app-pod -- sh

# Inside pod, list pods (should work)
TOKEN=$(cat /var/run/secrets/kubernetes.io/serviceaccount/token)
curl -k -H "Authorization: Bearer $TOKEN" https://kubernetes.default/api/v1/namespaces/default/pods

# Try to create pod (should fail - no permission)
# This will return 403 Forbidden
```

## Managing Service Accounts

### Viewing Service Accounts

```bash
# List service accounts
kubectl get serviceaccounts
kubectl get sa

# List in all namespaces
kubectl get sa -A

# Describe service account
kubectl describe sa my-service-account

# Get YAML
kubectl get sa my-service-account -o yaml

# Show labels
kubectl get sa --show-labels
```

### Deleting Service Accounts

```bash
# Delete service account
kubectl delete sa my-service-account

# Delete using file
kubectl delete -f serviceaccount.yaml

# Warning: Pods using this SA may lose API access
```

## Image Pull Secrets with Service Accounts

Link Docker registry secrets to service accounts for automatic image pulling:

```yaml
# 1. Create Docker registry secret
apiVersion: v1
kind: Secret
metadata:
  name: regcred
type: kubernetes.io/dockerconfigjson
data:
  .dockerconfigjson: <base64-encoded-docker-config>
---
# 2. Create Service Account with image pull secret
apiVersion: v1
kind: ServiceAccount
metadata:
  name: my-sa
imagePullSecrets:
- name: regcred
---
# 3. Pod using service account (automatically gets image pull secret)
apiVersion: v1
kind: Pod
metadata:
  name: my-pod
spec:
  serviceAccountName: my-sa
  containers:
  - name: app
    image: private-registry.com/myapp:1.0
```

**Alternative: Link secret to existing SA**

```bash
# Create secret first
kubectl create secret docker-registry regcred \
  --docker-server=myregistry.com \
  --docker-username=admin \
  --docker-password=secret

# Patch service account to add image pull secret
kubectl patch serviceaccount my-sa -p '{"imagePullSecrets": [{"name": "regcred"}]}'
```

## Testing Service Account Permissions

### Check Permissions

```bash
# Check if current user can perform action
kubectl auth can-i get pods
kubectl auth can-i create deployments

# Check as specific service account
kubectl auth can-i get pods --as=system:serviceaccount:default:my-service-account

# Check in specific namespace
kubectl auth can-i get pods --as=system:serviceaccount:default:my-service-account -n dev

# List all permissions for service account
kubectl auth can-i --list --as=system:serviceaccount:default:my-service-account
```

### Test from Inside Pod

```yaml
# Deploy a test pod with kubectl
apiVersion: v1
kind: Pod
metadata:
  name: kubectl-test
spec:
  serviceAccountName: my-service-account
  containers:
  - name: kubectl
    image: bitnami/kubectl:latest
    command: ["sleep", "3600"]
```

```bash
# Apply pod
kubectl apply -f kubectl-test.yaml

# Test commands from inside pod
kubectl exec -it kubectl-test -- kubectl get pods
kubectl exec -it kubectl-test -- kubectl get nodes  # Should fail if no permission
kubectl exec -it kubectl-test -- kubectl auth can-i get pods
```

## Service Account Best Practices

1. **Don't use default SA**: Create specific service accounts for applications
2. **Principle of least privilege**: Grant only necessary permissions
3. **Use namespace-scoped roles**: When possible, avoid ClusterRoles
4. **Separate SAs for different apps**: Don't share service accounts
5. **Disable auto-mount when not needed**: Save resources and improve security
6. **Audit SA usage**: Regularly review permissions
7. **Use short-lived tokens**: Consider token rotation
8. **Document permissions**: Clearly document what each SA can do
9. **Test permissions**: Use `kubectl auth can-i` before deployment
10. **Clean up unused SAs**: Remove old service accounts

## Common Use Cases

### 1. Application Reading ConfigMaps

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: configmap-reader
rules:
- apiGroups: [""]
  resources: ["configmaps"]
  verbs: ["get", "list", "watch"]
```

### 2. Application Watching Pods

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: pod-watcher
rules:
- apiGroups: [""]
  resources: ["pods"]
  verbs: ["get", "list", "watch"]
```

### 3. CI/CD Deployment

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: deployer
rules:
- apiGroups: ["apps"]
  resources: ["deployments", "replicasets"]
  verbs: ["get", "list", "create", "update", "patch"]
- apiGroups: [""]
  resources: ["pods", "services"]
  verbs: ["get", "list"]
```

### 4. Monitoring Application

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: monitoring
rules:
- apiGroups: [""]
  resources: ["pods", "nodes", "services"]
  verbs: ["get", "list", "watch"]
- apiGroups: ["apps"]
  resources: ["deployments", "statefulsets"]
  verbs: ["get", "list", "watch"]
```

## Troubleshooting

### Common Issues

#### 1. Permission Denied

```bash
# Check service account permissions
kubectl auth can-i get pods --as=system:serviceaccount:default:my-sa

# Verify role binding exists
kubectl get rolebinding
kubectl describe rolebinding my-binding

# Check role permissions
kubectl describe role my-role
```

#### 2. Service Account Not Found

```bash
# Verify SA exists
kubectl get sa my-sa

# Check namespace
kubectl get sa my-sa -n correct-namespace

# Verify pod configuration
kubectl describe pod my-pod | grep "Service Account"
```

#### 3. Token Not Mounted

```bash
# Check automount setting
kubectl get sa my-sa -o yaml | grep automountServiceAccountToken

# Verify from inside pod
kubectl exec my-pod -- ls /var/run/secrets/kubernetes.io/serviceaccount/
```

### Debugging Commands

```bash
# View service account details
kubectl describe sa my-sa

# Check what pods use this SA
kubectl get pods -o custom-columns=NAME:.metadata.name,SA:.spec.serviceAccountName

# View role bindings
kubectl get rolebindings
kubectl get clusterrolebindings

# Check RBAC permissions
kubectl auth can-i --list --as=system:serviceaccount:default:my-sa

# View token (from inside pod)
kubectl exec my-pod -- cat /var/run/secrets/kubernetes.io/serviceaccount/token

# Test API access
kubectl exec my-pod -- sh -c 'curl -k -H "Authorization: Bearer $(cat /var/run/secrets/kubernetes.io/serviceaccount/token)" https://kubernetes.default/api/v1/namespaces/default/pods'
```

## Quick Reference

```bash
# Create service account
kubectl create serviceaccount my-sa

# List service accounts
kubectl get sa

# Describe service account
kubectl describe sa my-sa

# Create role
kubectl create role pod-reader --verb=get,list --resource=pods

# Create role binding
kubectl create rolebinding my-binding --role=pod-reader --serviceaccount=default:my-sa

# Check permissions
kubectl auth can-i get pods --as=system:serviceaccount:default:my-sa

# Delete service account
kubectl delete sa my-sa

# Patch SA with image pull secret
kubectl patch sa my-sa -p '{"imagePullSecrets": [{"name": "regcred"}]}'
```

## Next Steps

- Learn about RBAC in depth
- Understand Pod Security Policies/Standards
- Explore external identity providers (OIDC)
- Study Kubernetes secrets management
- Practice with real-world RBAC scenarios
