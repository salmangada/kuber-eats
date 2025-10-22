# Kubernetes Learning Notes

Welcome to your comprehensive Kubernetes learning guide! This repository contains detailed notes covering all essential Kubernetes concepts, from basic architecture to advanced networking.

## About This Repository

This is a beginner-friendly guide to learning Kubernetes (K8s). Each topic is covered in depth with:
- Clear explanations and concepts
- Practical YAML examples
- Command-line references
- Best practices
- Troubleshooting tips
- Real-world use cases

## Learning Path

Follow this recommended order to build your Kubernetes knowledge progressively:

### Foundation (Start Here)

1. **[Kubernetes Architecture](01-kubernetes-architecture.md)**
   - Control plane components
   - Worker node components
   - How Kubernetes works
   - Cluster architecture overview

2. **[YAML Basics](03-yaml-basics.md)**
   - YAML syntax and structure
   - Kubernetes YAML format
   - Common patterns and mistakes
   - Generating YAML templates

3. **[Pods](02-pods.md)**
   - What are pods
   - Creating and managing pods
   - Pod lifecycle
   - Health probes and debugging

### Core Concepts

4. **[ReplicaSet](04-replicaset.md)**
   - Self-healing and high availability
   - Scaling applications
   - Label selectors
   - How ReplicaSets work

5. **[Deployments](05-deployments.md)**
   - Declarative updates
   - Rolling updates and rollbacks
   - Update strategies
   - Production deployment patterns

6. **[Services](06-services.md)**
   - Service types (ClusterIP, NodePort, LoadBalancer)
   - Service discovery and DNS
   - Endpoints and load balancing
   - Exposing applications

7. **[Namespaces](07-namespaces.md)**
   - Resource isolation
   - Multi-environment setup
   - Resource quotas and limits
   - Cross-namespace communication

### Configuration and Commands

8. **[Imperative Commands](08-imperative-commands.md)**
   - kubectl commands quick reference
   - Creating resources imperatively
   - Debugging and troubleshooting commands
   - Common command patterns

9. **[ConfigMaps and Secrets](09-configmaps-secrets.md)**
   - Application configuration
   - Sensitive data management
   - Environment variables and volumes
   - Best practices for secrets

### Advanced Topics

10. **[Service Accounts](10-service-accounts.md)**
    - Pod identity and authentication
    - RBAC integration
    - API access from pods
    - Service account tokens

11. **[Pod Scheduling](11-scheduling.md)**
    - Node selectors
    - Node affinity and anti-affinity
    - Taints and tolerations
    - Advanced scheduling strategies

12. **[Multi-Container Pods](12-multi-container-pods.md)**
    - Sidecar pattern
    - Init containers
    - Adapter and ambassador patterns
    - Container communication

13. **[Networking](13-networking.md)**
    - Pod and service networking
    - Network policies
    - Ingress controllers
    - DNS and service discovery
    - Troubleshooting network issues

### Security and Tooling

14. **[Security](14-security.md)**
    - RBAC (Role-Based Access Control)
    - Pod Security Standards
    - Network security and policies
    - Secrets management
    - Image security and scanning
    - Audit logging
    - Security best practices

15. **[Helm Fundamentals](15-helm-fundamentals.md)**
    - What is Helm and why use it
    - Charts, releases, and repositories
    - Installing and managing applications
    - Creating custom Helm charts
    - Templates and functions
    - Chart dependencies
    - Best practices and troubleshooting

## Quick Start Guide

### Prerequisites

```bash
# Install kubectl
# Linux
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl

# Verify installation
kubectl version --client

# Install Minikube (for local cluster)
curl -LO https://storage.googleapis.com/minikube/releases/latest/minikube-linux-amd64
sudo install minikube-linux-amd64 /usr/local/bin/minikube

# Start cluster
minikube start
```

### First Steps

```bash
# Check cluster info
kubectl cluster-info
kubectl get nodes

# Create your first pod
kubectl run nginx --image=nginx

# View the pod
kubectl get pods
kubectl describe pod nginx

# Delete the pod
kubectl delete pod nginx
```

### Essential Commands Reference

```bash
# Get resources
kubectl get pods
kubectl get deployments
kubectl get services
kubectl get all

# Describe resources
kubectl describe pod <name>
kubectl describe deployment <name>

# Create from YAML
kubectl apply -f <file.yaml>

# View logs
kubectl logs <pod-name>
kubectl logs -f <pod-name>  # Follow logs

# Execute commands
kubectl exec -it <pod-name> -- /bin/bash

# Port forwarding
kubectl port-forward <pod-name> 8080:80

# Delete resources
kubectl delete pod <name>
kubectl delete -f <file.yaml>
```

## Practice Exercises

### Beginner Level

1. Create a pod running nginx and expose it via a service
2. Create a deployment with 3 replicas
3. Scale the deployment to 5 replicas
4. Perform a rolling update to a new image version
5. Create a namespace and deploy resources in it

### Intermediate Level

1. Create a multi-container pod with a sidecar
2. Set up ConfigMaps and Secrets for an application
3. Configure resource limits and requests
4. Implement health checks (liveness and readiness probes)
5. Create network policies to restrict traffic

### Advanced Level

1. Set up pod affinity and anti-affinity rules
2. Configure taints and tolerations for dedicated nodes
3. Create an Ingress with path-based routing
4. Implement RBAC policies for different user roles
5. Create and deploy a custom Helm chart
6. Set up network policies for zero-trust security
7. Implement security contexts and pod security standards

## Useful Resources

### Official Documentation
- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [kubectl Cheat Sheet](https://kubernetes.io/docs/reference/kubectl/cheatsheet/)
- [Kubernetes API Reference](https://kubernetes.io/docs/reference/kubernetes-api/)

### Tools
- [kubectl](https://kubernetes.io/docs/tasks/tools/) - Kubernetes CLI
- [Minikube](https://minikube.sigs.k8s.io/) - Local Kubernetes cluster
- [K9s](https://k9scli.io/) - Terminal-based UI
- [Lens](https://k8slens.dev/) - Kubernetes IDE
- [Helm](https://helm.sh/) - Package manager for Kubernetes

### Interactive Learning
- [Kubernetes by Example](https://kubernetesbyexample.com/)
- [Play with Kubernetes](https://labs.play-with-k8s.com/)
- [Katacoda Kubernetes Courses](https://www.katacoda.com/courses/kubernetes)

## Study Tips

1. **Hands-On Practice**: Don't just read—try every command and example
2. **Use Minikube**: Set up a local cluster for experimentation
3. **Start Simple**: Master basics before moving to advanced topics
4. **Read Error Messages**: They often tell you exactly what's wrong
5. **Use `--dry-run`**: Test commands before execution
6. **Explore `kubectl explain`**: Built-in documentation for resources
7. **Join Communities**: Kubernetes Slack, Reddit, StackOverflow
8. **Build Projects**: Deploy real applications to solidify learning

## Common kubectl Shortcuts

```bash
# Short names
po      # pods
deploy  # deployments
svc     # services
ns      # namespaces
no      # nodes
cm      # configmaps
sa      # serviceaccounts
rs      # replicasets

# Examples
kubectl get po
kubectl get deploy
kubectl get svc
kubectl describe po nginx
```

## Troubleshooting Checklist

When things go wrong:

1. Check pod status: `kubectl get pods`
2. Describe the pod: `kubectl describe pod <name>`
3. Check logs: `kubectl logs <pod-name>`
4. Check events: `kubectl get events --sort-by='.lastTimestamp'`
5. Verify service endpoints: `kubectl get endpoints <service-name>`
6. Test DNS: `kubectl run debug --image=busybox -it --rm -- nslookup <service>`
7. Check resource quotas: `kubectl describe ns <namespace>`
8. Review YAML: `kubectl get <resource> <name> -o yaml`

## Certification Paths

If you're preparing for certifications:

- **CKA (Certified Kubernetes Administrator)**: Focus on cluster management, troubleshooting, and operations
- **CKAD (Certified Kubernetes Application Developer)**: Focus on application deployment and management
- **CKS (Certified Kubernetes Security Specialist)**: Focus on security best practices

## Contributing to Your Learning

As you learn, consider:
- Adding your own examples to these notes
- Creating practice scenarios
- Documenting issues you encounter and how you solved them
- Building sample applications
- Sharing your knowledge with others

## Next Steps After Completing This Guide

1. **Deep Dive into Advanced Topics**:
   - StatefulSets and persistent storage
   - DaemonSets and Jobs/CronJobs
   - Custom Resource Definitions (CRDs)
   - Operators and controllers
   - Helm charts

2. **Explore Cloud Platforms**:
   - AWS EKS
   - Google GKE
   - Azure AKS

3. **Learn Related Technologies**:
   - Docker and container runtime
   - Service mesh (Istio, Linkerd)
   - GitOps (ArgoCD, Flux)
   - Monitoring (Prometheus, Grafana)
   - Logging (ELK, Loki)

4. **Build Production Skills**:
   - High availability patterns
   - Disaster recovery
   - Cost optimization
   - Security hardening
   - Performance tuning

## File Structure

```
notes/
├── README.md                          # This file
├── 01-kubernetes-architecture.md      # K8s architecture
├── 02-pods.md                         # Pods basics
├── 03-yaml-basics.md                  # YAML syntax
├── 04-replicaset.md                   # ReplicaSets
├── 05-deployments.md                  # Deployments
├── 06-services.md                     # Services
├── 07-namespaces.md                   # Namespaces
├── 08-imperative-commands.md          # kubectl commands
├── 09-configmaps-secrets.md           # Configuration
├── 10-service-accounts.md             # Service accounts
├── 11-scheduling.md                   # Pod scheduling
├── 12-multi-container-pods.md         # Multi-container patterns
├── 13-networking.md                   # Networking
├── 14-security.md                     # Security best practices
└── 15-helm-fundamentals.md            # Helm package manager
```

## Getting Help

- **Kubernetes Slack**: https://slack.k8s.io/
- **Stack Overflow**: Tag your questions with `kubernetes`
- **GitHub Discussions**: https://github.com/kubernetes/kubernetes/discussions
- **Reddit**: r/kubernetes

## Version Information

These notes are based on Kubernetes 1.28+ but most concepts apply to earlier versions as well. Always check the official documentation for version-specific features.

---

Happy Learning! Remember, Kubernetes is complex, but with consistent practice and hands-on experience, you'll master it step by step.

Start with **01-kubernetes-architecture.md** and work your way through each topic. Good luck on your Kubernetes journey!
