# Helm Fundamentals

## What is Helm?

Helm is the package manager for Kubernetes. It helps you manage Kubernetes applications by packaging them into charts that can be easily deployed, upgraded, and shared.

**Think of Helm as:**
- apt/yum for Kubernetes
- npm for Kubernetes
- A templating engine for YAML files
- A release management tool

**Key Benefits:**
- Simplify complex deployments
- Reusable application packages
- Version control for releases
- Easy rollbacks
- Template-based configuration
- Share applications via repositories

## Core Concepts

### 1. Chart

A Helm package containing Kubernetes resource definitions.

**Chart Structure:**
```
mychart/
├── Chart.yaml          # Chart metadata
├── values.yaml         # Default configuration values
├── charts/             # Chart dependencies
├── templates/          # Kubernetes manifests (templated)
│   ├── deployment.yaml
│   ├── service.yaml
│   ├── ingress.yaml
│   ├── _helpers.tpl   # Template helpers
│   └── NOTES.txt      # Post-install notes
├── .helmignore        # Files to ignore
└── README.md          # Chart documentation
```

### 2. Release

An instance of a chart running in a Kubernetes cluster.

- Same chart can be installed multiple times
- Each installation creates a new release
- Releases have unique names

### 3. Repository

A collection of Helm charts that can be shared.

**Popular Repositories:**
- Artifact Hub (https://artifacthub.io/)
- Bitnami Charts
- Stable Charts (deprecated, moved to individual repos)

## Installing Helm

### Installation

```bash
# Linux (using script)
curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash

# Or download binary
wget https://get.helm.sh/helm-v3.13.0-linux-amd64.tar.gz
tar -zxvf helm-v3.13.0-linux-amd64.tar.gz
sudo mv linux-amd64/helm /usr/local/bin/helm

# macOS
brew install helm

# Verify installation
helm version
```

### Initial Setup

```bash
# Add repository
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo add stable https://charts.helm.sh/stable

# Update repositories
helm repo update

# List repositories
helm repo list

# Search for charts
helm search repo nginx
helm search repo wordpress

# Search Artifact Hub
helm search hub wordpress
```

## Using Helm Charts

### Installing Charts

```bash
# Install chart
helm install my-release bitnami/nginx

# Install with custom name
helm install my-nginx bitnami/nginx

# Install in specific namespace
helm install my-nginx bitnami/nginx --namespace production --create-namespace

# Install with custom values
helm install my-nginx bitnami/nginx --set service.type=LoadBalancer

# Install from values file
helm install my-nginx bitnami/nginx -f custom-values.yaml

# Dry run (see what would be installed)
helm install my-nginx bitnami/nginx --dry-run --debug

# Generate manifests without installing
helm template my-nginx bitnami/nginx > nginx-manifests.yaml
```

### Managing Releases

```bash
# List releases
helm list
helm list --all-namespaces
helm list -n production

# Get release status
helm status my-nginx

# Get release values
helm get values my-nginx

# Get release manifest
helm get manifest my-nginx

# Get all release info
helm get all my-nginx

# Release history
helm history my-nginx
```

### Upgrading Releases

```bash
# Upgrade release
helm upgrade my-nginx bitnami/nginx

# Upgrade with new values
helm upgrade my-nginx bitnami/nginx --set replicaCount=3

# Upgrade with values file
helm upgrade my-nginx bitnami/nginx -f new-values.yaml

# Upgrade or install (if not exists)
helm upgrade --install my-nginx bitnami/nginx

# Force upgrade
helm upgrade my-nginx bitnami/nginx --force

# Wait for resources to be ready
helm upgrade my-nginx bitnami/nginx --wait --timeout 5m
```

### Rolling Back Releases

```bash
# Rollback to previous version
helm rollback my-nginx

# Rollback to specific revision
helm rollback my-nginx 2

# Rollback with cleanup
helm rollback my-nginx 2 --cleanup-on-fail
```

### Uninstalling Releases

```bash
# Uninstall release
helm uninstall my-nginx

# Uninstall from specific namespace
helm uninstall my-nginx -n production

# Keep release history
helm uninstall my-nginx --keep-history
```

## Creating Helm Charts

### Creating a New Chart

```bash
# Create chart scaffold
helm create mychart

# Chart structure created:
# mychart/
# ├── Chart.yaml
# ├── values.yaml
# ├── charts/
# └── templates/
```

### Chart.yaml

Chart metadata file:

```yaml
apiVersion: v2
name: myapp
description: A Helm chart for my application
type: application
version: 1.0.0        # Chart version
appVersion: "2.0.0"   # Application version

keywords:
  - web
  - nginx
maintainers:
  - name: Your Name
    email: you@example.com
sources:
  - https://github.com/user/myapp
dependencies:
  - name: redis
    version: 17.0.0
    repository: https://charts.bitnami.com/bitnami
```

### values.yaml

Default configuration values:

```yaml
# Default values for myapp
replicaCount: 3

image:
  repository: myapp
  pullPolicy: IfNotPresent
  tag: "1.0.0"

service:
  type: ClusterIP
  port: 80

ingress:
  enabled: false
  className: nginx
  hosts:
    - host: myapp.example.com
      paths:
        - path: /
          pathType: Prefix

resources:
  limits:
    cpu: 500m
    memory: 512Mi
  requests:
    cpu: 250m
    memory: 256Mi

autoscaling:
  enabled: false
  minReplicas: 3
  maxReplicas: 10
  targetCPUUtilizationPercentage: 80

nodeSelector: {}
tolerations: []
affinity: {}
```

### Templates

#### Deployment Template

```yaml
# templates/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: {{ include "myapp.fullname" . }}
  labels:
    {{- include "myapp.labels" . | nindent 4 }}
spec:
  {{- if not .Values.autoscaling.enabled }}
  replicas: {{ .Values.replicaCount }}
  {{- end }}
  selector:
    matchLabels:
      {{- include "myapp.selectorLabels" . | nindent 6 }}
  template:
    metadata:
      labels:
        {{- include "myapp.selectorLabels" . | nindent 8 }}
    spec:
      containers:
      - name: {{ .Chart.Name }}
        image: "{{ .Values.image.repository }}:{{ .Values.image.tag | default .Chart.AppVersion }}"
        imagePullPolicy: {{ .Values.image.pullPolicy }}
        ports:
        - name: http
          containerPort: 8080
          protocol: TCP
        resources:
          {{- toYaml .Values.resources | nindent 10 }}
```

#### Service Template

```yaml
# templates/service.yaml
apiVersion: v1
kind: Service
metadata:
  name: {{ include "myapp.fullname" . }}
  labels:
    {{- include "myapp.labels" . | nindent 4 }}
spec:
  type: {{ .Values.service.type }}
  ports:
  - port: {{ .Values.service.port }}
    targetPort: http
    protocol: TCP
    name: http
  selector:
    {{- include "myapp.selectorLabels" . | nindent 4 }}
```

#### Conditional Resources

```yaml
# templates/ingress.yaml
{{- if .Values.ingress.enabled -}}
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: {{ include "myapp.fullname" . }}
  labels:
    {{- include "myapp.labels" . | nindent 4 }}
spec:
  ingressClassName: {{ .Values.ingress.className }}
  rules:
  {{- range .Values.ingress.hosts }}
  - host: {{ .host | quote }}
    http:
      paths:
      {{- range .paths }}
      - path: {{ .path }}
        pathType: {{ .pathType }}
        backend:
          service:
            name: {{ include "myapp.fullname" $ }}
            port:
              number: {{ $.Values.service.port }}
      {{- end }}
  {{- end }}
{{- end }}
```

### Helper Templates

```yaml
# templates/_helpers.tpl
{{/*
Expand the name of the chart.
*/}}
{{- define "myapp.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
*/}}
{{- define "myapp.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "myapp.labels" -}}
helm.sh/chart: {{ include "myapp.chart" . }}
{{ include "myapp.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "myapp.selectorLabels" -}}
app.kubernetes.io/name: {{ include "myapp.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}
```

### NOTES.txt

Post-installation notes:

```yaml
# templates/NOTES.txt
Thank you for installing {{ .Chart.Name }}.

Your release is named {{ .Release.Name }}.

To access your application:

{{- if .Values.ingress.enabled }}
  http://{{ .Values.ingress.hosts.0.host }}
{{- else if contains "NodePort" .Values.service.type }}
  export NODE_PORT=$(kubectl get --namespace {{ .Release.Namespace }} -o jsonpath="{.spec.ports[0].nodePort}" services {{ include "myapp.fullname" . }})
  export NODE_IP=$(kubectl get nodes --namespace {{ .Release.Namespace }} -o jsonpath="{.items[0].status.addresses[0].address}")
  echo http://$NODE_IP:$NODE_PORT
{{- else if contains "LoadBalancer" .Values.service.type }}
  export SERVICE_IP=$(kubectl get svc --namespace {{ .Release.Namespace }} {{ include "myapp.fullname" . }} -o jsonpath='{.status.loadBalancer.ingress[0].ip}')
  echo http://$SERVICE_IP:{{ .Values.service.port }}
{{- else if contains "ClusterIP" .Values.service.type }}
  export POD_NAME=$(kubectl get pods --namespace {{ .Release.Namespace }} -l "app.kubernetes.io/name={{ include "myapp.name" . }},app.kubernetes.io/instance={{ .Release.Name }}" -o jsonpath="{.items[0].metadata.name}")
  kubectl --namespace {{ .Release.Namespace }} port-forward $POD_NAME 8080:80
  echo "Visit http://127.0.0.1:8080 to use your application"
{{- end }}
```

## Template Functions and Pipelines

### Built-in Functions

```yaml
# String manipulation
{{ .Values.name | upper }}           # UPPERCASE
{{ .Values.name | lower }}           # lowercase
{{ .Values.name | title }}           # Title Case
{{ .Values.name | quote }}           # "quoted"
{{ .Values.name | trunc 5 }}         # Truncate to 5 chars
{{ .Values.name | trim }}            # Trim whitespace

# Default values
{{ .Values.name | default "myapp" }}

# Type conversion
{{ .Values.port | toString }}
{{ .Values.count | int }}

# Encoding
{{ .Values.data | b64enc }}          # Base64 encode
{{ .Values.data | b64dec }}          # Base64 decode

# YAML/JSON
{{ .Values.config | toYaml }}
{{ .Values.config | toJson }}

# Indentation
{{ .Values.labels | toYaml | nindent 4 }}
{{ .Values.labels | toYaml | indent 4 }}
```

### Conditionals

```yaml
# If statement
{{- if .Values.ingress.enabled }}
apiVersion: networking.k8s.io/v1
kind: Ingress
{{- end }}

# If-else
{{- if .Values.useRedis }}
redis: true
{{- else }}
redis: false
{{- end }}

# If-else if-else
{{- if eq .Values.environment "production" }}
replicas: 5
{{- else if eq .Values.environment "staging" }}
replicas: 3
{{- else }}
replicas: 1
{{- end }}
```

### Loops

```yaml
# Range over list
{{- range .Values.hosts }}
- host: {{ . }}
{{- end }}

# Range with index
{{- range $index, $host := .Values.hosts }}
- name: host-{{ $index }}
  value: {{ $host }}
{{- end }}

# Range over map
{{- range $key, $value := .Values.env }}
- name: {{ $key }}
  value: {{ $value | quote }}
{{- end }}
```

### Variables

```yaml
# Define variable
{{- $fullName := include "myapp.fullname" . }}

# Use variable
name: {{ $fullName }}

# Scoped variable
{{- with .Values.resources }}
resources:
  limits:
    cpu: {{ .limits.cpu }}
    memory: {{ .limits.memory }}
  requests:
    cpu: {{ .requests.cpu }}
    memory: {{ .requests.memory }}
{{- end }}
```

## Testing Charts

### Linting

```bash
# Lint chart
helm lint mychart

# Lint with values
helm lint mychart -f values-prod.yaml
```

### Dry Run

```bash
# Dry run install
helm install my-release mychart --dry-run --debug

# See rendered templates
helm template my-release mychart

# Test specific values
helm template my-release mychart --set replicaCount=5
```

### Chart Testing

```bash
# Install helm-unittest plugin
helm plugin install https://github.com/helm-unittest/helm-unittest

# Create test file: tests/deployment_test.yaml
suite: test deployment
templates:
  - deployment.yaml
tests:
  - it: should create deployment
    asserts:
      - isKind:
          of: Deployment
      - equal:
          path: spec.replicas
          value: 3

# Run tests
helm unittest mychart
```

## Packaging and Sharing Charts

### Package Chart

```bash
# Package chart
helm package mychart

# Outputs: mychart-1.0.0.tgz

# Package with dependencies
helm package mychart --dependency-update

# Package with specific version
helm package mychart --version 1.0.1
```

### Chart Repository

```bash
# Create repository index
helm repo index .

# Update repository
helm repo update

# Add local repository
helm repo add myrepo http://localhost:8080

# Serve charts locally
helm serve
```

### Upload to Artifact Hub

```bash
# Create GitHub repository
# Add artifacthub-repo.yml
# Push chart to repo
# Register on artifacthub.io
```

## Chart Dependencies

### Add Dependencies

```yaml
# Chart.yaml
dependencies:
  - name: redis
    version: 17.0.0
    repository: https://charts.bitnami.com/bitnami
  - name: postgresql
    version: 12.0.0
    repository: https://charts.bitnami.com/bitnami
    condition: postgresql.enabled
```

### Managing Dependencies

```bash
# Download dependencies
helm dependency update mychart

# List dependencies
helm dependency list mychart

# Build dependencies
helm dependency build mychart
```

### Override Dependency Values

```yaml
# values.yaml
redis:
  auth:
    enabled: true
    password: "mypassword"
  master:
    persistence:
      size: 8Gi

postgresql:
  enabled: false
  auth:
    username: myuser
    password: mypassword
    database: mydb
```

## Advanced Features

### Hooks

Execute actions at specific points in release lifecycle:

```yaml
# templates/pre-install-job.yaml
apiVersion: batch/v1
kind: Job
metadata:
  name: {{ include "myapp.fullname" . }}-db-migration
  annotations:
    "helm.sh/hook": pre-install
    "helm.sh/hook-weight": "5"
    "helm.sh/hook-delete-policy": hook-succeeded
spec:
  template:
    spec:
      containers:
      - name: migration
        image: migrate/migrate
        command: ["migrate", "up"]
      restartPolicy: Never
```

**Hook Types:**
- pre-install
- post-install
- pre-delete
- post-delete
- pre-upgrade
- post-upgrade
- pre-rollback
- post-rollback

### Schema Validation

```yaml
# values.schema.json
{
  "$schema": "https://json-schema.org/draft-07/schema#",
  "type": "object",
  "required": ["replicaCount", "image"],
  "properties": {
    "replicaCount": {
      "type": "integer",
      "minimum": 1
    },
    "image": {
      "type": "object",
      "required": ["repository", "tag"],
      "properties": {
        "repository": {
          "type": "string"
        },
        "tag": {
          "type": "string"
        }
      }
    }
  }
}
```

## Best Practices

1. **Use semantic versioning**: For both chart and app versions
2. **Document values.yaml**: Add comments for all values
3. **Provide sensible defaults**: Chart should work out of the box
4. **Use helper templates**: Avoid duplication
5. **Test thoroughly**: Use helm lint and helm test
6. **Keep charts simple**: Don't over-engineer
7. **Use .helmignore**: Exclude unnecessary files
8. **Version lock dependencies**: Specify exact versions
9. **Provide NOTES.txt**: Help users get started
10. **Follow naming conventions**: Use standard labels

## Common Use Cases

### Deploy Web Application

```bash
# Install with custom values
helm install myapp ./mychart \
  --set image.tag=2.0.0 \
  --set replicaCount=5 \
  --set ingress.enabled=true \
  --set ingress.hosts[0].host=myapp.example.com
```

### Deploy to Multiple Environments

```yaml
# values-dev.yaml
replicaCount: 1
environment: development

# values-staging.yaml
replicaCount: 3
environment: staging

# values-prod.yaml
replicaCount: 5
environment: production
```

```bash
# Deploy to different environments
helm install myapp-dev ./mychart -f values-dev.yaml -n dev
helm install myapp-staging ./mychart -f values-staging.yaml -n staging
helm install myapp-prod ./mychart -f values-prod.yaml -n production
```

## Troubleshooting

### Common Issues

```bash
# Chart not found
helm repo update

# Template rendering errors
helm template my-release ./mychart --debug

# Installation failures
helm install my-release ./mychart --dry-run --debug

# View release secrets
kubectl get secrets -l owner=helm

# View release history
helm history my-release

# Get release manifest
helm get manifest my-release

# Uninstall stuck release
helm uninstall my-release --no-hooks
```

## Quick Reference

```bash
# Repository management
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo update
helm search repo nginx

# Installing
helm install my-release bitnami/nginx
helm install my-release ./mychart -f values.yaml
helm install my-release ./mychart --set key=value

# Managing releases
helm list
helm status my-release
helm get values my-release
helm upgrade my-release ./mychart
helm rollback my-release 1
helm uninstall my-release

# Chart development
helm create mychart
helm lint mychart
helm package mychart
helm template my-release ./mychart

# Dependencies
helm dependency update ./mychart
helm dependency list ./mychart
```

## Next Steps

- Create your first Helm chart
- Explore Helm Hub / Artifact Hub
- Learn about Helmfile for managing multiple releases
- Study advanced templating techniques
- Understand chart testing strategies
- Explore CI/CD integration with Helm
- Learn about Helm secrets (helm-secrets plugin)
- Study Helmfile and ArgoCD for GitOps
