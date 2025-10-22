# Namespaces

## What is a Namespace?

Namespaces provide a mechanism for isolating groups of resources within a single cluster. They are virtual clusters within a physical cluster.

**Key Features:**
- Resource isolation
- Resource quotas and limits
- Access control (RBAC)
- Logical separation of environments
- Multi-tenancy support

## Why Use Namespaces?

**Use Cases:**
- Separate environments (dev, staging, prod)
- Team-based isolation
- Resource organization
- Apply different policies per namespace
- Multi-tenant applications

## Default Namespaces

Kubernetes creates these namespaces by default:

### 1. default
- Default namespace for resources without a specified namespace
- Where resources go if no namespace is specified

### 2. kube-system
- System components (kube-dns, kube-proxy, etc.)
- Kubernetes control plane components
- Don't create resources here

### 3. kube-public
- Publicly readable by all users
- Used for cluster information
- Mostly reserved for cluster usage

### 4. kube-node-lease
- Node heartbeat information
- Improves node health check performance
- System use only

```bash
# View all namespaces
kubectl get namespaces
kubectl get ns  # Short form

# Output:
# NAME              STATUS   AGE
# default           Active   10d
# kube-system       Active   10d
# kube-public       Active   10d
# kube-node-lease   Active   10d
```

## Creating Namespaces

### Imperative Method

```bash
# Create namespace
kubectl create namespace dev
kubectl create ns staging  # Using short form

# Create namespace with dry-run
kubectl create namespace prod --dry-run=client -o yaml
```

### Declarative Method

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: development
  labels:
    environment: dev
    team: backend
```

```bash
# Create from YAML
kubectl apply -f namespace.yaml
```

## Managing Namespaces

### Viewing Namespaces

```bash
# List all namespaces
kubectl get namespaces
kubectl get ns

# Describe namespace
kubectl describe namespace dev

# Get namespace YAML
kubectl get namespace dev -o yaml

# Show namespace labels
kubectl get ns --show-labels
```

### Working with Resources in Namespaces

```bash
# List pods in specific namespace
kubectl get pods -n dev
kubectl get pods --namespace=dev

# List pods in all namespaces
kubectl get pods --all-namespaces
kubectl get pods -A

# Create resource in specific namespace
kubectl run nginx --image=nginx -n dev
kubectl create deployment nginx --image=nginx -n staging

# Delete resource from namespace
kubectl delete pod nginx -n dev

# Get all resources in namespace
kubectl get all -n dev
```

### Setting Default Namespace

```bash
# View current context
kubectl config current-context

# Set default namespace for context
kubectl config set-context --current --namespace=dev

# Verify
kubectl config view --minify | grep namespace

# Now all commands use dev namespace by default
kubectl get pods  # Gets pods from dev namespace

# Switch back to default
kubectl config set-context --current --namespace=default
```

### Deleting Namespaces

```bash
# Delete namespace (also deletes all resources in it)
kubectl delete namespace dev

# Delete using file
kubectl delete -f namespace.yaml

# Warning: This deletes ALL resources in the namespace!
```

## Creating Resources in Namespaces

### Method 1: Command Line

```bash
# Create pod in specific namespace
kubectl run nginx --image=nginx -n dev

# Create deployment in namespace
kubectl create deployment webapp --image=webapp:1.0 -n staging

# Expose service in namespace
kubectl expose deployment webapp --port=80 -n staging
```

### Method 2: YAML with namespace

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: nginx
  namespace: dev      # Specify namespace here
spec:
  containers:
  - name: nginx
    image: nginx
```

```bash
# Create resource
kubectl apply -f pod.yaml
# Pod will be created in 'dev' namespace as specified in YAML
```

### Method 3: Apply YAML to specific namespace

```yaml
# pod.yaml (without namespace field)
apiVersion: v1
kind: Pod
metadata:
  name: nginx
spec:
  containers:
  - name: nginx
    image: nginx
```

```bash
# Override namespace during creation
kubectl apply -f pod.yaml -n dev
```

## Complete Example: Multi-Environment Setup

```yaml
# namespaces.yaml
---
apiVersion: v1
kind: Namespace
metadata:
  name: development
  labels:
    environment: dev
---
apiVersion: v1
kind: Namespace
metadata:
  name: staging
  labels:
    environment: staging
---
apiVersion: v1
kind: Namespace
metadata:
  name: production
  labels:
    environment: prod
---
# Development deployment
apiVersion: apps/v1
kind: Deployment
metadata:
  name: webapp
  namespace: development
spec:
  replicas: 1
  selector:
    matchLabels:
      app: webapp
  template:
    metadata:
      labels:
        app: webapp
    spec:
      containers:
      - name: webapp
        image: webapp:dev
---
# Staging deployment
apiVersion: apps/v1
kind: Deployment
metadata:
  name: webapp
  namespace: staging
spec:
  replicas: 2
  selector:
    matchLabels:
      app: webapp
  template:
    metadata:
      labels:
        app: webapp
    spec:
      containers:
      - name: webapp
        image: webapp:staging
---
# Production deployment
apiVersion: apps/v1
kind: Deployment
metadata:
  name: webapp
  namespace: production
spec:
  replicas: 5
  selector:
    matchLabels:
      app: webapp
  template:
    metadata:
      labels:
        app: webapp
    spec:
      containers:
      - name: webapp
        image: webapp:1.0
```

```bash
# Create all resources
kubectl apply -f namespaces.yaml

# View deployments across namespaces
kubectl get deployments -A
```

## Resource Quotas

Limit resource consumption per namespace:

```yaml
apiVersion: v1
kind: ResourceQuota
metadata:
  name: dev-quota
  namespace: development
spec:
  hard:
    requests.cpu: "10"
    requests.memory: 20Gi
    limits.cpu: "20"
    limits.memory: 40Gi
    pods: "10"
    services: "5"
    persistentvolumeclaims: "5"
```

```bash
# Create quota
kubectl apply -f quota.yaml

# View quotas
kubectl get resourcequota -n development
kubectl describe resourcequota dev-quota -n development

# View quota usage
kubectl describe ns development
```

## Limit Ranges

Set default resource limits for pods in namespace:

```yaml
apiVersion: v1
kind: LimitRange
metadata:
  name: dev-limits
  namespace: development
spec:
  limits:
  - max:
      cpu: "2"
      memory: "2Gi"
    min:
      cpu: "100m"
      memory: "128Mi"
    default:
      cpu: "500m"
      memory: "512Mi"
    defaultRequest:
      cpu: "250m"
      memory: "256Mi"
    type: Container
```

```bash
# Create limit range
kubectl apply -f limitrange.yaml

# View limit ranges
kubectl get limitrange -n development
kubectl describe limitrange dev-limits -n development
```

## Service Discovery Across Namespaces

### DNS Format

```
<service-name>.<namespace>.svc.cluster.local
```

### Example

```yaml
# Service in 'backend' namespace
apiVersion: v1
kind: Service
metadata:
  name: database
  namespace: backend
spec:
  selector:
    app: database
  ports:
  - port: 5432
```

**Accessing from same namespace:**
```bash
# From a pod in 'backend' namespace
curl http://database:5432
```

**Accessing from different namespace:**
```bash
# From a pod in 'frontend' namespace
curl http://database.backend:5432

# Or fully qualified
curl http://database.backend.svc.cluster.local:5432
```

## Network Policies per Namespace

Restrict network traffic between namespaces:

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: deny-from-other-namespaces
  namespace: production
spec:
  podSelector: {}    # Apply to all pods in namespace
  policyTypes:
  - Ingress
  ingress:
  - from:
    - podSelector: {}  # Only allow from same namespace
```

```yaml
# Allow traffic from specific namespace
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: allow-from-frontend
  namespace: backend
spec:
  podSelector:
    matchLabels:
      app: database
  policyTypes:
  - Ingress
  ingress:
  - from:
    - namespaceSelector:
        matchLabels:
          environment: frontend
```

## RBAC with Namespaces

Control access to namespaces using Role-Based Access Control:

```yaml
# Role: Permissions within namespace
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: developer
  namespace: development
rules:
- apiGroups: ["", "apps"]
  resources: ["pods", "deployments", "services"]
  verbs: ["get", "list", "create", "update", "delete"]
---
# RoleBinding: Grant role to user
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: developer-binding
  namespace: development
subjects:
- kind: User
  name: john
  apiGroup: rbac.authorization.k8s.io
roleRef:
  kind: Role
  name: developer
  apiGroup: rbac.authorization.k8s.io
```

## Namespace Best Practices

1. **Use namespaces for logical separation**: Don't create too many
2. **Set resource quotas**: Prevent resource exhaustion
3. **Apply network policies**: Secure inter-namespace communication
4. **Use RBAC**: Control access per namespace
5. **Label namespaces**: For organization and filtering
6. **Set default limits**: Using LimitRange
7. **Naming convention**: Use consistent, meaningful names
8. **Don't use for version control**: Use labels instead
9. **Document namespace purpose**: In metadata annotations
10. **Regular cleanup**: Delete unused namespaces

## Common Patterns

### Pattern 1: Environment-Based

```bash
kubectl create ns dev
kubectl create ns staging
kubectl create ns prod
```

### Pattern 2: Team-Based

```bash
kubectl create ns team-frontend
kubectl create ns team-backend
kubectl create ns team-data
```

### Pattern 3: Application-Based

```bash
kubectl create ns app-ecommerce
kubectl create ns app-analytics
kubectl create ns app-auth
```

### Pattern 4: Customer-Based (Multi-tenancy)

```bash
kubectl create ns customer-acme
kubectl create ns customer-globex
kubectl create ns customer-initech
```

## Troubleshooting Namespaces

### Common Issues

#### 1. Resource Not Found

```bash
# Check if resource is in different namespace
kubectl get pods --all-namespaces | grep nginx

# Or search specific namespace
kubectl get pods -n dev
```

#### 2. Cannot Delete Namespace (Stuck in Terminating)

```bash
# Check what's preventing deletion
kubectl get all -n stuck-namespace

# Force delete (if needed)
kubectl delete namespace stuck-namespace --force --grace-period=0

# Check for finalizers
kubectl get namespace stuck-namespace -o yaml
# Remove finalizers if blocking deletion
```

#### 3. Resource Quota Exceeded

```bash
# Check quota
kubectl describe resourcequota -n dev

# Check current usage
kubectl describe ns dev

# Increase quota or delete resources
```

### Debugging Commands

```bash
# View namespace details
kubectl describe namespace dev

# Check all resources in namespace
kubectl get all -n dev

# Check resource quotas
kubectl get resourcequota -n dev

# Check limit ranges
kubectl get limitrange -n dev

# View events in namespace
kubectl get events -n dev --sort-by='.lastTimestamp'

# Check RBAC permissions
kubectl auth can-i get pods -n dev
kubectl auth can-i create deployments -n dev
```

## Quick Reference

```bash
# Create namespace
kubectl create namespace dev

# List namespaces
kubectl get ns

# List resources in namespace
kubectl get pods -n dev

# List resources in all namespaces
kubectl get pods -A

# Set default namespace
kubectl config set-context --current --namespace=dev

# Create resource in namespace
kubectl run nginx --image=nginx -n dev
kubectl apply -f pod.yaml -n dev

# Delete namespace
kubectl delete namespace dev

# Describe namespace
kubectl describe ns dev

# Get all in namespace
kubectl get all -n dev
```

## Next Steps

- Learn about Resource Quotas in detail
- Understand LimitRange configurations
- Study RBAC for namespace-level access control
- Explore Network Policies for namespace isolation
- Practice multi-tenant architectures
