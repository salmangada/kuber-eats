# Imperative Commands

## What are Imperative Commands?

Imperative commands allow you to quickly create, update, or delete Kubernetes resources using `kubectl` without writing YAML files. They're ideal for testing, quick tasks, and generating YAML templates.

**Imperative vs Declarative:**

| Imperative | Declarative |
|-----------|------------|
| Tell K8s what to do | Tell K8s desired state |
| `kubectl run/create/expose` | `kubectl apply -f file.yaml` |
| Quick and direct | Reproducible and versioned |
| Good for testing | Good for production |

## Pod Commands

### Creating Pods

```bash
# Create a simple pod
kubectl run nginx --image=nginx

# Create pod with custom port
kubectl run nginx --image=nginx --port=80

# Create pod with labels
kubectl run nginx --image=nginx --labels="app=web,env=prod"

# Create pod with environment variables
kubectl run nginx --image=nginx --env="ENV=production" --env="DEBUG=false"

# Create pod with resource limits
kubectl run nginx --image=nginx --limits="cpu=200m,memory=256Mi" --requests="cpu=100m,memory=128Mi"

# Create pod with command override
kubectl run busybox --image=busybox -- sleep 3600
kubectl run busybox --image=busybox --command -- /bin/sh -c "while true; do echo hello; sleep 10; done"

# Create pod in specific namespace
kubectl run nginx --image=nginx -n dev

# Dry run (don't create, just show what would be created)
kubectl run nginx --image=nginx --dry-run=client

# Generate YAML
kubectl run nginx --image=nginx --dry-run=client -o yaml
kubectl run nginx --image=nginx --dry-run=client -o yaml > pod.yaml

# Create pod and expose as service
kubectl run nginx --image=nginx --port=80 --expose

# Create pod with restart policy
kubectl run busybox --image=busybox --restart=Never -- /bin/sh -c "echo hello"
kubectl run busybox --image=busybox --restart=OnFailure -- /bin/sh -c "exit 1"

# Create pod in interactive mode
kubectl run -it busybox --image=busybox --rm -- /bin/sh
# --rm deletes pod after exit
```

## Deployment Commands

### Creating Deployments

```bash
# Create deployment
kubectl create deployment nginx --image=nginx

# Create deployment with replicas
kubectl create deployment nginx --image=nginx --replicas=3

# Create deployment with port
kubectl create deployment nginx --image=nginx --port=80

# Create in namespace
kubectl create deployment nginx --image=nginx -n dev

# Generate YAML
kubectl create deployment nginx --image=nginx --dry-run=client -o yaml
kubectl create deployment nginx --image=nginx --replicas=3 --dry-run=client -o yaml > deployment.yaml
```

### Managing Deployments

```bash
# Scale deployment
kubectl scale deployment nginx --replicas=5

# Autoscale deployment
kubectl autoscale deployment nginx --min=3 --max=10 --cpu-percent=80

# Update deployment image
kubectl set image deployment/nginx nginx=nginx:1.22
kubectl set image deployment/nginx nginx=nginx:1.22 --record

# Update multiple containers
kubectl set image deployment/nginx container1=image1:v2 container2=image2:v2

# Set resource limits
kubectl set resources deployment nginx --limits=cpu=200m,memory=512Mi --requests=cpu=100m,memory=256Mi

# Set environment variable
kubectl set env deployment/nginx ENV=production

# Remove environment variable
kubectl set env deployment/nginx ENV-

# View rollout status
kubectl rollout status deployment/nginx

# View rollout history
kubectl rollout history deployment/nginx

# Rollback deployment
kubectl rollout undo deployment/nginx
kubectl rollout undo deployment/nginx --to-revision=2

# Pause deployment
kubectl rollout pause deployment/nginx

# Resume deployment
kubectl rollout resume deployment/nginx

# Restart deployment (rolling restart)
kubectl rollout restart deployment/nginx
```

## Service Commands

### Creating Services

```bash
# Expose deployment as ClusterIP service
kubectl expose deployment nginx --port=80 --target-port=8080

# Expose as NodePort
kubectl expose deployment nginx --type=NodePort --port=80

# Expose as LoadBalancer
kubectl expose deployment nginx --type=LoadBalancer --port=80

# Expose pod
kubectl expose pod nginx --port=80 --name=nginx-service

# Expose with custom name
kubectl expose deployment nginx --port=80 --name=my-service

# Create ClusterIP service
kubectl create service clusterip my-service --tcp=80:8080

# Create NodePort service
kubectl create service nodeport my-service --tcp=80:8080 --node-port=30080

# Create LoadBalancer service
kubectl create service loadbalancer my-service --tcp=80:8080

# Generate service YAML
kubectl expose deployment nginx --port=80 --dry-run=client -o yaml
kubectl create service clusterip my-service --tcp=80:8080 --dry-run=client -o yaml > service.yaml
```

## Namespace Commands

```bash
# Create namespace
kubectl create namespace dev
kubectl create ns staging

# Set default namespace
kubectl config set-context --current --namespace=dev

# Generate YAML
kubectl create namespace dev --dry-run=client -o yaml > namespace.yaml
```

## ConfigMap Commands

```bash
# Create from literals
kubectl create configmap app-config --from-literal=DB_HOST=mysql --from-literal=DB_PORT=3306

# Create from file
kubectl create configmap app-config --from-file=config.properties

# Create from directory
kubectl create configmap app-config --from-file=./config-dir/

# Create from env file
kubectl create configmap app-config --from-env-file=.env

# Multiple sources
kubectl create configmap app-config --from-literal=KEY1=value1 --from-file=config.json

# Generate YAML
kubectl create configmap app-config --from-literal=KEY=value --dry-run=client -o yaml
```

## Secret Commands

```bash
# Create generic secret from literals
kubectl create secret generic db-secret --from-literal=username=admin --from-literal=password=secret123

# Create from file
kubectl create secret generic db-secret --from-file=username.txt --from-file=password.txt

# Create TLS secret
kubectl create secret tls tls-secret --cert=path/to/cert.crt --key=path/to/key.key

# Create Docker registry secret
kubectl create secret docker-registry regcred \
  --docker-server=myregistry.com \
  --docker-username=admin \
  --docker-password=secret \
  --docker-email=admin@example.com

# Generate YAML
kubectl create secret generic db-secret --from-literal=password=secret --dry-run=client -o yaml
```

## Job and CronJob Commands

```bash
# Create job
kubectl create job my-job --image=busybox -- echo "Hello World"

# Create job from cronjob
kubectl create job test-job --from=cronjob/my-cronjob

# Create cronjob
kubectl create cronjob my-cronjob --image=busybox --schedule="*/5 * * * *" -- echo "Hello"

# Generate YAML
kubectl create cronjob my-cronjob --image=busybox --schedule="0 */1 * * *" --dry-run=client -o yaml
```

## ServiceAccount Commands

```bash
# Create service account
kubectl create serviceaccount my-sa

# Create in namespace
kubectl create serviceaccount my-sa -n dev

# Generate YAML
kubectl create serviceaccount my-sa --dry-run=client -o yaml
```

## Role and RoleBinding Commands

```bash
# Create role
kubectl create role pod-reader --verb=get,list,watch --resource=pods

# Create role with resource names
kubectl create role pod-reader --verb=get --resource=pods --resource-name=pod1,pod2

# Create ClusterRole
kubectl create clusterrole pod-reader --verb=get,list,watch --resource=pods

# Create RoleBinding
kubectl create rolebinding pod-reader-binding --role=pod-reader --user=john

# Create ClusterRoleBinding
kubectl create clusterrolebinding pod-reader-binding --clusterrole=pod-reader --user=john

# Bind to service account
kubectl create rolebinding pod-reader-binding --role=pod-reader --serviceaccount=default:my-sa
```

## Editing Resources

```bash
# Edit resource in default editor
kubectl edit deployment nginx
kubectl edit service nginx
kubectl edit pod nginx

# Edit specific resource
kubectl edit deployment/nginx
kubectl edit svc/nginx

# Edit in specific namespace
kubectl edit deployment nginx -n dev
```

## Labeling and Annotating

```bash
# Add label
kubectl label pod nginx env=production
kubectl label deployment nginx tier=frontend

# Update existing label
kubectl label pod nginx env=staging --overwrite

# Remove label
kubectl label pod nginx env-

# Label multiple resources
kubectl label pods --all app=myapp

# Add annotation
kubectl annotate pod nginx description="Nginx web server"

# Remove annotation
kubectl annotate pod nginx description-
```

## Viewing Resources

```bash
# Get resources
kubectl get pods
kubectl get deployments
kubectl get services
kubectl get all

# Get with more details
kubectl get pods -o wide
kubectl get pods -o yaml
kubectl get pods -o json

# Get specific resource
kubectl get pod nginx
kubectl get deployment/nginx

# Get with labels
kubectl get pods --show-labels
kubectl get pods -l app=nginx
kubectl get pods -l 'env in (prod,staging)'

# Get in all namespaces
kubectl get pods -A
kubectl get pods --all-namespaces

# Watch resources
kubectl get pods --watch
kubectl get pods -w

# Describe resource
kubectl describe pod nginx
kubectl describe deployment nginx

# Get multiple resource types
kubectl get pods,services,deployments
```

## Debugging Commands

```bash
# View logs
kubectl logs nginx
kubectl logs nginx -f  # Follow logs
kubectl logs nginx --tail=100
kubectl logs nginx --since=1h
kubectl logs nginx --previous  # Previous container logs

# Execute commands in pod
kubectl exec nginx -- ls /
kubectl exec -it nginx -- /bin/bash
kubectl exec -it nginx -- sh

# Execute in specific container
kubectl exec -it nginx -c container-name -- /bin/bash

# Copy files to/from pod
kubectl cp /local/file nginx:/remote/file
kubectl cp nginx:/remote/file /local/file

# Port forward
kubectl port-forward pod/nginx 8080:80
kubectl port-forward service/nginx 8080:80
kubectl port-forward deployment/nginx 8080:80

# Proxy to Kubernetes API
kubectl proxy
kubectl proxy --port=8080

# Top (resource usage)
kubectl top nodes
kubectl top pods
kubectl top pod nginx
```

## Deleting Resources

```bash
# Delete pod
kubectl delete pod nginx

# Delete deployment
kubectl delete deployment nginx

# Delete service
kubectl delete service nginx

# Delete multiple resources
kubectl delete pod pod1 pod2 pod3
kubectl delete deployment dep1 dep2

# Delete by label
kubectl delete pods -l app=nginx
kubectl delete all -l app=nginx

# Delete all of a type
kubectl delete pods --all
kubectl delete deployments --all

# Delete all in namespace
kubectl delete all --all -n dev

# Delete namespace (and all resources in it)
kubectl delete namespace dev

# Force delete
kubectl delete pod nginx --force --grace-period=0
```

## Scaling Commands

```bash
# Scale deployment
kubectl scale deployment nginx --replicas=5

# Scale ReplicaSet
kubectl scale rs nginx-rs --replicas=3

# Scale StatefulSet
kubectl scale statefulset nginx-sts --replicas=3

# Conditional scaling
kubectl scale deployment nginx --current-replicas=3 --replicas=5
```

## Context and Configuration

```bash
# View contexts
kubectl config get-contexts

# Switch context
kubectl config use-context minikube

# View current context
kubectl config current-context

# Set namespace for context
kubectl config set-context --current --namespace=dev

# View config
kubectl config view
```

## Common Command Patterns

### Create and Generate YAML

```bash
# Pattern: create resource and save YAML
kubectl create deployment nginx --image=nginx --dry-run=client -o yaml > deployment.yaml

# Common variations
kubectl run nginx --image=nginx --dry-run=client -o yaml > pod.yaml
kubectl expose deployment nginx --port=80 --dry-run=client -o yaml > service.yaml
kubectl create configmap config --from-literal=KEY=value --dry-run=client -o yaml > configmap.yaml
```

### Quick Testing

```bash
# Test image
kubectl run test --image=nginx --rm -it --restart=Never -- /bin/bash

# Test network connectivity
kubectl run debug --image=nicolaka/netshoot --rm -it -- bash

# Test DNS
kubectl run debug --image=busybox --rm -it -- nslookup kubernetes.default

# Quick curl
kubectl run curl --image=curlimages/curl --rm -it -- curl http://service-name
```

### Resource Management

```bash
# Update image
kubectl set image deployment/nginx nginx=nginx:1.22

# Set resources
kubectl set resources deployment/nginx --limits=cpu=200m,memory=512Mi

# Set environment
kubectl set env deployment/nginx KEY=value

# Add label
kubectl label deployment nginx version=v1

# Annotate
kubectl annotate deployment nginx kubernetes.io/change-cause="Updated to v1.22"
```

## Useful Shortcuts

```bash
# Short names
kubectl get po        # pods
kubectl get deploy    # deployments
kubectl get svc       # services
kubectl get ns        # namespaces
kubectl get no        # nodes
kubectl get rs        # replicasets
kubectl get cm        # configmaps
kubectl get sa        # serviceaccounts

# Combine with other options
kubectl get po -A -o wide --show-labels
kubectl describe po nginx -n dev
kubectl logs deploy/nginx -f --tail=50
```

## Best Practices

1. **Use imperative for testing**: Quick experiments and learning
2. **Use declarative for production**: Version control and reproducibility
3. **Generate YAML first**: Use --dry-run=client -o yaml
4. **Label resources**: Makes selection and cleanup easier
5. **Use namespaces**: Organize and isolate resources
6. **Document changes**: Use annotations for change cause
7. **Clean up**: Delete test resources after use
8. **Use --dry-run**: Test commands before execution
9. **Learn shortcuts**: Speeds up daily work
10. **Combine with YAML**: Generate template, then customize

## Quick Reference

```bash
# Create
kubectl run nginx --image=nginx
kubectl create deployment nginx --image=nginx --replicas=3
kubectl expose deployment nginx --port=80

# View
kubectl get pods
kubectl describe pod nginx
kubectl logs nginx

# Update
kubectl scale deployment nginx --replicas=5
kubectl set image deployment/nginx nginx=nginx:1.22
kubectl edit deployment nginx

# Delete
kubectl delete pod nginx
kubectl delete deployment nginx

# Debug
kubectl exec -it nginx -- /bin/bash
kubectl port-forward pod/nginx 8080:80
kubectl logs nginx -f

# Generate YAML
kubectl run nginx --image=nginx --dry-run=client -o yaml > pod.yaml
```

## Next Steps

- Practice combining imperative and declarative approaches
- Learn advanced kubectl plugins
- Explore kubectl aliases for efficiency
- Study kubectl cheat sheets
- Master resource selection with labels
