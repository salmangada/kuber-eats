# ConfigMaps and Secrets

## What are ConfigMaps?

ConfigMaps store non-confidential configuration data as key-value pairs. They decouple configuration from container images, making applications more portable.

**Use Cases:**
- Application configuration
- Environment variables
- Command-line arguments
- Configuration files

## What are Secrets?

Secrets store sensitive information like passwords, tokens, and keys. They're similar to ConfigMaps but designed for confidential data.

**Use Cases:**
- Passwords and API keys
- TLS certificates
- SSH keys
- Docker registry credentials

**Important:** Secrets are base64 encoded, NOT encrypted by default. Use encryption at rest for better security.

## ConfigMaps

### Creating ConfigMaps

#### Method 1: From Literals

```bash
# Create from literal values
kubectl create configmap app-config \
  --from-literal=DB_HOST=mysql \
  --from-literal=DB_PORT=3306 \
  --from-literal=APP_MODE=production

# View the configmap
kubectl get configmap app-config -o yaml
```

#### Method 2: From File

```bash
# Create a config file
cat > app.properties <<EOF
database.host=mysql
database.port=3306
app.mode=production
EOF

# Create ConfigMap from file
kubectl create configmap app-config --from-file=app.properties

# Create with custom key name
kubectl create configmap app-config --from-file=config=app.properties
```

#### Method 3: From Directory

```bash
# Create directory with multiple files
mkdir config
echo "production" > config/environment
echo "mysql" > config/database
echo "3306" > config/port

# Create ConfigMap from directory
kubectl create configmap app-config --from-file=./config/
```

#### Method 4: From Env File

```bash
# Create .env file
cat > .env <<EOF
DB_HOST=mysql
DB_PORT=3306
APP_MODE=production
EOF

# Create ConfigMap from env file
kubectl create configmap app-config --from-env-file=.env
```

#### Method 5: From YAML

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: app-config
  namespace: default
data:
  # Simple key-value pairs
  DB_HOST: "mysql"
  DB_PORT: "3306"
  APP_MODE: "production"

  # Multi-line data
  app.properties: |
    database.host=mysql
    database.port=3306
    app.mode=production

  # JSON configuration
  config.json: |
    {
      "database": {
        "host": "mysql",
        "port": 3306
      },
      "app": {
        "mode": "production"
      }
    }
```

```bash
kubectl apply -f configmap.yaml
```

### Using ConfigMaps in Pods

#### Method 1: As Environment Variables

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: app-pod
spec:
  containers:
  - name: app
    image: myapp:1.0
    env:
    # Single environment variable from ConfigMap
    - name: DATABASE_HOST
      valueFrom:
        configMapKeyRef:
          name: app-config
          key: DB_HOST
    - name: DATABASE_PORT
      valueFrom:
        configMapKeyRef:
          name: app-config
          key: DB_PORT
```

#### Method 2: All Keys as Environment Variables

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: app-pod
spec:
  containers:
  - name: app
    image: myapp:1.0
    envFrom:
    - configMapRef:
        name: app-config
    # All keys from ConfigMap become environment variables
```

#### Method 3: As Volume (Files)

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: app-pod
spec:
  containers:
  - name: app
    image: myapp:1.0
    volumeMounts:
    - name: config-volume
      mountPath: /etc/config
  volumes:
  - name: config-volume
    configMap:
      name: app-config
```

Files will be created in `/etc/config/`:
- `/etc/config/DB_HOST`
- `/etc/config/DB_PORT`
- `/etc/config/app.properties`

#### Method 4: Specific Keys as Files

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: app-pod
spec:
  containers:
  - name: app
    image: myapp:1.0
    volumeMounts:
    - name: config-volume
      mountPath: /etc/config
  volumes:
  - name: config-volume
    configMap:
      name: app-config
      items:
      - key: app.properties
        path: application.properties
      # Only this key will be mounted as /etc/config/application.properties
```

### Managing ConfigMaps

```bash
# List ConfigMaps
kubectl get configmaps
kubectl get cm

# Describe ConfigMap
kubectl describe configmap app-config

# View ConfigMap data
kubectl get configmap app-config -o yaml

# Edit ConfigMap
kubectl edit configmap app-config

# Delete ConfigMap
kubectl delete configmap app-config

# Create from YAML
kubectl apply -f configmap.yaml
```

## Secrets

### Creating Secrets

#### Method 1: From Literals

```bash
# Create generic secret
kubectl create secret generic db-secret \
  --from-literal=username=admin \
  --from-literal=password=P@ssw0rd123

# View secret
kubectl get secret db-secret -o yaml
# Values are base64 encoded
```

#### Method 2: From Files

```bash
# Create files with secret data
echo -n 'admin' > username.txt
echo -n 'P@ssw0rd123' > password.txt

# Create secret from files
kubectl create secret generic db-secret \
  --from-file=username=username.txt \
  --from-file=password=password.txt

# Cleanup
rm username.txt password.txt
```

#### Method 3: From YAML

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: db-secret
type: Opaque
data:
  # Values must be base64 encoded
  username: YWRtaW4=              # admin
  password: UEBzc3cwcmQxMjM=      # P@ssw0rd123
```

```bash
# Encode values
echo -n 'admin' | base64
# YWRtaW4=

echo -n 'P@ssw0rd123' | base64
# UEBzc3cwcmQxMjM=

kubectl apply -f secret.yaml
```

#### Method 4: Using stringData (No encoding needed)

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: db-secret
type: Opaque
stringData:
  # Plain text values (will be encoded automatically)
  username: admin
  password: P@ssw0rd123
```

### Secret Types

#### 1. Opaque (Default)

Generic secrets for any data:

```bash
kubectl create secret generic my-secret \
  --from-literal=key=value
```

#### 2. Docker Registry Secret

For pulling images from private registries:

```bash
kubectl create secret docker-registry regcred \
  --docker-server=myregistry.com \
  --docker-username=admin \
  --docker-password=secret \
  --docker-email=admin@example.com
```

```yaml
# Use in pod
apiVersion: v1
kind: Pod
metadata:
  name: private-pod
spec:
  containers:
  - name: app
    image: myregistry.com/myapp:1.0
  imagePullSecrets:
  - name: regcred
```

#### 3. TLS Secret

For TLS certificates:

```bash
# Create TLS secret
kubectl create secret tls tls-secret \
  --cert=path/to/tls.crt \
  --key=path/to/tls.key
```

```yaml
# Use in Ingress
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: tls-ingress
spec:
  tls:
  - hosts:
    - example.com
    secretName: tls-secret
  rules:
  - host: example.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: web-service
            port:
              number: 80
```

#### 4. SSH Auth Secret

```bash
kubectl create secret generic ssh-secret \
  --from-file=ssh-privatekey=/path/to/.ssh/id_rsa
```

#### 5. Basic Auth Secret

```bash
kubectl create secret generic basic-auth \
  --from-literal=username=admin \
  --from-literal=password=secret
```

### Using Secrets in Pods

#### Method 1: As Environment Variables

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: app-pod
spec:
  containers:
  - name: app
    image: myapp:1.0
    env:
    # Single environment variable from Secret
    - name: DB_USERNAME
      valueFrom:
        secretKeyRef:
          name: db-secret
          key: username
    - name: DB_PASSWORD
      valueFrom:
        secretKeyRef:
          name: db-secret
          key: password
```

#### Method 2: All Keys as Environment Variables

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: app-pod
spec:
  containers:
  - name: app
    image: myapp:1.0
    envFrom:
    - secretRef:
        name: db-secret
```

#### Method 3: As Volume (Files)

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: app-pod
spec:
  containers:
  - name: app
    image: myapp:1.0
    volumeMounts:
    - name: secret-volume
      mountPath: /etc/secrets
      readOnly: true
  volumes:
  - name: secret-volume
    secret:
      secretName: db-secret
```

Files created:
- `/etc/secrets/username` (decoded)
- `/etc/secrets/password` (decoded)

#### Method 4: Specific Keys as Files

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: app-pod
spec:
  containers:
  - name: app
    image: myapp:1.0
    volumeMounts:
    - name: secret-volume
      mountPath: /etc/secrets
      readOnly: true
  volumes:
  - name: secret-volume
    secret:
      secretName: db-secret
      items:
      - key: username
        path: db-username
      - key: password
        path: db-password
```

### Managing Secrets

```bash
# List secrets
kubectl get secrets

# Describe secret (doesn't show data)
kubectl describe secret db-secret

# View secret data (base64 encoded)
kubectl get secret db-secret -o yaml

# Decode secret value
kubectl get secret db-secret -o jsonpath='{.data.password}' | base64 --decode

# Edit secret
kubectl edit secret db-secret

# Delete secret
kubectl delete secret db-secret
```

## Complete Example: Application with ConfigMap and Secret

```yaml
# ConfigMap for non-sensitive config
apiVersion: v1
kind: ConfigMap
metadata:
  name: app-config
data:
  APP_ENV: "production"
  LOG_LEVEL: "info"
  DB_NAME: "myapp"
  app.yaml: |
    server:
      port: 8080
      timeout: 30s
---
# Secret for sensitive data
apiVersion: v1
kind: Secret
metadata:
  name: app-secret
type: Opaque
stringData:
  DB_USERNAME: "admin"
  DB_PASSWORD: "P@ssw0rd123"
  API_KEY: "abc123xyz789"
---
# Deployment using both
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
        # Environment variables from ConfigMap
        envFrom:
        - configMapRef:
            name: app-config
        # Environment variables from Secret
        env:
        - name: DB_USER
          valueFrom:
            secretKeyRef:
              name: app-secret
              key: DB_USERNAME
        - name: DB_PASS
          valueFrom:
            secretKeyRef:
              name: app-secret
              key: DB_PASSWORD
        # Mount config file
        volumeMounts:
        - name: config
          mountPath: /etc/app
        # Mount secrets
        - name: secrets
          mountPath: /etc/secrets
          readOnly: true
      volumes:
      - name: config
        configMap:
          name: app-config
          items:
          - key: app.yaml
            path: config.yaml
      - name: secrets
        secret:
          secretName: app-secret
```

## ConfigMap vs Secret Comparison

| Feature | ConfigMap | Secret |
|---------|-----------|--------|
| Purpose | Non-sensitive config | Sensitive data |
| Encoding | Plain text | Base64 encoded |
| Size Limit | 1MB | 1MB |
| Encryption | No | Optional (at rest) |
| Use Case | App settings, files | Passwords, keys, certs |

## Immutable ConfigMaps and Secrets

Prevent accidental updates:

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: immutable-config
immutable: true
data:
  KEY: "value"
```

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: immutable-secret
immutable: true
stringData:
  password: "secret"
```

**Benefits:**
- Protects from accidental updates
- Improves performance (kube-apiserver doesn't watch for changes)
- Requires deletion and recreation to update

## Best Practices

### ConfigMaps

1. **Organize by purpose**: Separate configs for different components
2. **Version your configs**: Use labels/names with versions
3. **Use declarative YAML**: Version control configuration
4. **Limit size**: Keep under 1MB
5. **Use for non-sensitive data only**: Never store passwords
6. **Document structure**: Add comments in YAML

### Secrets

1. **Use for sensitive data only**: Passwords, keys, tokens
2. **Enable encryption at rest**: Configure in cluster
3. **Use RBAC**: Restrict secret access
4. **Don't commit to Git**: Use .gitignore
5. **Consider external secret managers**: Vault, AWS Secrets Manager
6. **Rotate regularly**: Update secrets periodically
7. **Use readOnly volumes**: Prevent container modifications
8. **Limit secret exposure**: Mount only needed keys
9. **Use separate secrets**: Don't mix different credentials
10. **Monitor access**: Audit secret usage

## Troubleshooting

### Common Issues

#### 1. ConfigMap/Secret Not Found

```bash
# Check if it exists
kubectl get configmap app-config
kubectl get secret db-secret

# Check namespace
kubectl get configmap app-config -n my-namespace

# Verify pod reference
kubectl describe pod app-pod
```

#### 2. Environment Variables Not Set

```bash
# Check pod environment
kubectl exec app-pod -- env

# Verify ConfigMap/Secret keys match
kubectl describe configmap app-config
kubectl get configmap app-config -o yaml
```

#### 3. Files Not Mounted

```bash
# Check inside pod
kubectl exec app-pod -- ls /etc/config
kubectl exec app-pod -- cat /etc/config/key

# Verify volumeMount path
kubectl describe pod app-pod
```

#### 4. Base64 Encoding Issues

```bash
# Correct encoding (no newline)
echo -n 'password' | base64

# Decode to verify
echo 'cGFzc3dvcmQ=' | base64 --decode
```

### Debugging Commands

```bash
# View ConfigMap data
kubectl get configmap app-config -o yaml
kubectl describe configmap app-config

# View Secret data (decoded)
kubectl get secret db-secret -o jsonpath='{.data.password}' | base64 --decode

# Check pod environment
kubectl exec pod-name -- printenv

# Check mounted files
kubectl exec pod-name -- ls -la /etc/config
kubectl exec pod-name -- cat /etc/config/file

# View pod spec
kubectl get pod pod-name -o yaml
```

## Quick Reference

```bash
# ConfigMap
kubectl create configmap config --from-literal=KEY=value
kubectl create configmap config --from-file=config.txt
kubectl create configmap config --from-env-file=.env
kubectl get configmap config -o yaml
kubectl delete configmap config

# Secret
kubectl create secret generic secret --from-literal=password=secret
kubectl create secret generic secret --from-file=secret.txt
kubectl create secret docker-registry regcred --docker-server=...
kubectl create secret tls tls-secret --cert=cert.crt --key=key.key
kubectl get secret secret -o yaml
kubectl delete secret secret

# Decode secret
kubectl get secret secret -o jsonpath='{.data.password}' | base64 --decode

# Generate YAML
kubectl create configmap config --from-literal=KEY=value --dry-run=client -o yaml
kubectl create secret generic secret --from-literal=password=secret --dry-run=client -o yaml
```

## Next Steps

- Learn about Service Accounts
- Understand RBAC for secret access control
- Explore external secret management (Vault, Sealed Secrets)
- Study Pod Security Standards
- Practice with StatefulSets and persistent configuration
