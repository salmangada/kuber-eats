# Pod Scheduling: Node Selector, Affinity, Taints & Tolerations

## Overview

Kubernetes scheduler decides which node should run each pod. You can control this decision using:

1. **Node Selector**: Simple key-value label matching
2. **Node Affinity**: Advanced node selection with complex rules
3. **Pod Affinity/Anti-Affinity**: Schedule based on other pods
4. **Taints & Tolerations**: Repel pods from nodes (unless tolerated)

## Node Selector (Simple)

The simplest way to schedule pods on specific nodes using labels.

### Label Nodes

```bash
# Add label to node
kubectl label nodes node1 disktype=ssd
kubectl label nodes node2 disktype=hdd
kubectl label nodes node3 gpu=true

# View node labels
kubectl get nodes --show-labels

# View specific label
kubectl get nodes -L disktype,gpu
```

### Use Node Selector in Pod

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: nginx
spec:
  containers:
  - name: nginx
    image: nginx
  nodeSelector:
    disktype: ssd    # Will only schedule on nodes with this label
```

### Example with Deployment

```yaml
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
      nodeSelector:
        environment: production
        disktype: ssd
```

**Limitations:**
- Only supports equality-based matching
- Cannot express OR logic
- Cannot use operators like "NotIn", "Exists"

## Node Affinity (Advanced)

More expressive node selection with complex matching rules.

### Types of Node Affinity

1. **requiredDuringSchedulingIgnoredDuringExecution**
   - Hard requirement (must match)
   - Pod won't schedule if no matching node

2. **preferredDuringSchedulingIgnoredDuringExecution**
   - Soft preference (nice to have)
   - Pod schedules anyway if no match

### Required Node Affinity

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: nginx
spec:
  containers:
  - name: nginx
    image: nginx
  affinity:
    nodeAffinity:
      requiredDuringSchedulingIgnoredDuringExecution:
        nodeSelectorTerms:
        - matchExpressions:
          - key: disktype
            operator: In
            values:
            - ssd
            - nvme
```

### Preferred Node Affinity

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: nginx
spec:
  containers:
  - name: nginx
    image: nginx
  affinity:
    nodeAffinity:
      preferredDuringSchedulingIgnoredDuringExecution:
      - weight: 1
        preference:
          matchExpressions:
          - key: disktype
            operator: In
            values:
            - ssd
```

### Combined Required and Preferred

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: webapp
spec:
  containers:
  - name: webapp
    image: webapp:1.0
  affinity:
    nodeAffinity:
      # MUST have zone label
      requiredDuringSchedulingIgnoredDuringExecution:
        nodeSelectorTerms:
        - matchExpressions:
          - key: topology.kubernetes.io/zone
            operator: In
            values:
            - us-east-1a
            - us-east-1b
      # PREFER nodes with SSD
      preferredDuringSchedulingIgnoredDuringExecution:
      - weight: 100
        preference:
          matchExpressions:
          - key: disktype
            operator: In
            values:
            - ssd
      - weight: 50
        preference:
          matchExpressions:
          - key: instance-type
            operator: In
            values:
            - large
            - xlarge
```

### Node Affinity Operators

- **In**: Label value must be in the list
- **NotIn**: Label value must not be in the list
- **Exists**: Label key must exist (any value)
- **DoesNotExist**: Label key must not exist
- **Gt**: Greater than (numeric values)
- **Lt**: Less than (numeric values)

```yaml
# Examples of different operators
affinity:
  nodeAffinity:
    requiredDuringSchedulingIgnoredDuringExecution:
      nodeSelectorTerms:
      - matchExpressions:
        # In operator
        - key: environment
          operator: In
          values:
          - production
          - staging

        # NotIn operator
        - key: node-role
          operator: NotIn
          values:
          - master

        # Exists operator (any value OK)
        - key: gpu
          operator: Exists

        # DoesNotExist operator
        - key: spot-instance
          operator: DoesNotExist
```

## Pod Affinity and Anti-Affinity

Schedule pods based on labels of other pods already running on nodes.

### Pod Affinity (Co-locate Pods)

Run pods on the same node (or zone) as other specific pods.

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: web-frontend
spec:
  containers:
  - name: nginx
    image: nginx
  affinity:
    podAffinity:
      requiredDuringSchedulingIgnoredDuringExecution:
      - labelSelector:
          matchExpressions:
          - key: app
            operator: In
            values:
            - cache
        topologyKey: kubernetes.io/hostname
        # Schedule on same node as pods with app=cache label
```

### Pod Anti-Affinity (Spread Pods)

Avoid scheduling pods on the same node (or zone) as other specific pods.

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: web-app
  labels:
    app: web
spec:
  containers:
  - name: webapp
    image: webapp:1.0
  affinity:
    podAntiAffinity:
      requiredDuringSchedulingIgnoredDuringExecution:
      - labelSelector:
          matchExpressions:
          - key: app
            operator: In
            values:
            - web
        topologyKey: kubernetes.io/hostname
        # Don't schedule on same node as other web pods
```

### Complete Example: Web App with Cache

```yaml
# Cache deployment
apiVersion: apps/v1
kind: Deployment
metadata:
  name: redis-cache
spec:
  replicas: 3
  selector:
    matchLabels:
      app: redis
      tier: cache
  template:
    metadata:
      labels:
        app: redis
        tier: cache
    spec:
      containers:
      - name: redis
        image: redis:6
      # Spread redis pods across nodes
      affinity:
        podAntiAffinity:
          requiredDuringSchedulingIgnoredDuringExecution:
          - labelSelector:
              matchExpressions:
              - key: app
                operator: In
                values:
                - redis
            topologyKey: kubernetes.io/hostname
---
# Web app deployment
apiVersion: apps/v1
kind: Deployment
metadata:
  name: webapp
spec:
  replicas: 5
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
      affinity:
        # Place near cache pods
        podAffinity:
          preferredDuringSchedulingIgnoredDuringExecution:
          - weight: 100
            podAffinityTerm:
              labelSelector:
                matchExpressions:
                - key: app
                  operator: In
                  values:
                  - redis
              topologyKey: kubernetes.io/hostname
        # Spread web pods
        podAntiAffinity:
          preferredDuringSchedulingIgnoredDuringExecution:
          - weight: 100
            podAffinityTerm:
              labelSelector:
                matchExpressions:
                - key: app
                  operator: In
                  values:
                  - webapp
              topologyKey: kubernetes.io/hostname
```

### Topology Keys

Common topology keys:

```yaml
# Same node
topologyKey: kubernetes.io/hostname

# Same zone
topologyKey: topology.kubernetes.io/zone

# Same region
topologyKey: topology.kubernetes.io/region

# Custom label
topologyKey: custom-label-key
```

## Taints and Tolerations

Taints repel pods from nodes. Tolerations allow pods to tolerate (ignore) taints.

**Use Cases:**
- Dedicate nodes to specific workloads
- Prevent pods from scheduling on special nodes
- Evict pods from nodes with problems

### Tainting Nodes

```bash
# Add taint to node
kubectl taint nodes node1 key=value:NoSchedule
kubectl taint nodes node1 key=value:NoExecute
kubectl taint nodes node1 key=value:PreferNoSchedule

# Examples
kubectl taint nodes node1 gpu=true:NoSchedule
kubectl taint nodes node2 dedicated=special:NoSchedule
kubectl taint nodes node3 disk=ssd:PreferNoSchedule

# View taints
kubectl describe node node1 | grep Taints

# Remove taint (add minus sign at end)
kubectl taint nodes node1 gpu=true:NoSchedule-
```

### Taint Effects

1. **NoSchedule**
   - New pods won't be scheduled on the node
   - Existing pods remain

2. **PreferNoSchedule**
   - Soft version of NoSchedule
   - Try to avoid scheduling, but not guaranteed

3. **NoExecute**
   - New pods won't be scheduled
   - Existing pods without toleration are evicted

### Adding Tolerations to Pods

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: gpu-pod
spec:
  containers:
  - name: cuda-app
    image: cuda-app:1.0
  tolerations:
  - key: "gpu"
    operator: "Equal"
    value: "true"
    effect: "NoSchedule"
```

### Toleration Operators

#### Equal Operator (Key and Value Match)

```yaml
tolerations:
- key: "dedicated"
  operator: "Equal"
  value: "gpu"
  effect: "NoSchedule"
```

#### Exists Operator (Only Key Matches)

```yaml
tolerations:
- key: "dedicated"
  operator: "Exists"
  effect: "NoSchedule"
```

#### Tolerate All Taints

```yaml
tolerations:
- operator: "Exists"
  # Tolerates all taints
```

### Examples

#### Dedicated GPU Node

```bash
# Taint GPU node
kubectl taint nodes gpu-node1 gpu=true:NoSchedule
```

```yaml
# Pod that can use GPU node
apiVersion: v1
kind: Pod
metadata:
  name: ml-training
spec:
  containers:
  - name: training
    image: ml-trainer:1.0
  tolerations:
  - key: "gpu"
    operator: "Equal"
    value: "true"
    effect: "NoSchedule"
  nodeSelector:
    gpu: "true"  # Also ensure it schedules on GPU node
```

#### Dedicated Master Nodes

```bash
# Master nodes typically have this taint (automatically added)
kubectl describe node master | grep Taints
# Taints: node-role.kubernetes.io/master:NoSchedule
```

```yaml
# DaemonSet that runs on masters too
apiVersion: apps/v1
kind: DaemonSet
metadata:
  name: monitoring-agent
spec:
  selector:
    matchLabels:
      app: monitoring
  template:
    metadata:
      labels:
        app: monitoring
    spec:
      tolerations:
      - key: node-role.kubernetes.io/master
        operator: Exists
        effect: NoSchedule
      containers:
      - name: agent
        image: monitoring-agent:1.0
```

#### Node Problem (NoExecute)

```bash
# Mark node as having disk issues
kubectl taint nodes node1 disk-pressure=true:NoExecute
```

```yaml
# Pod with toleration and grace period
apiVersion: v1
kind: Pod
metadata:
  name: webapp
spec:
  containers:
  - name: webapp
    image: webapp:1.0
  tolerations:
  - key: "disk-pressure"
    operator: "Exists"
    effect: "NoExecute"
    tolerationSeconds: 300  # Tolerate for 5 minutes, then evict
```

## Complete Scheduling Example

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: production-app
spec:
  replicas: 5
  selector:
    matchLabels:
      app: production-app
  template:
    metadata:
      labels:
        app: production-app
        tier: frontend
    spec:
      containers:
      - name: app
        image: myapp:1.0
        resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "1000m"

      # Node affinity: Require production nodes with SSD
      affinity:
        nodeAffinity:
          requiredDuringSchedulingIgnoredDuringExecution:
            nodeSelectorTerms:
            - matchExpressions:
              - key: environment
                operator: In
                values:
                - production
              - key: disktype
                operator: In
                values:
                - ssd
          preferredDuringSchedulingIgnoredDuringExecution:
          - weight: 100
            preference:
              matchExpressions:
              - key: instance-size
                operator: In
                values:
                - large
                - xlarge

        # Pod anti-affinity: Spread across nodes
        podAntiAffinity:
          preferredDuringSchedulingIgnoredDuringExecution:
          - weight: 100
            podAffinityTerm:
              labelSelector:
                matchExpressions:
                - key: app
                  operator: In
                  values:
                  - production-app
              topologyKey: kubernetes.io/hostname

      # Tolerations: Can run on dedicated nodes
      tolerations:
      - key: "dedicated"
        operator: "Equal"
        value: "production"
        effect: "NoSchedule"
```

## Common Patterns

### 1. High Availability (Spread Across Zones)

```yaml
affinity:
  podAntiAffinity:
    requiredDuringSchedulingIgnoredDuringExecution:
    - labelSelector:
        matchExpressions:
        - key: app
          operator: In
          values:
          - myapp
      topologyKey: topology.kubernetes.io/zone
```

### 2. Co-locate Cache and App

```yaml
affinity:
  podAffinity:
    requiredDuringSchedulingIgnoredDuringExecution:
    - labelSelector:
        matchExpressions:
        - key: app
          operator: In
          values:
          - cache
      topologyKey: kubernetes.io/hostname
```

### 3. GPU Nodes Only

```bash
kubectl taint nodes gpu-node gpu=true:NoSchedule
kubectl label nodes gpu-node gpu=true
```

```yaml
spec:
  nodeSelector:
    gpu: "true"
  tolerations:
  - key: "gpu"
    operator: "Equal"
    value: "true"
    effect: "NoSchedule"
```

### 4. Spot Instances (with Toleration)

```bash
kubectl taint nodes spot-node instance-type=spot:NoSchedule
```

```yaml
tolerations:
- key: "instance-type"
  operator: "Equal"
  value: "spot"
  effect: "NoSchedule"
```

## Best Practices

1. **Use node affinity over nodeSelector**: More flexible and expressive
2. **Combine affinity rules**: Use both required and preferred
3. **Spread critical workloads**: Use anti-affinity for HA
4. **Label nodes consistently**: Use standard labels
5. **Document taints**: Clearly label why nodes are tainted
6. **Use soft affinity**: Prefer "preferred" over "required" when possible
7. **Set toleration seconds**: For NoExecute taints
8. **Test scheduling rules**: Verify pods land on expected nodes
9. **Monitor node capacity**: Ensure enough resources available
10. **Use topology keys wisely**: Consider zone vs node spreading

## Troubleshooting

### Pod Not Scheduling

```bash
# Check pod status
kubectl get pods
kubectl describe pod <pod-name>

# Look for scheduling errors
kubectl get events --sort-by='.lastTimestamp'

# Common issues:
# - No nodes match node selector
# - No nodes satisfy affinity rules
# - All nodes have taints without tolerations
# - Insufficient resources
```

### Debug Node Affinity

```bash
# Check node labels
kubectl get nodes --show-labels
kubectl get nodes -L disktype,environment

# Verify pod affinity requirements
kubectl get pod <pod-name> -o yaml | grep -A 20 affinity

# Check which nodes match
kubectl get nodes -l disktype=ssd
```

### Debug Taints and Tolerations

```bash
# View node taints
kubectl describe nodes | grep Taints

# Check specific node
kubectl describe node <node-name> | grep -A 5 Taints

# Verify pod tolerations
kubectl get pod <pod-name> -o yaml | grep -A 10 tolerations
```

### Debugging Commands

```bash
# See where pod is scheduled
kubectl get pod <pod-name> -o wide

# Check scheduler logs
kubectl logs -n kube-system kube-scheduler-<pod>

# View all node labels
kubectl get nodes --show-labels

# Check pod events
kubectl describe pod <pod-name> | grep -A 20 Events
```

## Quick Reference

```bash
# Node Selector
kubectl label nodes node1 disktype=ssd
# Use in pod: nodeSelector: {disktype: ssd}

# Taints
kubectl taint nodes node1 key=value:NoSchedule
kubectl taint nodes node1 key=value:NoSchedule-  # Remove

# View node info
kubectl get nodes --show-labels
kubectl describe node node1 | grep Taints

# Check pod placement
kubectl get pods -o wide
kubectl describe pod <pod-name>

# Debug scheduling
kubectl get events --sort-by='.lastTimestamp'
```

## Next Steps

- Learn about Pod Priority and Preemption
- Explore DaemonSets for node-specific workloads
- Study Resource Quotas and Limit Ranges
- Understand Cluster Autoscaling
- Practice with multi-zone clusters
