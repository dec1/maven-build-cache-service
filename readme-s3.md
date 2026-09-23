# S3 / LocalStack development and integration tests

This file documents the S3-backed `CacheStorage` implementation and its automated LocalStack integration tests.

## Storage selection

The application keeps the in-memory implementation as the default:

```text
cache.storage.type=memory
```

This means the application and the HTTP/controller tests can run without S3 or LocalStack.

Select S3 explicitly with:

```bash
export CACHE_STORAGE_TYPE=s3
```

When `s3` is selected, these settings are required:

```bash
export CACHE_STORAGE_S3_BUCKET=maven-build-cache
export CACHE_STORAGE_S3_REGION=eu-central-1
```

## S3 configuration

The S3 client supports two modes without changing application code.

### LocalStack / another S3-compatible endpoint

Set an endpoint override and static test credentials:

```bash
export CACHE_STORAGE_TYPE=s3
export CACHE_STORAGE_S3_ENDPOINT=http://localhost:4566
export CACHE_STORAGE_S3_BUCKET=maven-build-cache
export CACHE_STORAGE_S3_REGION=eu-central-1
export CACHE_STORAGE_S3_ACCESS_KEY=test
export CACHE_STORAGE_S3_SECRET_KEY=test
```

The application does not contain LocalStack-specific behaviour. The endpoint override is normal AWS SDK configuration.

These variables are for running the application itself against an already-running S3-compatible endpoint. They are not needed by `S3CacheStorageIntegrationTest`, because that test creates its own LocalStack container and AWS SDK client.

### Amazon S3

Leave the endpoint and static credentials unset:

```bash
export CACHE_STORAGE_TYPE=s3
export CACHE_STORAGE_S3_BUCKET=maven-build-cache
export CACHE_STORAGE_S3_REGION=eu-central-1
unset CACHE_STORAGE_S3_ENDPOINT
unset CACHE_STORAGE_S3_ACCESS_KEY
unset CACHE_STORAGE_S3_SECRET_KEY
```

The AWS SDK then uses the normal Amazon S3 endpoint and its default credential provider chain, for example a workload IAM role.

AWS deployment is not covered by this document.

## Automated LocalStack integration tests

LocalStack is **not installed separately**. Testcontainers pulls and starts LocalStack in Docker for the integration test and removes the container afterwards.

The test deliberately pins:

```text
localstack/localstack:4.14.0
```

LocalStack 4.14.0 is the final pre-2026.3 Community release. It provides the basic S3 behaviour needed here without requiring a LocalStack account or `LOCALSTACK_AUTH_TOKEN`. The image is pinned instead of using `latest` so the test environment cannot silently change.

### What the integration test covers

`S3CacheStorageIntegrationTest`:

1. starts LocalStack with S3 enabled;
2. creates a dedicated test bucket;
3. verifies that `S3CacheStorage.put` stores the exact bytes in S3;
4. verifies a binary PUT/GET round trip;
5. verifies that a missing S3 object becomes `Optional.empty()`;
6. lets Testcontainers remove the LocalStack container when the test run finishes.

The integration test talks directly to `S3CacheStorage`. HTTP/controller behaviour remains covered separately by `CacheControllerTest`, preserving the boundary:

```text
HTTP/controller
    -> CacheStorage
    -> S3CacheStorage
    -> S3-compatible endpoint
```

## Test output

The tests use human-readable JUnit display names and a small test-only reporter so the normal Maven output shows each tested behaviour explicitly, for example:

```text
[TEST] PASS  HTTP: PUT then GET preserves binary content
[TEST] PASS  HTTP: unknown cache key returns 404
[TEST] PASS  Memory storage: stores and retrieves binary content
[TEST] PASS  Memory storage: unknown key returns empty
[TEST] PASS  Memory storage: stored data cannot be mutated through array references
[TEST] PASS  S3: PUT stores exact bytes in S3
[TEST] PASS  S3: GET retrieves exact bytes
[TEST] PASS  S3: missing object returns empty
```

Maven/Surefire still prints its normal aggregate summary as well.

## Running the tests

### Maven on the host

Requirement: Docker must be installed and the current user must be able to use it.

Run:

```bash
mvn test
```

No LocalStack process, installation, account, token, endpoint variable or AWS credential variable is required for the integration test.

### Maven inside the development container

The normal command is:

```bash
docker compose exec dev mvn test
```

In this case Testcontainers itself runs inside the `dev` container, but it must still be able to ask Docker to start the separate LocalStack container.

The `dev` service therefore needs access to a Docker daemon. With the host-socket setup used by this project, `compose.yaml` contains:

```yaml
services:
  dev:
    volumes:
      - .:/workspace
      - maven-cache:/root/.m2
      - /var/run/docker.sock:/var/run/docker.sock
```

This does **not** run LocalStack inside the `dev` container. The flow is:

```text
dev container
    -> Maven/JUnit
    -> Testcontainers
    -> host Docker daemon
    -> separate LocalStack container
```

After changing the `dev` service definition, apply the Compose configuration before testing:

```bash
docker compose up -d dev
```

Then run:

```bash
docker compose exec dev mvn test
```

### IntelliJ

Run `S3CacheStorageIntegrationTest` normally from IntelliJ while Docker is running. No LocalStack-specific environment variables are required.

If IntelliJ itself is running on the host, Testcontainers connects to the host Docker daemon in the normal way. If the IDE/test JVM runs inside a container, that container likewise needs access to a Docker daemon.

## Why LocalStack is pinned

Current LocalStack releases use an authenticated distribution model. These tests only need basic S3 PUT/GET semantics, so requiring a LocalStack account and token would add setup without increasing the test coverage needed here.

The tradeoff is that `4.14.0` is intentionally an older fixed test dependency. If future tests need behaviour unavailable there, the LocalStack version/authentication choice should be revisited explicitly rather than silently changing the image tag.
