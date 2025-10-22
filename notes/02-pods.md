# Pods

## What is a Pod?

A Pod is the smallest deployable unit in Kubernetes. It represents a single instance of a running process in your cluster.

**Key Characteristics:**
- One or more containers (usually one)
- Shared network namespace (same IP address)
- Shared storage volumes
- Ephemeral by nature (can be created and destroyed)

## Pod Lifecycle

### Pod Phases:

1. **Pending**: Pod accepted but containers not yet created
2. **Running**: Pod bound to node, all containers created, at least one running
3. **Succeeded**: All containers terminated successfully
4. **Failed**: All containers terminated, at least one failed
5. **Unknown**: Pod state cannot be determined

### Container States:

- **Waiting**: Container is waiting to start (pulling image, etc.)
- **Running**: Container is executing
- **Terminated**: Container finished execution or failed

## Creating Pods

### Imperative Way (Quick Testing)

```bash
# Create a simple nginx pod
kubectl run nginx --image=nginx

# Create a pod with custom port
kubectl run nginx --image=nginx --port=80

# Create a pod with labels
kubectl run nginx --image=nginx --labels="app=web,env=prod"

# Create a pod with environment variables
kubectl run nginx --image=nginx --env="ENV=production"

# Dry run - generate YAML without creating
kubectl run nginx --image=nginx --dry-run=client -o yaml

# Create and expose pod
kubectl run nginx --image=nginx --port=80 --expose
```

### Declarative Way (Production)

Create a file `simple-pod.yaml`:

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: nginx-pod
  labels:
    app: nginx
    tier: frontend
spec:
  containers:
  - name: nginx
    image: nginx:1.21
    ports:
    - containerPort: 80
```

```bash
# Create pod from YAML
kubectl apply -f simple-pod.yaml

# Or use create (cannot be updated)
kubectl create -f simple-pod.yaml
```

## Managing Pods

### Viewing Pods

```bash
# List all pods in current namespace
kubectl get pods

# List pods in all namespaces
kubectl get pods --all-namespaces
kubectl get pods -A

# Wide output (shows node, IP)
kubectl get pods -o wide

# Show labels
kubectl get pods --show-labels

# Filter by label
kubectl get pods -l app=nginx
kubectl get pods --selector app=nginx

# Watch pods in real-time
kubectl get pods --watch
kubectl get pods -w

# Get pod in JSON/YAML format
kubectl get pod nginx-pod -o yaml
kubectl get pod nginx-pod -o json
```

### Describing Pods

```bash
# Get detailed information
kubectl describe pod nginx-pod

# Shows: events, conditions, container info, volumes, etc.
```

### Pod Logs

```bash
# View logs
kubectl logs nginx-pod

# Follow logs (stream)
kubectl logs -f nginx-pod

# Previous container logs (if crashed)
kubectl logs nginx-pod --previous

# Logs from specific container in multi-container pod
kubectl logs nginx-pod -c container-name

# Last 100 lines
kubectl logs nginx-pod --tail=100

# Logs since 1 hour ago
kubectl logs nginx-pod --since=1h
```

### Executing Commands in Pods

```bash
# Execute a command
kubectl exec nginx-pod -- ls /usr/share/nginx/html

# Interactive shell
kubectl exec -it nginx-pod -- /bin/bash
kubectl exec -it nginx-pod -- sh

# Execute in specific container (multi-container pod)
kubectl exec -it nginx-pod -c container-name -- /bin/bash

# Run a single command
kubectl exec nginx-pod -- env
kubectl exec nginx-pod -- cat /etc/nginx/nginx.conf
```

### Deleting Pods

```bash
# Delete a pod
kubectl delete pod nginx-pod

# Delete pod using file
kubectl delete -f simple-pod.yaml

# Delete all pods in namespace
kubectl delete pods --all

# Delete pods by label
kubectl delete pods -l app=nginx

# Force delete (not recommended)
kubectl delete pod nginx-pod --force --grace-period=0
```

## Advanced Pod Configuration

### Pod with Resource Limits

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: resource-pod
spec:
  containers:
  - name: nginx
    image: nginx
    resources:
      requests:
        memory: "64Mi"
        cpu: "250m"
      limits:
        memory: "128Mi"
        cpu: "500m"
```

### Pod with Environment Variables

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: env-pod
spec:
  containers:
  - name: nginx
    image: nginx
    env:
    - name: ENVIRONMENT
      value: "production"
    - name: LOG_LEVEL
      value: "info"
```

### Pod with Volume

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: volume-pod
spec:
  containers:
  - name: nginx
    image: nginx
    volumeMounts:
    - name: html-volume
      mountPath: /usr/share/nginx/html
  volumes:
  - name: html-volume
    emptyDir: {}
```

### Pod with Liveness and Readiness Probes

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: probe-pod
spec:
  containers:
  - name: nginx
    image: nginx
    livenessProbe:
      httpGet:
        path: /
        port: 80
      initialDelaySeconds: 3
      periodSeconds: 3
    readinessProbe:
      httpGet:
        path: /
        port: 80
      initialDelaySeconds: 5
      periodSeconds: 5
```

### Pod with Init Container

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: init-pod
spec:
  initContainers:
  - name: init-service
    image: busybox
    command: ['sh', '-c', 'echo Initializing... && sleep 5']
  containers:
  - name: main-app
    image: nginx
```

## Pod Troubleshooting

### Common Issues and Commands

```bash
# Pod stuck in Pending
kubectl describe pod <pod-name>
# Check: Events, Conditions, Resource availability

# Pod in CrashLoopBackOff
kubectl logs <pod-name>
kubectl logs <pod-name> --previous
kubectl describe pod <pod-name>

# Pod in ImagePullBackOff
kubectl describe pod <pod-name>
# Check: Image name, registry credentials

# Check pod events
kubectl get events --field-selector involvedObject.name=<pod-name>

# Check resource usage
kubectl top pod <pod-name>

# Port forward to access pod
kubectl port-forward pod/nginx-pod 8080:80
# Access via localhost:8080
```

## Pod Networking

```bash
# Get pod IP
kubectl get pod nginx-pod -o wide

# Access pod from another pod
kubectl run debug --image=busybox -it --rm -- wget -O- http://<pod-ip>

# DNS within cluster
kubectl run debug --image=busybox -it --rm -- nslookup <service-name>
```

## Best Practices

1. **Don't use naked Pods**: Use Deployments, ReplicaSets, or StatefulSets
2. **Set resource requests and limits**: Ensures proper scheduling and prevents resource exhaustion
3. **Use health probes**: Implement liveness and readiness probes
4. **Use labels**: Organize and select pods effectively
5. **Single responsibility**: One main process per container
6. **Use init containers**: For setup tasks before main container starts
7. **Don't store data in containers**: Use volumes for persistence

## Quick Reference

```bash
# Create
kubectl run <name> --image=<image>

# List
kubectl get pods

# Details
kubectl describe pod <name>

# Logs
kubectl logs <name>

# Execute
kubectl exec -it <name> -- /bin/bash

# Delete
kubectl delete pod <name>

# Edit
kubectl edit pod <name>

# Get YAML
kubectl get pod <name> -o yaml

# Apply changes
kubectl apply -f pod.yaml
```

## Next Steps

- Learn about YAML syntax in detail
- Understand ReplicaSets for managing multiple pod replicas
- Explore Deployments for declarative updates
- Study Services for networking between pods
