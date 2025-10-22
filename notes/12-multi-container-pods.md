# Multi-Container Pods

## Overview

A Pod can contain multiple containers that work together as a single unit. These containers share the same network namespace, storage volumes, and lifecycle.

**Shared Resources:**
- Network namespace (same IP, localhost communication)
- IPC namespace (inter-process communication)
- Volumes (shared storage)
- Lifecycle (start/stop together)

**Each Container Has:**
- Own filesystem
- Own process space
- Own resource limits

## Why Multi-Container Pods?

**Use Cases:**
1. **Sidecar Pattern**: Helper container alongside main application
2. **Adapter Pattern**: Transform/adapt output of main container
3. **Ambassador Pattern**: Proxy connections from main container
4. **Init Containers**: Setup tasks before main container starts

## Basic Multi-Container Pod

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: multi-container-pod
spec:
  containers:
  # Main application
  - name: webapp
    image: webapp:1.0
    ports:
    - containerPort: 8080

  # Sidecar container
  - name: log-agent
    image: fluentd:latest
    volumeMounts:
    - name: logs
      mountPath: /var/log

  volumes:
  - name: logs
    emptyDir: {}
```

## Container Communication

### 1. Via Localhost

Containers in the same pod can communicate via `localhost`:

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: app-with-cache
spec:
  containers:
  # Main application
  - name: webapp
    image: webapp:1.0
    env:
    - name: REDIS_HOST
      value: "localhost"  # Redis on same pod
    - name: REDIS_PORT
      value: "6379"
    ports:
    - containerPort: 8080

  # Redis cache (sidecar)
  - name: redis
    image: redis:6
    ports:
    - containerPort: 6379
```

### 2. Via Shared Volumes

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: shared-volume-pod
spec:
  containers:
  # Writer container
  - name: writer
    image: busybox
    command: ["sh", "-c", "while true; do echo $(date) >> /data/log.txt; sleep 5; done"]
    volumeMounts:
    - name: shared-data
      mountPath: /data

  # Reader container
  - name: reader
    image: busybox
    command: ["sh", "-c", "tail -f /data/log.txt"]
    volumeMounts:
    - name: shared-data
      mountPath: /data

  volumes:
  - name: shared-data
    emptyDir: {}
```

### 3. Via Inter-Process Communication (IPC)

Containers can share IPC namespace for shared memory:

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: ipc-pod
spec:
  shareProcessNamespace: true  # Optional: share process namespace
  containers:
  - name: container1
    image: app1:1.0
  - name: container2
    image: app2:1.0
```

## Design Patterns

### 1. Sidecar Pattern

A helper container that extends or enhances the main container.

**Common Sidecars:**
- Log collectors
- Monitoring agents
- Data synchronizers
- Security scanners

**Example: Log Collection Sidecar**

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: webapp-with-logging
spec:
  containers:
  # Main application
  - name: webapp
    image: nginx
    ports:
    - containerPort: 80
    volumeMounts:
    - name: logs
      mountPath: /var/log/nginx

  # Logging sidecar
  - name: log-shipper
    image: fluent/fluent-bit:latest
    volumeMounts:
    - name: logs
      mountPath: /var/log/nginx
    - name: config
      mountPath: /fluent-bit/etc/

  volumes:
  - name: logs
    emptyDir: {}
  - name: config
    configMap:
      name: fluent-bit-config
```

**Example: Metrics Collection Sidecar**

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: app-with-metrics
spec:
  containers:
  # Main application
  - name: webapp
    image: webapp:1.0
    ports:
    - containerPort: 8080

  # Prometheus exporter sidecar
  - name: metrics-exporter
    image: prom/nginx-exporter:latest
    ports:
    - containerPort: 9113
    args:
    - "-nginx.scrape-uri=http://localhost:8080/metrics"
```

### 2. Adapter Pattern

Standardizes or transforms the output of the main container.

**Example: Log Format Adapter**

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: app-with-adapter
spec:
  containers:
  # Main application (writes logs in custom format)
  - name: webapp
    image: legacy-app:1.0
    volumeMounts:
    - name: logs
      mountPath: /var/log/app

  # Adapter: converts logs to standard format
  - name: log-adapter
    image: log-converter:1.0
    volumeMounts:
    - name: logs
      mountPath: /var/log/app
    - name: formatted-logs
      mountPath: /var/log/formatted
    command: ["sh", "-c", "while true; do convert-logs /var/log/app /var/log/formatted; sleep 10; done"]

  volumes:
  - name: logs
    emptyDir: {}
  - name: formatted-logs
    emptyDir: {}
```

**Example: Protocol Adapter**

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: database-with-adapter
spec:
  containers:
  # Legacy database
  - name: database
    image: legacy-db:1.0
    ports:
    - containerPort: 5000

  # Adapter: exposes modern REST API
  - name: rest-adapter
    image: db-rest-adapter:1.0
    ports:
    - containerPort: 8080
    env:
    - name: DB_HOST
      value: "localhost"
    - name: DB_PORT
      value: "5000"
```

### 3. Ambassador Pattern

A proxy container that represents the main container to the outside world.

**Example: Database Ambassador**

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: app-with-ambassador
spec:
  containers:
  # Main application
  - name: webapp
    image: webapp:1.0
    env:
    - name: DB_HOST
      value: "localhost"  # Connect to ambassador
    - name: DB_PORT
      value: "5432"

  # Ambassador: proxies to actual database
  - name: db-ambassador
    image: haproxy:latest
    ports:
    - containerPort: 5432
    volumeMounts:
    - name: config
      mountPath: /usr/local/etc/haproxy
    # HAProxy config points to actual database cluster

  volumes:
  - name: config
    configMap:
      name: haproxy-config
```

**Example: API Gateway Ambassador**

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: app-with-gateway
spec:
  containers:
  # Main application
  - name: webapp
    image: webapp:1.0
    ports:
    - containerPort: 8080

  # Ambassador: handles authentication, rate limiting
  - name: api-gateway
    image: nginx:latest
    ports:
    - containerPort: 80
    volumeMounts:
    - name: nginx-config
      mountPath: /etc/nginx/conf.d
    # Proxies to localhost:8080 with added security

  volumes:
  - name: nginx-config
    configMap:
      name: nginx-gateway-config
```

## Init Containers

Special containers that run before main containers start. Used for initialization tasks.

**Characteristics:**
- Run to completion before main containers start
- Run sequentially (one after another)
- If init container fails, pod is restarted
- Don't support lifecycle, readiness, or liveness probes

### Basic Init Container

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: init-demo
spec:
  # Init containers
  initContainers:
  - name: init-setup
    image: busybox
    command: ["sh", "-c", "echo 'Initializing...' && sleep 5"]

  # Main containers
  containers:
  - name: webapp
    image: nginx
```

### Practical Init Container Examples

#### 1. Wait for Service to be Ready

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: webapp
spec:
  initContainers:
  # Wait for database service
  - name: wait-for-db
    image: busybox:latest
    command: ["sh", "-c", "until nslookup database-service; do echo waiting for database; sleep 2; done"]

  # Wait for cache service
  - name: wait-for-cache
    image: busybox:latest
    command: ["sh", "-c", "until nslookup redis-service; do echo waiting for cache; sleep 2; done"]

  containers:
  - name: webapp
    image: webapp:1.0
```

#### 2. Clone Git Repository

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: git-sync-pod
spec:
  initContainers:
  - name: git-clone
    image: alpine/git:latest
    command:
    - git
    - clone
    - https://github.com/user/repo.git
    - /data
    volumeMounts:
    - name: git-repo
      mountPath: /data

  containers:
  - name: webapp
    image: nginx
    volumeMounts:
    - name: git-repo
      mountPath: /usr/share/nginx/html

  volumes:
  - name: git-repo
    emptyDir: {}
```

#### 3. Database Migration

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: app-with-migration
spec:
  initContainers:
  - name: db-migration
    image: migrate/migrate:latest
    command:
    - migrate
    - "-path=/migrations"
    - "-database=$(DATABASE_URL)"
    - "up"
    env:
    - name: DATABASE_URL
      valueFrom:
        secretKeyRef:
          name: db-secret
          key: url
    volumeMounts:
    - name: migrations
      mountPath: /migrations

  containers:
  - name: webapp
    image: webapp:1.0

  volumes:
  - name: migrations
    configMap:
      name: db-migrations
```

#### 4. Download Configuration

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: config-downloader
spec:
  initContainers:
  - name: download-config
    image: curlimages/curl:latest
    command:
    - sh
    - -c
    - |
      curl -o /config/app.conf https://config-server/app.conf
      curl -o /config/secrets.conf https://vault/secrets.conf
    volumeMounts:
    - name: config
      mountPath: /config

  containers:
  - name: webapp
    image: webapp:1.0
    volumeMounts:
    - name: config
      mountPath: /etc/app

  volumes:
  - name: config
    emptyDir: {}
```

#### 5. Set Permissions

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: permission-setup
spec:
  initContainers:
  - name: setup-permissions
    image: busybox
    command:
    - sh
    - -c
    - |
      mkdir -p /data/logs /data/uploads
      chmod 777 /data/logs
      chmod 755 /data/uploads
      chown -R 1000:1000 /data
    volumeMounts:
    - name: data
      mountPath: /data

  containers:
  - name: webapp
    image: webapp:1.0
    securityContext:
      runAsUser: 1000
    volumeMounts:
    - name: data
      mountPath: /data

  volumes:
  - name: data
    emptyDir: {}
```

## Complete Multi-Container Example

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: production-app
spec:
  replicas: 3
  selector:
    matchLabels:
      app: production-app
  template:
    metadata:
      labels:
        app: production-app
    spec:
      # Init containers
      initContainers:
      # Wait for dependencies
      - name: wait-for-services
        image: busybox:latest
        command:
        - sh
        - -c
        - |
          until nslookup database-service && nslookup cache-service; do
            echo "Waiting for services..."
            sleep 2
          done
          echo "Services are ready!"

      # Download configuration
      - name: config-loader
        image: curlimages/curl:latest
        command: ["sh", "-c", "curl -o /config/app.yaml https://config-server/app.yaml"]
        volumeMounts:
        - name: config
          mountPath: /config

      # Main containers
      containers:
      # Primary application
      - name: webapp
        image: webapp:1.0
        ports:
        - containerPort: 8080
        env:
        - name: CACHE_HOST
          value: "localhost"
        - name: CACHE_PORT
          value: "6379"
        volumeMounts:
        - name: config
          mountPath: /etc/app
        - name: logs
          mountPath: /var/log/app
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /ready
            port: 8080
          initialDelaySeconds: 5
          periodSeconds: 5

      # Sidecar: Redis cache
      - name: redis
        image: redis:6-alpine
        ports:
        - containerPort: 6379
        resources:
          requests:
            memory: "128Mi"
            cpu: "100m"
          limits:
            memory: "256Mi"
            cpu: "200m"

      # Sidecar: Log collector
      - name: log-collector
        image: fluent/fluent-bit:latest
        volumeMounts:
        - name: logs
          mountPath: /var/log/app
        - name: fluent-config
          mountPath: /fluent-bit/etc/
        resources:
          requests:
            memory: "64Mi"
            cpu: "50m"
          limits:
            memory: "128Mi"
            cpu: "100m"

      # Sidecar: Metrics exporter
      - name: metrics-exporter
        image: prom/statsd-exporter:latest
        ports:
        - containerPort: 9102
        resources:
          requests:
            memory: "32Mi"
            cpu: "25m"
          limits:
            memory: "64Mi"
            cpu: "50m"

      # Volumes
      volumes:
      - name: config
        emptyDir: {}
      - name: logs
        emptyDir: {}
      - name: fluent-config
        configMap:
          name: fluent-bit-config
```

## Managing Multi-Container Pods

### View Container Status

```bash
# List all containers in pod
kubectl get pod multi-pod -o jsonpath='{.spec.containers[*].name}'

# Check status of each container
kubectl get pod multi-pod -o jsonpath='{range .status.containerStatuses[*]}{.name}{"\t"}{.ready}{"\n"}{end}'
```

### Logs from Specific Container

```bash
# View logs from specific container
kubectl logs multi-pod -c webapp
kubectl logs multi-pod -c log-collector

# Follow logs
kubectl logs -f multi-pod -c webapp

# Previous container logs
kubectl logs multi-pod -c webapp --previous

# Logs from all containers
kubectl logs multi-pod --all-containers=true
```

### Execute Commands in Specific Container

```bash
# Execute in specific container
kubectl exec multi-pod -c webapp -- ls /
kubectl exec -it multi-pod -c redis -- redis-cli

# Interactive shell
kubectl exec -it multi-pod -c webapp -- /bin/bash
```

### Debugging Multi-Container Pods

```bash
# Describe pod (shows all containers)
kubectl describe pod multi-pod

# Get detailed container info
kubectl get pod multi-pod -o yaml

# Check init container status
kubectl get pod multi-pod -o jsonpath='{.status.initContainerStatuses}'

# View events
kubectl get events --field-selector involvedObject.name=multi-pod
```

## Best Practices

1. **Single Responsibility**: Each container should have one main purpose
2. **Resource Limits**: Set limits for each container individually
3. **Health Checks**: Add probes to main containers
4. **Shared Volumes**: Use emptyDir for inter-container communication
5. **Logging**: Use sidecars for centralized log collection
6. **Init Containers**: Use for one-time setup tasks
7. **Localhost Communication**: Leverage localhost for fast IPC
8. **Container Ordering**: Remember init containers run sequentially
9. **Failure Handling**: One container failure restarts entire pod
10. **Monitoring**: Monitor all containers separately

## Common Pitfalls

1. **Too Many Containers**: Don't overload pods with containers
2. **Resource Starvation**: Set limits for all containers
3. **Tight Coupling**: Containers should be loosely coupled
4. **Log Duplication**: Avoid multiple log collectors
5. **Port Conflicts**: Containers share network, can't use same port
6. **Volume Permissions**: Ensure containers have proper access
7. **Init Container Failures**: Will prevent pod from starting
8. **Missing Dependencies**: Use init containers to wait for services

## Quick Reference

```bash
# View container names
kubectl get pod <pod> -o jsonpath='{.spec.containers[*].name}'

# Logs from specific container
kubectl logs <pod> -c <container>

# Execute in specific container
kubectl exec <pod> -c <container> -- <command>

# Describe pod with all containers
kubectl describe pod <pod>

# Check init container status
kubectl get pod <pod> -o jsonpath='{.status.initContainerStatuses}'

# Debug failing init container
kubectl logs <pod> -c <init-container>
```

## Next Steps

- Learn about StatefulSets for stateful multi-container apps
- Explore service mesh (Istio, Linkerd) for advanced sidecar patterns
- Study DaemonSets for node-level sidecars
- Understand Pod disruption budgets
- Practice with real-world sidecar patterns
