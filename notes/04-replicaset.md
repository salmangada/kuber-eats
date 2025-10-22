# ReplicaSet

## What is a ReplicaSet?

A ReplicaSet ensures that a specified number of pod replicas are running at any given time. It maintains a stable set of replica Pods and replaces failed pods automatically.

**Key Features:**
- Ensures desired number of pods are running
- Self-healing: Creates new pods if any fail
- Load balancing across pods
- Horizontal scaling

## Why ReplicaSet?

**Problem with bare Pods:**
- If a pod dies, it's not recreated
- No automatic scaling
- No load distribution

**ReplicaSet Solution:**
- Maintains desired replica count
- Automatically replaces failed pods
- Enables horizontal scaling
- Works with selectors for flexibility

## ReplicaSet Architecture

```
┌─────────────────────────────────────┐
│         ReplicaSet                  │
│  ┌───────────────────────────────┐  │
│  │ Desired Replicas: 3           │  │
│  │ Current Replicas: 3           │  │
│  │ Ready Replicas: 3             │  │
│  └───────────────────────────────┘  │
│              │                       │
│    Manages   │                       │
└──────────────┼───────────────────────┘
               │
     ┌─────────┼─────────┐
     │         │         │
┌────▼───┐ ┌──▼────┐ ┌──▼────┐
│ Pod 1  │ │ Pod 2 │ │ Pod 3 │
└────────┘ └───────┘ └───────┘
```

## Creating ReplicaSets

### Basic ReplicaSet YAML

```yaml
apiVersion: apps/v1
kind: ReplicaSet
metadata:
  name: nginx-replicaset
  labels:
    app: nginx
spec:
  replicas: 3                    # Desired number of pods
  selector:                      # How to identify pods to manage
    matchLabels:
      app: nginx
  template:                      # Pod template
    metadata:
      labels:
        app: nginx              # Must match selector
    spec:
      containers:
      - name: nginx
        image: nginx:1.21
        ports:
        - containerPort: 80
```

### Understanding Key Components

#### 1. spec.replicas
Number of pod copies to maintain

```yaml
spec:
  replicas: 5  # Maintain 5 pods
```

#### 2. spec.selector
Identifies which pods belong to this ReplicaSet

```yaml
spec:
  selector:
    matchLabels:              # Simple equality-based
      app: nginx
      tier: frontend

  # Or use matchExpressions for complex matching
  selector:
    matchExpressions:
    - key: app
      operator: In
      values:
      - nginx
      - httpd
```

#### 3. spec.template
Pod template used to create new pods

```yaml
spec:
  template:
    metadata:
      labels:
        app: nginx           # Labels for the pods
    spec:
      containers:
      - name: nginx
        image: nginx:1.21
```

**Important:** Pod template labels MUST match the selector!

## Managing ReplicaSets

### Creating ReplicaSets

```bash
# Create from YAML
kubectl apply -f replicaset.yaml
kubectl create -f replicaset.yaml

# Note: No direct imperative command to create ReplicaSet
# Use deployment creation instead (covered later)
```

### Viewing ReplicaSets

```bash
# List ReplicaSets
kubectl get replicaset
kubectl get rs  # Short form

# Wide output (shows images, selectors)
kubectl get rs -o wide

# Show labels
kubectl get rs --show-labels

# Filter by label
kubectl get rs -l app=nginx

# Watch ReplicaSets
kubectl get rs --watch
kubectl get rs -w

# Detailed information
kubectl describe rs nginx-replicaset

# Get YAML definition
kubectl get rs nginx-replicaset -o yaml
```

### Scaling ReplicaSets

```bash
# Scale using kubectl scale
kubectl scale rs nginx-replicaset --replicas=5

# Scale multiple ReplicaSets
kubectl scale rs rs1 rs2 rs3 --replicas=3

# Conditional scaling (only if current replicas = 3)
kubectl scale rs nginx-replicaset --current-replicas=3 --replicas=5

# Edit ReplicaSet to change replicas
kubectl edit rs nginx-replicaset
# Change spec.replicas value

# Update YAML file and apply
# Edit replicaset.yaml: change replicas to 5
kubectl apply -f replicaset.yaml
```

### Deleting ReplicaSets

```bash
# Delete ReplicaSet (also deletes all pods)
kubectl delete rs nginx-replicaset

# Delete using file
kubectl delete -f replicaset.yaml

# Delete ReplicaSet but keep pods
kubectl delete rs nginx-replicaset --cascade=orphan

# Delete all ReplicaSets in namespace
kubectl delete rs --all
```

## ReplicaSet vs Replication Controller

**Replication Controller** (older, deprecated):
- Only supports equality-based selectors
- `selector: {app: nginx}`

**ReplicaSet** (current, recommended):
- Supports set-based selectors
- More flexible matching
- Required by Deployments

```yaml
# ReplicaSet supports this:
selector:
  matchExpressions:
  - key: environment
    operator: In
    values:
    - production
    - staging
```

## How ReplicaSet Works

### Pod Lifecycle Management

1. **ReplicaSet created** → Reads desired replicas count
2. **Checks current state** → Counts pods matching selector
3. **Creates/Deletes pods** → Matches desired state
4. **Continuous monitoring** → Watches pod status
5. **Self-healing** → Replaces failed pods

### Example Scenario

```bash
# Create ReplicaSet with 3 replicas
kubectl apply -f replicaset.yaml

# Check status
kubectl get rs
# NAME               DESIRED   CURRENT   READY   AGE
# nginx-replicaset   3         3         3       10s

# Check pods
kubectl get pods
# NAME                     READY   STATUS    RESTARTS   AGE
# nginx-replicaset-abc12   1/1     Running   0          10s
# nginx-replicaset-def34   1/1     Running   0          10s
# nginx-replicaset-ghi56   1/1     Running   0          10s

# Delete a pod manually
kubectl delete pod nginx-replicaset-abc12

# ReplicaSet immediately creates a new pod
kubectl get pods
# NAME                     READY   STATUS    RESTARTS   AGE
# nginx-replicaset-def34   1/1     Running   0          1m
# nginx-replicaset-ghi56   1/1     Running   0          1m
# nginx-replicaset-jkl78   1/1     Running   0          2s  ← NEW POD
```

## Advanced ReplicaSet Configurations

### ReplicaSet with Resource Limits

```yaml
apiVersion: apps/v1
kind: ReplicaSet
metadata:
  name: nginx-rs
spec:
  replicas: 3
  selector:
    matchLabels:
      app: nginx
  template:
    metadata:
      labels:
        app: nginx
    spec:
      containers:
      - name: nginx
        image: nginx:1.21
        resources:
          requests:
            memory: "64Mi"
            cpu: "250m"
          limits:
            memory: "128Mi"
            cpu: "500m"
```

### ReplicaSet with Environment Variables

```yaml
apiVersion: apps/v1
kind: ReplicaSet
metadata:
  name: app-rs
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
      containers:
      - name: app
        image: myapp:1.0
        env:
        - name: ENVIRONMENT
          value: "production"
        - name: DATABASE_URL
          value: "postgres://db:5432"
```

### ReplicaSet with Multiple Match Expressions

```yaml
apiVersion: apps/v1
kind: ReplicaSet
metadata:
  name: complex-rs
spec:
  replicas: 3
  selector:
    matchExpressions:
    - key: app
      operator: In
      values:
      - nginx
      - httpd
    - key: environment
      operator: NotIn
      values:
      - development
    - key: tier
      operator: Exists
  template:
    metadata:
      labels:
        app: nginx
        environment: production
        tier: frontend
    spec:
      containers:
      - name: nginx
        image: nginx
```

**Selector Operators:**
- `In`: Label value must be in the list
- `NotIn`: Label value must not be in the list
- `Exists`: Label key must exist (any value)
- `DoesNotExist`: Label key must not exist

## Troubleshooting ReplicaSets

### Common Issues

#### 1. Pods Not Created

```bash
# Check ReplicaSet events
kubectl describe rs nginx-replicaset

# Common causes:
# - Invalid image name
# - Insufficient resources
# - Node selector constraints
# - Image pull errors
```

#### 2. Selector Doesn't Match Template

```yaml
# WRONG - Selector and template labels don't match
spec:
  selector:
    matchLabels:
      app: nginx          # This label
  template:
    metadata:
      labels:
        app: httpd        # Doesn't match this

# Error: selector does not match template labels
```

#### 3. ReplicaSet Not Scaling

```bash
# Check resource quotas
kubectl describe resourcequota

# Check node capacity
kubectl describe nodes

# Check pod status
kubectl get pods
kubectl describe pod <pod-name>
```

### Debugging Commands

```bash
# View ReplicaSet status
kubectl get rs nginx-replicaset -o wide

# Detailed events and conditions
kubectl describe rs nginx-replicaset

# Check managed pods
kubectl get pods -l app=nginx

# View logs from all pods
kubectl logs -l app=nginx --all-containers=true

# Check events in namespace
kubectl get events --sort-by='.lastTimestamp'
```

## ReplicaSet Best Practices

1. **Don't use ReplicaSets directly**: Use Deployments instead (they manage ReplicaSets)
2. **Match selectors carefully**: Ensure template labels match selector
3. **Use meaningful labels**: Makes selection and debugging easier
4. **Set resource limits**: Prevents resource exhaustion
5. **Use readiness probes**: Ensures only ready pods receive traffic
6. **Version your images**: Don't use `latest` tag
7. **Plan for scaling**: Set appropriate initial replica count

## When to Use ReplicaSets

**Use ReplicaSets when:**
- You need a specific number of pod replicas
- You want self-healing capability
- You need to scale horizontally

**Don't use ReplicaSets when:**
- You need rolling updates → Use Deployments
- You need stateful applications → Use StatefulSets
- You need one pod per node → Use DaemonSets
- You need batch processing → Use Jobs

## Quick Reference

```bash
# Create
kubectl apply -f replicaset.yaml

# List
kubectl get rs

# Scale
kubectl scale rs <name> --replicas=5

# Details
kubectl describe rs <name>

# Delete (with pods)
kubectl delete rs <name>

# Delete (keep pods)
kubectl delete rs <name> --cascade=orphan

# Edit
kubectl edit rs <name>

# Get YAML
kubectl get rs <name> -o yaml

# Watch pods
kubectl get pods -l app=nginx -w
```

## Next Steps

- Learn about Deployments (which manage ReplicaSets)
- Understand rolling updates and rollbacks
- Explore horizontal pod autoscaling
- Study different update strategies
