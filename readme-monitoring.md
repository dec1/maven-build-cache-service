# Health and logging

## Health endpoint

Spring Boot Actuator exposes:

```text
GET /actuator/health
```

With in-memory storage, this reports the normal Spring application health.

With S3 storage, `S3HealthIndicator` also checks the configured bucket with S3 `HeadBucket`:

```text
/actuator/health
    |
    +-- application running
    |
    +-- configured S3 bucket reachable
```

If the S3 check fails, the overall health status becomes `DOWN`. This allows the deployment load balancer to treat a running application with an unusable S3 backend as unhealthy.

`HeadBucket` requires permission to check the bucket. The AWS task role therefore needs the corresponding bucket-level S3 permission in addition to the object permissions used for cache GET/PUT/HEAD operations.

Only `health` is exposed through Actuator for now:

```properties
management.endpoints.web.exposure.include=health
```

Prometheus metrics are intentionally not enabled yet. If the optional Alloy -> Grafana Cloud setup is added later, add the Prometheus Micrometer registry and expose `/actuator/prometheus` at that point.

## Logging

No Log4j or Log4j2 dependency/configuration is required.

Spring Boot's web starter already provides its default logging setup, which uses Logback and writes to the console. The application should therefore keep normal application logs on stdout/stderr:

```text
Spring application
    |
    | stdout/stderr
    v
ECS awslogs
    |
    v
CloudWatch Logs
```

The ECS/Fargate deployment configures the `awslogs` driver and CloudWatch Log Group. Nothing CloudWatch-specific is required in the Java application.

Do not add file logging inside the container unless there is a specific requirement for it.

## Local check

Start the application:

```bash
docker compose exec dev mvn spring-boot:run
```

Then check health:

```bash
curl -i http://localhost:8080/actuator/health
```

For the default in-memory backend, the response should report `UP`.

When running with the S3 backend, the response is `UP` only when the application can also reach the configured S3 bucket.

## Tests

Run:

```bash
docker compose exec dev mvn test
```

`S3HealthIndicatorTest` verifies:

```text
reachable S3 bucket -> UP
failed S3 check     -> DOWN
```

The existing LocalStack integration tests continue to verify the real S3 storage path separately.
