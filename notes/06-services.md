# Services

## What is a Service?

A Service is an abstraction that defines a logical set of Pods and a policy to access them. Services provide stable networking for ephemeral Pods.

**Why Services?**
- Pods have dynamic IP addresses (change on restart)
- Services provide stable endpoint
- Load balancing across pods
- Service discovery via DNS
- Enable communication between components

## Service Types

Kubernetes provides 4 types of Services:

### 1. ClusterIP (Default)

Exposes service on a cluster-internal IP. Only accessible within the cluster.

**Use Case:** Internal communication between services

```yaml
apiVersion: v1
kind: Service
metadata:
  name: backend-service
spec:
  type: ClusterIP
  selector:
    app: backend
  ports:
  - port: 80          # Service port
    targetPort: 8080  # Container port
```

### 2. NodePort

Exposes service on each Node's IP at a static port (30000-32767).

**Use Case:** External access for development/testing

```yaml
apiVersion: v1
kind: Service
metadata:
  name: webapp-service
spec:
  type: NodePort
  selector:
    app: webapp
  ports:
  - port: 80          # Service port
    targetPort: 8080  # Container port
    nodePort: 30080   # External port (optional)
```

### 3. LoadBalancer

Exposes service externally using a cloud provider's load balancer.

**Use Case:** Production external access (AWS, GCP, Azure)

```yaml
apiVersion: v1
kind: Service
metadata:
  name: frontend-service
spec:
  type: LoadBalancer
  selector:
    app: frontend
  ports:
  - port: 80
    targetPort: 8080
```

### 4. ExternalName

Maps service to a DNS name (no proxying).

**Use Case:** Reference external services by name

```yaml
apiVersion: v1
kind: Service
metadata:
  name: external-db
spec:
  type: ExternalName
  externalName: database.example.com
```

## Service Architecture

```
┌─────────────────────────────────────────────┐
│              Service (ClusterIP)            │
│  Name: backend-service                      │
│  ClusterIP: 10.100.200.50                  │
│  Port: 80 → TargetPort: 8080               │
└──────────────┬──────────────────────────────┘
               │ (Load Balances)
        ┌──────┴──────┬─────────┐
        │             │         │
   ┌────▼───┐   ┌────▼───┐   ┌─▼──────┐
   │ Pod 1  │   │ Pod 2  │   │ Pod 3  │
   │ :8080  │   │ :8080  │   │ :8080  │
   └────────┘   └────────┘   └────────┘
   app=backend  app=backend  app=backend
```

## Creating Services

### Imperative Commands

```bash
# Expose a deployment
kubectl expose deployment nginx --port=80 --target-port=8080

# Create ClusterIP service
kubectl expose deployment nginx --type=ClusterIP --port=80

# Create NodePort service
kubectl expose deployment nginx --type=NodePort --port=80

# Create LoadBalancer service
kubectl expose deployment nginx --type=LoadBalancer --port=80

# Expose a pod
kubectl expose pod nginx --port=80 --name=nginx-service

# Generate YAML
kubectl expose deployment nginx --port=80 --dry-run=client -o yaml > service.yaml

# Create service using create command
kubectl create service clusterip my-service --tcp=80:8080
kubectl create service nodeport my-service --tcp=80:8080 --node-port=30080
```

### Declarative Method

**ClusterIP Service:**

```yaml
apiVersion: v1
kind: Service
metadata:
  name: backend-service
  labels:
    app: backend
spec:
  type: ClusterIP
  selector:
    app: backend      # Matches pods with this label
  ports:
  - name: http
    protocol: TCP
    port: 80          # Port service listens on
    targetPort: 8080  # Port on the pod
```

**NodePort Service:**

```yaml
apiVersion: v1
kind: Service
metadata:
  name: webapp-nodeport
spec:
  type: NodePort
  selector:
    app: webapp
  ports:
  - port: 80
    targetPort: 8080
    nodePort: 30080   # 30000-32767 range
```

**LoadBalancer Service:**

```yaml
apiVersion: v1
kind: Service
metadata:
  name: frontend-lb
spec:
  type: LoadBalancer
  selector:
    app: frontend
  ports:
  - port: 80
    targetPort: 8080
```

## Managing Services

### Viewing Services

```bash
# List all services
kubectl get services
kubectl get svc  # Short form

# Wide output
kubectl get svc -o wide

# Show labels
kubectl get svc --show-labels

# Filter by label
kubectl get svc -l app=nginx

# Describe service
kubectl describe svc backend-service

# Get YAML
kubectl get svc backend-service -o yaml

# Get endpoints
kubectl get endpoints backend-service
kubectl get ep backend-service
```

### Testing Services

```bash
# From within cluster (using a pod)
kubectl run debug --image=busybox -it --rm -- wget -O- http://backend-service

# Port forward to local machine
kubectl port-forward service/backend-service 8080:80
# Access via localhost:8080

# For NodePort services
kubectl get nodes -o wide  # Get node IP
# Access via http://<node-ip>:<node-port>

# For LoadBalancer services
kubectl get svc backend-service
# Use EXTERNAL-IP shown
```

### Deleting Services

```bash
# Delete service
kubectl delete svc backend-service

# Delete using file
kubectl delete -f service.yaml

# Delete multiple services
kubectl delete svc svc1 svc2 svc3

# Delete all services (careful!)
kubectl delete svc --all
```

## Service Discovery

Kubernetes provides two methods for service discovery:

### 1. Environment Variables

When a pod is created, environment variables are injected:

```bash
# Inside a pod
env | grep BACKEND_SERVICE
# BACKEND_SERVICE_HOST=10.100.200.50
# BACKEND_SERVICE_PORT=80
```

### 2. DNS (Recommended)

CoreDNS provides DNS names for services:

```bash
# Same namespace
curl http://backend-service

# Different namespace
curl http://backend-service.namespace-name

# Fully qualified domain name (FQDN)
curl http://backend-service.namespace-name.svc.cluster.local
```

**DNS Format:**
```
<service-name>.<namespace>.svc.cluster.local
```

## Service Port Mapping

Understanding port terminology:

```yaml
spec:
  ports:
  - name: http
    port: 80           # Service port (other pods use this)
    targetPort: 8080   # Container port (application listens on this)
    nodePort: 30080    # External port on node (NodePort only)
```

**Example Flow:**
```
External Request → NodePort (30080)
                 ↓
Service Port (80)
                 ↓
Target Port (8080) → Container
```

## Advanced Service Configurations

### Multiple Ports

```yaml
apiVersion: v1
kind: Service
metadata:
  name: multi-port-service
spec:
  selector:
    app: myapp
  ports:
  - name: http
    port: 80
    targetPort: 8080
  - name: https
    port: 443
    targetPort: 8443
  - name: metrics
    port: 9090
    targetPort: 9090
```

### Named Ports

Define port names in pod, reference in service:

```yaml
# Pod/Deployment
apiVersion: v1
kind: Pod
metadata:
  name: nginx
spec:
  containers:
  - name: nginx
    image: nginx
    ports:
    - name: http
      containerPort: 80
    - name: https
      containerPort: 443
---
# Service
apiVersion: v1
kind: Service
metadata:
  name: nginx-service
spec:
  selector:
    app: nginx
  ports:
  - name: http
    port: 80
    targetPort: http   # References named port
  - name: https
    port: 443
    targetPort: https  # References named port
```

### Session Affinity

Route requests from same client to same pod:

```yaml
apiVersion: v1
kind: Service
metadata:
  name: sticky-service
spec:
  selector:
    app: myapp
  sessionAffinity: ClientIP
  sessionAffinityConfig:
    clientIP:
      timeoutSeconds: 3600
  ports:
  - port: 80
    targetPort: 8080
```

### Headless Service

No cluster IP allocated, DNS returns pod IPs directly:

```yaml
apiVersion: v1
kind: Service
metadata:
  name: headless-service
spec:
  clusterIP: None      # Headless service
  selector:
    app: myapp
  ports:
  - port: 80
    targetPort: 8080
```

**Use Cases:**
- StatefulSets
- Custom load balancing
- Direct pod-to-pod communication

## Service and Deployment Together

Complete example with deployment and service:

```yaml
# Deployment
apiVersion: apps/v1
kind: Deployment
metadata:
  name: webapp
spec:
  replicas: 3
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
        ports:
        - containerPort: 8080
---
# Service
apiVersion: v1
kind: Service
metadata:
  name: webapp-service
spec:
  type: LoadBalancer
  selector:
    app: webapp       # Must match deployment labels
  ports:
  - port: 80
    targetPort: 8080
```

## Service Endpoints

Services route traffic to endpoints (pod IPs):

```bash
# View endpoints
kubectl get endpoints backend-service

# Describe endpoints
kubectl describe endpoints backend-service

# Endpoints are automatically managed based on:
# 1. Pod selector matching
# 2. Pod readiness status
```

**Manual Endpoints (for external services):**

```yaml
# Service without selector
apiVersion: v1
kind: Service
metadata:
  name: external-service
spec:
  ports:
  - port: 80
---
# Manual endpoints
apiVersion: v1
kind: Endpoints
metadata:
  name: external-service  # Must match service name
subsets:
- addresses:
  - ip: 192.168.1.100
  - ip: 192.168.1.101
  ports:
  - port: 80
```

## Service Networking Internals

### How Services Work

1. **Service Created** → Assigned ClusterIP (virtual IP)
2. **kube-proxy** → Configures iptables/IPVS rules on each node
3. **Traffic to ClusterIP** → Intercepted by iptables
4. **Load Balanced** → Distributed to backend pods
5. **Pod Response** → Returned to client

### kube-proxy Modes

```bash
# Check kube-proxy mode
kubectl logs -n kube-system kube-proxy-<pod> | grep "mode"

# Modes:
# - iptables (default)
# - ipvs (better performance)
# - userspace (legacy)
```

## Service Types Comparison

| Type | Accessibility | Use Case | External IP |
|------|--------------|----------|-------------|
| ClusterIP | Internal only | Inter-service communication | No |
| NodePort | External via node IP:port | Development/Testing | No |
| LoadBalancer | External via LB | Production | Yes |
| ExternalName | DNS mapping | External service reference | N/A |

## Troubleshooting Services

### Common Issues

#### 1. Service Not Accessible

```bash
# Check service exists
kubectl get svc backend-service

# Check endpoints
kubectl get endpoints backend-service
# If no endpoints, selector might not match pods

# Check pod labels
kubectl get pods --show-labels
kubectl get pods -l app=backend

# Test DNS resolution
kubectl run debug --image=busybox -it --rm -- nslookup backend-service
```

#### 2. No Endpoints

```bash
# Check if pods are ready
kubectl get pods -l app=backend

# Check pod labels match service selector
kubectl describe svc backend-service  # Check Selector
kubectl get pods --show-labels        # Check pod labels

# Check readiness probes
kubectl describe pod <pod-name>
```

#### 3. Connection Refused

```bash
# Check targetPort matches container port
kubectl describe svc backend-service
kubectl describe pod <pod-name>

# Check if container is listening on correct port
kubectl exec <pod-name> -- netstat -tlnp
```

### Debugging Commands

```bash
# Test service connectivity
kubectl run debug --image=nicolaka/netshoot -it --rm -- bash
# Inside container:
curl http://service-name
nslookup service-name

# Check service details
kubectl describe svc <service-name>

# Check endpoints
kubectl get endpoints <service-name>

# Check kube-proxy logs
kubectl logs -n kube-system -l k8s-app=kube-proxy

# Port forward for testing
kubectl port-forward svc/backend-service 8080:80
```

## Best Practices

1. **Use ClusterIP for internal services**: Default and most common
2. **Use LoadBalancer for external access**: In production cloud environments
3. **Avoid NodePort in production**: Use Ingress or LoadBalancer instead
4. **Name your ports**: Makes configuration clearer
5. **Use readiness probes**: Ensures only healthy pods receive traffic
6. **Set appropriate selectors**: Match pod labels correctly
7. **Use DNS for discovery**: Instead of environment variables
8. **Label services properly**: For organization and filtering
9. **Monitor service endpoints**: Ensure pods are being selected
10. **Use Ingress for HTTP(S)**: Instead of multiple LoadBalancers

## Quick Reference

```bash
# Create service
kubectl expose deployment nginx --port=80
kubectl create service clusterip my-svc --tcp=80:8080

# List services
kubectl get svc

# Describe service
kubectl describe svc <name>

# Get endpoints
kubectl get endpoints <name>

# Test service
kubectl run debug --image=busybox -it --rm -- wget -O- http://service-name

# Port forward
kubectl port-forward svc/my-service 8080:80

# Delete service
kubectl delete svc <name>

# Edit service
kubectl edit svc <name>
```

## Next Steps

- Learn about Ingress for HTTP routing
- Understand Network Policies for security
- Explore Service Mesh (Istio, Linkerd)
- Study DNS configuration in Kubernetes
- Practice different service types
