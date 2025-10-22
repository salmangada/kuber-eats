# Kubernetes Architecture

## What is Kubernetes?

Kubernetes (K8s) is an open-source container orchestration platform that automates the deployment, scaling, and management of containerized applications.

## Cluster Architecture

A Kubernetes cluster consists of two types of nodes:

### 1. Control Plane (Master Node)

The control plane manages the cluster and makes decisions about scheduling, scaling, and maintaining the desired state.

**Components:**

#### API Server (kube-apiserver)
- Entry point for all REST commands used to control the cluster
- Validates and processes requests
- Updates the state in etcd
- Only component that directly communicates with etcd

```bash
# Check API server status
kubectl cluster-info
```

#### etcd
- Consistent and highly-available key-value store
- Stores all cluster data (configuration, state, metadata)
- Source of truth for the cluster

```bash
# View etcd members (if accessible)
kubectl get pods -n kube-system | grep etcd
```

#### Scheduler (kube-scheduler)
- Watches for newly created Pods with no assigned node
- Selects the best node for the Pod based on:
  - Resource requirements
  - Hardware/software constraints
  - Affinity/anti-affinity rules
  - Taints and tolerations

```bash
# View scheduler logs
kubectl logs -n kube-system kube-scheduler-<node-name>
```

#### Controller Manager (kube-controller-manager)
- Runs controller processes that regulate the cluster state
- Examples:
  - **Node Controller**: Monitors node health
  - **Replication Controller**: Maintains correct number of pods
  - **Endpoints Controller**: Populates endpoint objects
  - **Service Account Controller**: Creates default service accounts

```bash
# View controller manager
kubectl get pods -n kube-system | grep controller-manager
```

#### Cloud Controller Manager (optional)
- Manages cloud-provider-specific controllers
- Integrates with cloud services (load balancers, storage, networking)

### 2. Worker Nodes

Worker nodes run the containerized applications.

**Components:**

#### Kubelet
- Primary node agent that runs on each node
- Ensures containers are running in Pods
- Reports node and pod status to the control plane
- Manages Pod lifecycle

```bash
# Check kubelet status (on node)
systemctl status kubelet
```

#### Kube-proxy
- Network proxy that runs on each node
- Maintains network rules for pod communication
- Implements Services abstraction
- Handles load balancing across pods

```bash
# View kube-proxy logs
kubectl logs -n kube-system kube-proxy-<pod-name>
```

#### Container Runtime
- Software responsible for running containers
- Examples: Docker, containerd, CRI-O
- Pulls images and manages container lifecycle

```bash
# Check container runtime
kubectl get nodes -o wide
```

## Architecture Flow

```
┌─────────────────────────────────────────────────────────┐
│                    Control Plane                         │
│  ┌──────────┐  ┌──────┐  ┌───────────┐  ┌────────────┐ │
│  │API Server│◄─┤ etcd ├─►│ Scheduler │  │Controller  │ │
│  └────┬─────┘  └──────┘  └─────┬─────┘  │Manager     │ │
│       │                         │        └──────┬─────┘ │
└───────┼─────────────────────────┼───────────────┼───────┘
        │                         │               │
        │                         ▼               ▼
┌───────┼─────────────────────────────────────────────────┐
│       │                  Worker Node                     │
│  ┌────▼────┐         ┌──────────┐                       │
│  │ Kubelet │◄────────┤Container │                       │
│  └────┬────┘         │Runtime   │                       │
│       │              └────┬─────┘                        │
│       │                   │                              │
│  ┌────▼────┐        ┌────▼─────┐      ┌──────────┐     │
│  │Kube-    │        │   Pod    │      │   Pod    │     │
│  │Proxy    │        └──────────┘      └──────────┘     │
│  └─────────┘                                             │
└──────────────────────────────────────────────────────────┘
```

## How Kubernetes Works

### Pod Creation Flow:

1. **User submits** a Pod manifest via `kubectl`
2. **API Server** receives the request, validates it, and stores in etcd
3. **Scheduler** watches for unscheduled Pods and assigns a node
4. **API Server** updates etcd with scheduling decision
5. **Kubelet** on the assigned node sees the Pod assignment
6. **Kubelet** instructs the Container Runtime to pull images and start containers
7. **Kubelet** reports status back to the API Server
8. **API Server** updates etcd with current status

## Common Commands

```bash
# View cluster info
kubectl cluster-info

# View cluster nodes
kubectl get nodes

# View all components in kube-system namespace
kubectl get all -n kube-system

# Describe a node
kubectl describe node <node-name>

# Check component health
kubectl get componentstatuses  # (deprecated but may work)
kubectl get --raw='/readyz?verbose'

# View API resources
kubectl api-resources

# View cluster events
kubectl get events --all-namespaces
```

## Key Concepts

- **Declarative Model**: You declare desired state, Kubernetes makes it happen
- **Self-healing**: Automatically replaces and reschedules failed containers
- **Horizontal Scaling**: Scale applications up or down easily
- **Service Discovery**: Automatic DNS for services
- **Load Balancing**: Distributes traffic across pods
- **Rolling Updates**: Update applications with zero downtime

## Next Steps

Understanding the architecture is crucial before diving into:
- Creating and managing Pods
- Working with YAML manifests
- Understanding ReplicaSets and Deployments
- Configuring Services for networking
