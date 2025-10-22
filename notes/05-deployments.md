# Deployments

## What is a Deployment?

A Deployment is a higher-level abstraction that manages ReplicaSets and provides declarative updates to Pods. It's the recommended way to deploy and manage applications in Kubernetes.

**Key Features:**
- Manages ReplicaSets automatically
- Rolling updates with zero downtime
- Rollback to previous versions
- Pause and resume deployments
- Scale applications easily
- Self-healing and high availability

## Deployment vs ReplicaSet

| Feature | ReplicaSet | Deployment |
|---------|-----------|------------|
| Manages Pods | ✓ | ✓ |
| Self-healing | ✓ | ✓ |
| Scaling | ✓ | ✓ |
| Rolling Updates | ✗ | ✓ |
| Rollback | ✗ | ✓ |
| Version History | ✗ | ✓ |
| Update Strategy | ✗ | ✓ |

**Use Deployments** for stateless applications instead of ReplicaSets directly.

## Deployment Architecture

```
┌────────────────────────────────────────────┐
│           Deployment                        │
│  ┌──────────────────────────────────────┐  │
│  │ Manages ReplicaSets                  │  │
│  │ Handles Updates & Rollbacks          │  │
│  └───────────┬──────────────────────────┘  │
└──────────────┼─────────────────────────────┘
               │
        ┌──────┴────────┐
        │               │
┌───────▼─────┐   ┌────▼────────┐
│ReplicaSet v2│   │ReplicaSet v1│
│ (current)   │   │  (old)      │
│ replicas: 3 │   │ replicas: 0 │
└──────┬──────┘   └─────────────┘
       │
   ┌───┴───┬───────┐
   │       │       │
┌──▼──┐ ┌──▼──┐ ┌──▼──┐
│Pod 1│ │Pod 2│ │Pod 3│
└─────┘ └─────┘ └─────┘
```

## Creating Deployments

### Imperative Method

```bash
# Create a deployment
kubectl create deployment nginx --image=nginx

# Create with replicas
kubectl create deployment nginx --image=nginx --replicas=3

# Create and save YAML
kubectl create deployment nginx --image=nginx --dry-run=client -o yaml > deployment.yaml

# Create with port
kubectl create deployment nginx --image=nginx --port=80
```

### Declarative Method (Recommended)

**Basic Deployment YAML:**

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: nginx-deployment
  labels:
    app: nginx
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
        ports:
        - containerPort: 80
```

```bash
# Create deployment
kubectl apply -f deployment.yaml
```

## Managing Deployments

### Viewing Deployments

```bash
# List deployments
kubectl get deployments
kubectl get deploy  # Short form

# Wide output
kubectl get deploy -o wide

# Show labels
kubectl get deploy --show-labels

# Watch deployments
kubectl get deploy --watch
kubectl get deploy -w

# Detailed information
kubectl describe deploy nginx-deployment

# Get YAML
kubectl get deploy nginx-deployment -o yaml

# View all (deployment, replicaset, pods)
kubectl get all
```

### Deployment Status

```bash
# Check rollout status
kubectl rollout status deployment/nginx-deployment

# View rollout history
kubectl rollout history deployment/nginx-deployment

# View specific revision
kubectl rollout history deployment/nginx-deployment --revision=2
```

### Scaling Deployments

```bash
# Scale deployment
kubectl scale deployment nginx-deployment --replicas=5

# Autoscale (HPA)
kubectl autoscale deployment nginx-deployment --min=3 --max=10 --cpu-percent=80
```

### Updating Deployments

```bash
# Update image
kubectl set image deployment/nginx-deployment nginx=nginx:1.22

# Update multiple containers
kubectl set image deployment/nginx-deployment nginx=nginx:1.22 sidecar=sidecar:2.0

# Edit deployment
kubectl edit deployment nginx-deployment

# Update using YAML
# Edit deployment.yaml, then:
kubectl apply -f deployment.yaml

# Update with annotation
kubectl annotate deployment nginx-deployment kubernetes.io/change-cause="Update to nginx 1.22"
```

### Rolling Back Deployments

```bash
# Rollback to previous version
kubectl rollout undo deployment/nginx-deployment

# Rollback to specific revision
kubectl rollout undo deployment/nginx-deployment --to-revision=2

# View history
kubectl rollout history deployment/nginx-deployment
```

### Pausing and Resuming

```bash
# Pause deployment (for multiple updates)
kubectl rollout pause deployment/nginx-deployment

# Make multiple changes
kubectl set image deployment/nginx-deployment nginx=nginx:1.22
kubectl set resources deployment/nginx-deployment -c nginx --limits=cpu=200m,memory=512Mi

# Resume deployment
kubectl rollout resume deployment/nginx-deployment
```

### Deleting Deployments

```bash
# Delete deployment (also deletes ReplicaSets and Pods)
kubectl delete deployment nginx-deployment

# Delete using file
kubectl delete -f deployment.yaml

# Delete all deployments
kubectl delete deployments --all
```

## Update Strategies

### 1. Rolling Update (Default)

Updates pods gradually, ensuring availability.

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: nginx-deployment
spec:
  replicas: 10
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 2           # Max new pods above desired replicas
      maxUnavailable: 1     # Max pods unavailable during update
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
```

**Parameters:**
- `maxSurge`: Maximum number of pods above desired replicas (can be number or %)
- `maxUnavailable`: Maximum number of pods unavailable during update (can be number or %)

**Example:**
- Replicas: 10
- maxSurge: 2 → Can have up to 12 pods during update
- maxUnavailable: 1 → At least 9 pods must be available

### 2. Recreate Strategy

Terminates all existing pods before creating new ones.

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: nginx-deployment
spec:
  replicas: 3
  strategy:
    type: Recreate
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
```

**Use Cases:**
- Applications that can't run multiple versions simultaneously
- When downtime is acceptable
- Database schema updates

## Complete Deployment Example

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: webapp-deployment
  labels:
    app: webapp
  annotations:
    kubernetes.io/change-cause: "Initial deployment"
spec:
  replicas: 3
  revisionHistoryLimit: 10        # Number of old ReplicaSets to keep
  progressDeadlineSeconds: 600    # Timeout for rollout
  minReadySeconds: 10             # Min time for pod to be ready
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  selector:
    matchLabels:
      app: webapp
  template:
    metadata:
      labels:
        app: webapp
        version: v1.0
    spec:
      containers:
      - name: webapp
        image: webapp:1.0
        ports:
        - containerPort: 8080
          name: http
        env:
        - name: ENVIRONMENT
          value: production
        resources:
          requests:
            memory: "128Mi"
            cpu: "250m"
          limits:
            memory: "256Mi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /healthz
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /ready
            port: 8080
          initialDelaySeconds: 5
          periodSeconds: 5
```

## Rollout Process

### Rolling Update Flow

1. **New ReplicaSet Created**: With updated pod template
2. **Gradual Scaling**: New ReplicaSet scales up, old scales down
3. **Health Checks**: New pods must pass readiness probes
4. **Controlled Rollout**: Respects maxSurge and maxUnavailable
5. **Completion**: Old ReplicaSet scaled to 0

**Example Timeline:**

```
Initial State: 3 pods (v1)
┌───┐ ┌───┐ ┌───┐
│v1 │ │v1 │ │v1 │
└───┘ └───┘ └───┘

Step 1: Create 1 new pod (maxSurge=1)
┌───┐ ┌───┐ ┌───┐ ┌───┐
│v1 │ │v1 │ │v1 │ │v2 │ ← New
└───┘ └───┘ └───┘ └───┘

Step 2: Remove 1 old pod
┌───┐ ┌───┐ ┌───┐
│v1 │ │v1 │ │v2 │
└───┘ └───┘ └───┘

Step 3: Add another new pod
┌───┐ ┌───┐ ┌───┐ ┌───┐
│v1 │ │v1 │ │v2 │ │v2 │
└───┘ └───┘ └───┘ └───┘

Step 4: Remove old pod
┌───┐ ┌───┐ ┌───┐
│v1 │ │v2 │ │v2 │
└───┘ └───┘ └───┘

Step 5: Complete
┌───┐ ┌───┐ ┌───┐
│v2 │ │v2 │ │v2 │
└───┘ └───┘ └───┘
```

## Advanced Deployment Patterns

### Blue-Green Deployment

Use labels to switch between versions:

```yaml
# Blue deployment
apiVersion: apps/v1
kind: Deployment
metadata:
  name: app-blue
spec:
  replicas: 3
  selector:
    matchLabels:
      app: myapp
      version: blue
  template:
    metadata:
      labels:
        app: myapp
        version: blue
    spec:
      containers:
      - name: app
        image: myapp:v1
---
# Green deployment
apiVersion: apps/v1
kind: Deployment
metadata:
  name: app-green
spec:
  replicas: 3
  selector:
    matchLabels:
      app: myapp
      version: green
  template:
    metadata:
      labels:
        app: myapp
        version: green
    spec:
      containers:
      - name: app
        image: myapp:v2
```

Switch traffic by updating Service selector:
```bash
kubectl patch service myapp -p '{"spec":{"selector":{"version":"green"}}}'
```

### Canary Deployment

Run both versions simultaneously:

```yaml
# Stable deployment (90% traffic)
apiVersion: apps/v1
kind: Deployment
metadata:
  name: app-stable
spec:
  replicas: 9
  selector:
    matchLabels:
      app: myapp
      track: stable
  template:
    metadata:
      labels:
        app: myapp
        track: stable
    spec:
      containers:
      - name: app
        image: myapp:v1
---
# Canary deployment (10% traffic)
apiVersion: apps/v1
kind: Deployment
metadata:
  name: app-canary
spec:
  replicas: 1
  selector:
    matchLabels:
      app: myapp
      track: canary
  template:
    metadata:
      labels:
        app: myapp
        track: canary
    spec:
      containers:
      - name: app
        image: myapp:v2
```

## Troubleshooting Deployments

### Common Issues

#### 1. Deployment Stuck in Progress

```bash
# Check deployment status
kubectl rollout status deployment/nginx-deployment

# Check events
kubectl describe deployment nginx-deployment

# Check pods
kubectl get pods -l app=nginx
kubectl describe pod <pod-name>

# Common causes:
# - Image pull errors
# - Insufficient resources
# - Failed health checks
# - Invalid configuration
```

#### 2. Rollout Failed

```bash
# Check rollout history
kubectl rollout history deployment/nginx-deployment

# Rollback
kubectl rollout undo deployment/nginx-deployment

# Check events
kubectl get events --sort-by='.lastTimestamp'
```

#### 3. Pods CrashLoopBackOff

```bash
# Check logs
kubectl logs deployment/nginx-deployment
kubectl logs <pod-name> --previous

# Check resource limits
kubectl describe deployment nginx-deployment

# Check health probes
kubectl describe pod <pod-name>
```

### Debugging Commands

```bash
# View deployment details
kubectl describe deployment <name>

# Check ReplicaSets
kubectl get rs -l app=nginx

# Check pods
kubectl get pods -l app=nginx

# View logs
kubectl logs deployment/<name>
kubectl logs -l app=nginx --all-containers=true

# Check events
kubectl get events --field-selector involvedObject.name=<name>

# Check rollout status
kubectl rollout status deployment/<name>

# Check resource usage
kubectl top deployment <name>
kubectl top pods -l app=nginx
```

## Best Practices

1. **Use Deployments, not ReplicaSets**: Deployments provide more features
2. **Set resource requests and limits**: For proper scheduling and stability
3. **Implement health probes**: Both liveness and readiness
4. **Use rolling updates**: Default strategy for zero downtime
5. **Keep revision history**: Set `revisionHistoryLimit` appropriately (default: 10)
6. **Tag images properly**: Never use `latest` in production
7. **Use labels wisely**: For organization and selection
8. **Set readiness probes**: Prevent routing to unhealthy pods
9. **Monitor rollouts**: Use `kubectl rollout status`
10. **Test rollbacks**: Practice in non-production environments

## Quick Reference

```bash
# Create
kubectl create deployment nginx --image=nginx --replicas=3
kubectl apply -f deployment.yaml

# List
kubectl get deployments

# Scale
kubectl scale deployment nginx --replicas=5

# Update image
kubectl set image deployment/nginx nginx=nginx:1.22

# Rollout status
kubectl rollout status deployment/nginx

# Rollout history
kubectl rollout history deployment/nginx

# Rollback
kubectl rollout undo deployment/nginx

# Pause
kubectl rollout pause deployment/nginx

# Resume
kubectl rollout resume deployment/nginx

# Delete
kubectl delete deployment nginx

# Describe
kubectl describe deployment nginx
```

## Next Steps

- Learn about Services for networking
- Understand Horizontal Pod Autoscaling
- Explore StatefulSets for stateful applications
- Study Ingress for HTTP routing
- Practice blue-green and canary deployments
