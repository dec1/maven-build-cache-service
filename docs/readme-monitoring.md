# Health and logging

## Health endpoint

```text
GET /actuator/health
```

With in-memory storage, this reports Spring Boot's application health without an S3 check.

With S3 storage, `S3HealthIndicator` also calls `HeadBucket` for the configured bucket. A failed call marks that component `DOWN` and makes overall health `DOWN` (HTTP 503 by default). A successful bucket check does not verify object read/write permissions.

A load balancer must be configured separately to poll this endpoint; no deployment configuration is supplied. For S3 permissions, see [S3 configuration](readme-s3.md).

Only `health` is exposed through Actuator by default. The project does not include a Prometheus registry or exporter configuration.

## Logging

Spring Boot's default Logback setup writes application logs to the console. No custom logging configuration is supplied.

When running via `docker compose exec dev`, logs appear in the attached terminal. CloudWatch collection would require deployment logging configuration; the Java application does not configure it.

## Local check

With the application running (see [development runbook](readme-dev.md)):

```bash
curl -i http://localhost:8080/actuator/health
```
