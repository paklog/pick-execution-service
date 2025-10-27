# Pick Execution Service Helm Chart

Pick execution with TSP path optimization

## Installation

```bash
helm install pick-execution-service .
```

## Configuration

See `values.yaml` for all configuration options.

### Key Configuration

```yaml
image:
  repository: paklog/pick-execution-service
  tag: latest

service:
  port: 8102
  managementPort: 8081

resources:
  limits:
    cpu: 1000m
    memory: 1Gi
  requests:
    cpu: 500m
    memory: 512Mi

autoscaling:
  enabled: true
  minReplicas: 2
  maxReplicas: 10
```

### Database Configuration

MongoDB is enabled by default.



### Observability

- **Metrics**: Prometheus metrics available at `/actuator/prometheus`
- **Tracing**: OpenTelemetry traces sent to Tempo
- **Logging**: Structured logs sent to Loki

## Uninstallation

```bash
helm uninstall pick-execution-service
```

## Maintainers

- Paklog Team
