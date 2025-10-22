# Kubernetes Networking

## Overview

Kubernetes networking enables communication between:
- Pods to Pods (within/across nodes)
- Pods to Services
- External clients to Services
- Containers within a Pod

**Kubernetes Networking Model Requirements:**
1. All pods can communicate with each other without NAT
2. All nodes can communicate with all pods without NAT
3. Pod sees itself with the same IP as others see it

## Networking Components

### 1. Pod Networking

Each pod gets its own IP address. Containers in a pod share the same network namespace.

```bash
# View pod IPs
kubectl get pods -o wide

# Check pod network
kubectl exec <pod> -- ip addr
kubectl exec <pod> -- ifconfig
```

**Pod-to-Pod Communication:**

```yaml
# Pod 1
apiVersion: v1
kind: Pod
metadata:
  name: pod1
spec:
  containers:
  - name: nginx
    image: nginx
    ports:
    - containerPort: 80
---
# Pod 2 - can access Pod 1
apiVersion: v1
kind: Pod
metadata:
  name: pod2
spec:
  containers:
  - name: busybox
    image: busybox
    command: ["sh", "-c", "wget -O- http://<pod1-ip>"]
```

### 2. Service Networking

Services provide stable endpoints for pods.

**Service Types:**
- **ClusterIP**: Internal cluster communication
- **NodePort**: External access via node IP
- **LoadBalancer**: Cloud load balancer
- **ExternalName**: DNS alias

```yaml
apiVersion: v1
kind: Service
metadata:
  name: webapp-service
spec:
  type: ClusterIP
  selector:
    app: webapp
  ports:
  - port: 80
    targetPort: 8080
```

**Service Discovery via DNS:**

```bash
# From any pod, access service by name
curl http://webapp-service
curl http://webapp-service.default.svc.cluster.local

# Cross-namespace
curl http://service-name.namespace-name.svc.cluster.local
```

### 3. DNS

Kubernetes uses CoreDNS for service discovery.

**DNS Records:**

```bash
# Service DNS format
<service-name>.<namespace>.svc.cluster.local

# Pod DNS format
<pod-ip-with-dashes>.<namespace>.pod.cluster.local

# Example
webapp-service.default.svc.cluster.local
10-244-1-5.default.pod.cluster.local
```

**Testing DNS:**

```bash
# Create debug pod
kubectl run debug --image=busybox -it --rm -- sh

# Inside pod, test DNS
nslookup kubernetes.default
nslookup webapp-service
nslookup webapp-service.default.svc.cluster.local

# Check nameserver
cat /etc/resolv.conf
```

### 4. Network Policies

Control traffic flow between pods.

**Default Behavior:**
- All pods can communicate with each other
- No restrictions by default

**With Network Policies:**
- Define allowed ingress/egress traffic
- Deny by default, allow explicitly

```yaml
# Deny all ingress traffic to pods
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: deny-all
  namespace: default
spec:
  podSelector: {}
  policyTypes:
  - Ingress
```

```yaml
# Allow ingress from specific pods
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: allow-frontend
  namespace: default
spec:
  podSelector:
    matchLabels:
      app: backend
  policyTypes:
  - Ingress
  ingress:
  - from:
    - podSelector:
        matchLabels:
          app: frontend
    ports:
    - protocol: TCP
      port: 8080
```

```yaml
# Allow ingress from specific namespace
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: allow-from-namespace
  namespace: production
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
          environment: production
```

```yaml
# Control egress (outbound) traffic
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: allow-dns-and-api
  namespace: default
spec:
  podSelector:
    matchLabels:
      app: webapp
  policyTypes:
  - Egress
  egress:
  # Allow DNS
  - to:
    - namespaceSelector:
        matchLabels:
          name: kube-system
    ports:
    - protocol: UDP
      port: 53
  # Allow API server
  - to:
    - podSelector:
        matchLabels:
          app: api
    ports:
    - protocol: TCP
      port: 443
```

**Complete Network Policy Example:**

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: webapp-netpol
  namespace: default
spec:
  podSelector:
    matchLabels:
      app: webapp
  policyTypes:
  - Ingress
  - Egress

  # Ingress rules
  ingress:
  # Allow from frontend on port 8080
  - from:
    - podSelector:
        matchLabels:
          app: frontend
    ports:
    - protocol: TCP
      port: 8080

  # Allow from monitoring namespace
  - from:
    - namespaceSelector:
        matchLabels:
          name: monitoring

  # Egress rules
  egress:
  # Allow DNS
  - to:
    - namespaceSelector: {}
      podSelector:
        matchLabels:
          k8s-app: kube-dns
    ports:
    - protocol: UDP
      port: 53

  # Allow database
  - to:
    - podSelector:
        matchLabels:
          app: database
    ports:
    - protocol: TCP
      port: 5432

  # Allow external HTTPS
  - to:
    - podSelector: {}
    ports:
    - protocol: TCP
      port: 443
```

## Ingress

Manages external HTTP/HTTPS access to services.

**Benefits:**
- Single entry point
- Path-based routing
- Host-based routing
- SSL/TLS termination
- Load balancing

### Ingress Controller

Must install an Ingress controller (e.g., NGINX, Traefik, HAProxy):

```bash
# Install NGINX Ingress Controller
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.8.1/deploy/static/provider/cloud/deploy.yaml

# Verify
kubectl get pods -n ingress-nginx
```

### Basic Ingress

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: simple-ingress
spec:
  rules:
  - host: example.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: webapp-service
            port:
              number: 80
```

### Path-Based Routing

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: path-based-ingress
spec:
  rules:
  - host: example.com
    http:
      paths:
      # Route /api to api-service
      - path: /api
        pathType: Prefix
        backend:
          service:
            name: api-service
            port:
              number: 8080
      # Route /web to frontend-service
      - path: /web
        pathType: Prefix
        backend:
          service:
            name: frontend-service
            port:
              number: 80
```

### Host-Based Routing

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: host-based-ingress
spec:
  rules:
  # api.example.com → api-service
  - host: api.example.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: api-service
            port:
              number: 8080

  # web.example.com → frontend-service
  - host: web.example.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: frontend-service
            port:
              number: 80
```

### TLS/SSL Termination

```yaml
# Create TLS secret
apiVersion: v1
kind: Secret
metadata:
  name: tls-secret
type: kubernetes.io/tls
data:
  tls.crt: <base64-encoded-cert>
  tls.key: <base64-encoded-key>
---
# Ingress with TLS
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: tls-ingress
spec:
  tls:
  - hosts:
    - example.com
    - www.example.com
    secretName: tls-secret
  rules:
  - host: example.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: webapp-service
            port:
              number: 80
```

### Ingress with Annotations

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: annotated-ingress
  annotations:
    # NGINX-specific annotations
    nginx.ingress.kubernetes.io/rewrite-target: /
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
    nginx.ingress.kubernetes.io/rate-limit: "10"
    # CORS
    nginx.ingress.kubernetes.io/enable-cors: "true"
    nginx.ingress.kubernetes.io/cors-allow-origin: "*"
spec:
  rules:
  - host: example.com
    http:
      paths:
      - path: /api
        pathType: Prefix
        backend:
          service:
            name: api-service
            port:
              number: 8080
```

## Port Forwarding

Access pods/services locally for debugging.

```bash
# Forward pod port to localhost
kubectl port-forward pod/nginx 8080:80
# Access via localhost:8080

# Forward service port
kubectl port-forward service/webapp 8080:80

# Forward deployment port
kubectl port-forward deployment/webapp 8080:80

# Forward to specific local IP
kubectl port-forward --address 0.0.0.0 pod/nginx 8080:80

# Forward multiple ports
kubectl port-forward pod/nginx 8080:80 8443:443
```

## CNI (Container Network Interface)

Kubernetes uses CNI plugins for pod networking.

**Popular CNI Plugins:**
- **Calico**: Network policies, BGP routing
- **Flannel**: Simple overlay network
- **Weave**: Mesh network
- **Cilium**: eBPF-based, advanced features
- **Canal**: Flannel + Calico

```bash
# Check CNI plugin
kubectl get pods -n kube-system | grep -E 'calico|flannel|weave|cilium'

# View CNI config
ls /etc/cni/net.d/
```

## Network Troubleshooting

### Debug Tools Pod

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: netshoot
spec:
  containers:
  - name: netshoot
    image: nicolaka/netshoot
    command: ["sleep", "3600"]
```

```bash
kubectl apply -f netshoot.yaml
kubectl exec -it netshoot -- bash

# Inside pod, use tools:
# ping, curl, wget, nslookup, dig, traceroute, tcpdump, etc.
```

### Common Debugging Commands

```bash
# Test pod connectivity
kubectl run debug --image=busybox -it --rm -- wget -O- http://service-name

# DNS lookup
kubectl run debug --image=busybox -it --rm -- nslookup service-name

# Check service endpoints
kubectl get endpoints service-name

# Describe service
kubectl describe service service-name

# Check network policies
kubectl get networkpolicies
kubectl describe networkpolicy <name>

# View pod network
kubectl exec <pod> -- ip addr
kubectl exec <pod> -- netstat -tuln

# Test specific port
kubectl run debug --image=busybox -it --rm -- nc -vz service-name 80

# Check CoreDNS
kubectl get pods -n kube-system -l k8s-app=kube-dns
kubectl logs -n kube-system -l k8s-app=kube-dns
```

### Network Policy Testing

```bash
# Create test pods
kubectl run frontend --image=nginx --labels=app=frontend
kubectl run backend --image=nginx --labels=app=backend

# Test connectivity before policy
kubectl exec frontend -- wget -O- --timeout=2 backend

# Apply network policy
kubectl apply -f network-policy.yaml

# Test connectivity after policy
kubectl exec frontend -- wget -O- --timeout=2 backend
# Should succeed or fail based on policy

# Check if policy is applied
kubectl describe pod backend | grep -A 5 "Network Policy"
```

## Service Mesh

Advanced networking and observability (Istio, Linkerd, Consul).

**Features:**
- Traffic management
- Security (mTLS)
- Observability
- Load balancing
- Circuit breaking
- Retries and timeouts

**Example: Istio VirtualService**

```yaml
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: webapp
spec:
  hosts:
  - webapp-service
  http:
  - match:
    - headers:
        user-agent:
          regex: ".*Chrome.*"
    route:
    - destination:
        host: webapp-v2
  - route:
    - destination:
        host: webapp-v1
```

## Best Practices

1. **Use Services**: Don't connect to pods directly by IP
2. **Implement Network Policies**: Secure pod-to-pod communication
3. **Use Ingress**: Single entry point for HTTP traffic
4. **Enable DNS**: Essential for service discovery
5. **Label Services**: Organize and filter easily
6. **Monitor Network**: Track traffic patterns
7. **Use Service Mesh**: For complex microservices
8. **Test Policies**: Verify network policies work as expected
9. **Document Routes**: Keep ingress rules documented
10. **Secure Traffic**: Use TLS for sensitive data

## Common Issues

### 1. Pod Can't Reach Service

```bash
# Check service exists
kubectl get svc service-name

# Check endpoints
kubectl get endpoints service-name
# If no endpoints, selector might not match pods

# Verify pod labels
kubectl get pods --show-labels
```

### 2. DNS Not Working

```bash
# Check CoreDNS pods
kubectl get pods -n kube-system -l k8s-app=kube-dns

# Test DNS from pod
kubectl run debug --image=busybox -it --rm -- nslookup kubernetes.default

# Check DNS config in pod
kubectl exec <pod> -- cat /etc/resolv.conf
```

### 3. Network Policy Blocking Traffic

```bash
# List network policies
kubectl get networkpolicies

# Check policy details
kubectl describe networkpolicy <name>

# Verify pod labels match
kubectl get pods --show-labels

# Temporarily delete policy to test
kubectl delete networkpolicy <name>
```

### 4. Ingress Not Working

```bash
# Check ingress controller
kubectl get pods -n ingress-nginx

# Describe ingress
kubectl describe ingress <name>

# Check service backing the ingress
kubectl get svc <service-name>

# View ingress controller logs
kubectl logs -n ingress-nginx <controller-pod>
```

## Quick Reference

```bash
# View pod IPs
kubectl get pods -o wide

# Test service connectivity
kubectl run debug --image=busybox -it --rm -- wget -O- http://service-name

# DNS lookup
kubectl run debug --image=busybox -it --rm -- nslookup service-name

# Port forward
kubectl port-forward pod/nginx 8080:80

# View endpoints
kubectl get endpoints

# Network policies
kubectl get networkpolicies
kubectl describe networkpolicy <name>

# Ingress
kubectl get ingress
kubectl describe ingress <name>

# Debug pod
kubectl run netshoot --image=nicolaka/netshoot -it --rm -- bash
```

## Next Steps

- Learn about Service Mesh (Istio, Linkerd)
- Explore advanced Ingress features
- Study network policy enforcement
- Understand CNI plugins in depth
- Practice with multi-cluster networking
- Learn about IPv6 in Kubernetes
