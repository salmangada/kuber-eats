# YAML Basics for Kubernetes

## What is YAML?

YAML (YAML Ain't Markup Language) is a human-readable data serialization format used for configuration files. Kubernetes uses YAML for defining resources.

## YAML Syntax Rules

### 1. Indentation
- Use **spaces** (NOT tabs)
- Typically 2 spaces per indentation level
- Indentation defines structure

```yaml
parent:
  child:
    grandchild: value
```

### 2. Key-Value Pairs

```yaml
name: nginx
version: 1.21
port: 80
```

### 3. Lists/Arrays

```yaml
# Method 1: Dash notation
fruits:
  - apple
  - banana
  - orange

# Method 2: Inline
fruits: [apple, banana, orange]
```

### 4. Dictionaries/Objects

```yaml
person:
  name: John
  age: 30
  city: NYC
```

### 5. Multi-line Strings

```yaml
# Preserve newlines (|)
description: |
  This is line 1
  This is line 2
  This is line 3

# Fold newlines (>)
description: >
  This is a long
  sentence that will
  be folded into one line
```

### 6. Comments

```yaml
# This is a comment
name: nginx  # Inline comment
```

## Kubernetes YAML Structure

Every Kubernetes resource has 4 required top-level fields:

```yaml
apiVersion: v1           # API version
kind: Pod                # Resource type
metadata:                # Metadata about the object
  name: my-pod
  labels:
    app: myapp
spec:                    # Desired state
  containers:
  - name: nginx
    image: nginx
```

### 1. apiVersion

Specifies which version of the Kubernetes API to use.

```yaml
# Common API versions:
apiVersion: v1                    # Core resources: Pod, Service, ConfigMap, Secret
apiVersion: apps/v1               # Deployment, ReplicaSet, StatefulSet, DaemonSet
apiVersion: batch/v1              # Job, CronJob
apiVersion: networking.k8s.io/v1  # Ingress, NetworkPolicy
apiVersion: rbac.authorization.k8s.io/v1  # Role, RoleBinding
```

```bash
# Find API version for a resource
kubectl api-resources | grep deployment
kubectl explain deployment
```

### 2. kind

The type of Kubernetes object to create.

```yaml
# Common kinds:
kind: Pod
kind: Service
kind: Deployment
kind: ConfigMap
kind: Secret
kind: Namespace
kind: Ingress
```

```bash
# List all available kinds
kubectl api-resources
```

### 3. metadata

Data that helps identify the object.

```yaml
metadata:
  name: my-app              # Required: Object name
  namespace: default        # Optional: Namespace (default if omitted)
  labels:                   # Optional: Key-value pairs for organization
    app: myapp
    environment: production
    tier: frontend
  annotations:              # Optional: Non-identifying metadata
    description: "My application"
    version: "1.0.0"
```

**Labels vs Annotations:**
- **Labels**: Used for selection and grouping (queries, selectors)
- **Annotations**: Additional metadata, not used for selection

### 4. spec

The desired state of the object (varies by resource type).

```yaml
spec:
  containers:
  - name: nginx
    image: nginx:1.21
    ports:
    - containerPort: 80
```

## Complete Pod YAML Example

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: nginx-pod
  namespace: default
  labels:
    app: nginx
    env: production
  annotations:
    description: "Nginx web server"
spec:
  containers:
  - name: nginx
    image: nginx:1.21
    ports:
    - containerPort: 80
      protocol: TCP
    env:
    - name: ENVIRONMENT
      value: production
    resources:
      requests:
        memory: "64Mi"
        cpu: "250m"
      limits:
        memory: "128Mi"
        cpu: "500m"
    volumeMounts:
    - name: html
      mountPath: /usr/share/nginx/html
  volumes:
  - name: html
    emptyDir: {}
```

## Working with YAML Files

### Creating Resources

```bash
# Create resource
kubectl create -f pod.yaml

# Apply resource (create or update)
kubectl apply -f pod.yaml

# Create from multiple files
kubectl apply -f file1.yaml -f file2.yaml

# Create from directory
kubectl apply -f ./configs/

# Create from URL
kubectl apply -f https://example.com/pod.yaml
```

### Generating YAML

```bash
# Generate YAML without creating
kubectl run nginx --image=nginx --dry-run=client -o yaml

# Generate and save to file
kubectl run nginx --image=nginx --dry-run=client -o yaml > pod.yaml

# Generate from existing resource
kubectl get pod nginx -o yaml

# Generate deployment YAML
kubectl create deployment nginx --image=nginx --dry-run=client -o yaml

# Generate service YAML
kubectl create service clusterip my-service --tcp=80:80 --dry-run=client -o yaml
```

### Editing Resources

```bash
# Edit in default editor
kubectl edit pod nginx-pod

# Get YAML for editing
kubectl get pod nginx-pod -o yaml > pod.yaml
# Edit the file, then apply
kubectl apply -f pod.yaml

# Replace resource (delete and create)
kubectl replace -f pod.yaml --force
```

### Validating YAML

```bash
# Dry run to validate
kubectl apply -f pod.yaml --dry-run=client

# Server-side validation
kubectl apply -f pod.yaml --dry-run=server

# Validate syntax (using external tools)
# Install yamllint: pip install yamllint
yamllint pod.yaml
```

## Common YAML Patterns

### Multiple Resources in One File

Use `---` to separate resources:

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: pod1
spec:
  containers:
  - name: nginx
    image: nginx
---
apiVersion: v1
kind: Pod
metadata:
  name: pod2
spec:
  containers:
  - name: httpd
    image: httpd
```

### Using Variables (Not native YAML, but common pattern)

Use templating tools like Kustomize or Helm:

```yaml
# Using Kustomize
apiVersion: v1
kind: Pod
metadata:
  name: $(POD_NAME)
spec:
  containers:
  - name: nginx
    image: nginx:$(IMAGE_TAG)
```

### ConfigMap as YAML

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: app-config
data:
  database_url: "postgres://db:5432"
  log_level: "info"
  config.json: |
    {
      "name": "myapp",
      "version": "1.0"
    }
```

## Understanding kubectl explain

Use `kubectl explain` to understand YAML structure:

```bash
# Explain a resource
kubectl explain pod

# Explain nested fields
kubectl explain pod.spec
kubectl explain pod.spec.containers
kubectl explain pod.spec.containers.resources

# Show all fields recursively
kubectl explain pod --recursive

# Show specific field
kubectl explain pod.spec.containers.livenessProbe
```

## Common YAML Mistakes

### 1. Using Tabs Instead of Spaces

```yaml
# WRONG
spec:
	containers:  # Tab used
	- name: nginx

# CORRECT
spec:
  containers:  # Spaces used
  - name: nginx
```

### 2. Incorrect Indentation

```yaml
# WRONG
spec:
  containers:
- name: nginx  # Should be indented

# CORRECT
spec:
  containers:
  - name: nginx
```

### 3. Missing Dash for List Items

```yaml
# WRONG
spec:
  containers:
    name: nginx  # Missing dash

# CORRECT
spec:
  containers:
  - name: nginx
```

### 4. Quotes in Strings

```yaml
# Usually not needed
name: my-app

# Needed for special characters
name: "my:app"
name: 'my-app: version 1.0'

# Needed for numbers as strings
version: "1.0"
port: "80"
```

## YAML Best Practices

1. **Use 2 spaces** for indentation
2. **Use meaningful names** for resources
3. **Add labels** for organization
4. **Use comments** to document complex configurations
5. **Validate YAML** before applying
6. **Version control** your YAML files
7. **Use kubectl explain** to understand fields
8. **Test with --dry-run** before applying
9. **Keep files small** and focused
10. **Use separate files** for different environments

## Quick Reference

```bash
# Create from YAML
kubectl apply -f file.yaml

# Generate YAML
kubectl run nginx --image=nginx --dry-run=client -o yaml

# Get YAML of existing resource
kubectl get pod nginx -o yaml

# Validate YAML
kubectl apply -f file.yaml --dry-run=client

# Explain structure
kubectl explain pod.spec

# Edit resource
kubectl edit pod nginx

# Delete from YAML
kubectl delete -f file.yaml
```

## Next Steps

- Practice writing YAML for different resources
- Learn about ReplicaSets and how to define them
- Understand Deployments and their YAML structure
- Explore advanced configurations with ConfigMaps and Secrets
