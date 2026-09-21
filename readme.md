# Maven Build Cache Proxy — Chunk 1

Minimal Spring Boot implementation of the HTTP/storage boundary.

## Requirements

- JDK 25
- Maven 3.6.3+

## Run tests

```bash
mvn test
```

## Run locally

```bash
mvn spring-boot:run
```

Or run `MavenBuildCacheProxyApplication` directly from IntelliJ.

The service listens on port 8080 by default.

## Manual HTTP check

Store binary/text content:

```bash
curl -i -X PUT \
  -H 'Content-Type: application/octet-stream' \
  --data-binary 'hello-cache' \
  http://localhost:8080/cache/com/example/demo/hash/data
```

Expected response: `204 No Content`.

Retrieve it:

```bash
curl -i \
  http://localhost:8080/cache/com/example/demo/hash/data
```

Expected response: `200 OK` with `hello-cache` as the body.

Unknown keys return `404 Not Found`.

## Architecture in this chunk

```text
HTTP GET/PUT
    -> CacheController
    -> CacheStorage
    -> InMemoryCacheStorage
```

`CacheStorage` is intentionally the boundary that Chunk 2 will use for `S3CacheStorage`.

## Deliberately not included yet

- AWS SDK
- S3
- LocalStack
- Docker
- Maven Build Cache test client
- AWS deployment
