# Maven Build Cache S3 Proxy

Small Java HTTP service intended to act as a gateway between the Maven Build Cache Extension and an S3-compatible storage backend.

The project is being implemented incrementally. The current state is **Chunk 1**: the HTTP service and storage abstraction, using an in-memory storage implementation.

## Current architecture

```text
HTTP GET / PUT
      |
      v
CacheController
      |
      v
CacheStorage
      |
      v
InMemoryCacheStorage
```

Later, `InMemoryCacheStorage` will be supplemented/replaced by an S3-backed implementation:

```text
CacheStorage
      |
      v
S3CacheStorage
      |
      v
LocalStack / Amazon S3
```

## Requirements

For local development:

* Docker with Docker Compose
* IntelliJ IDEA

Maven does **not** need to be installed on the host. The development container provides the required JDK and Maven installation.

See [`DOCKER_DEVELOPMENT.md`](DOCKER_DEVELOPMENT.md) for the complete development, testing and IntelliJ debugging workflow.

## HTTP API

### Store an object

```http
PUT /cache/{key}
Content-Type: application/octet-stream
```

Example key:

```text
/cache/com/example/demo/hash/data
```

A successful PUT returns:

```text
204 No Content
```

### Retrieve an object

```http
GET /cache/{key}
```

If the object exists:

```text
200 OK
Content-Type: application/octet-stream
```

If it does not exist:

```text
404 Not Found
```

## Source layout

```text
src/
├── main/java/de/example/mavenbuildcacheproxy
│   ├── MavenBuildCacheProxyApplication.java
│   ├── http/
│   │   └── CacheController.java
│   └── storage/
│       ├── CacheStorage.java
│       └── InMemoryCacheStorage.java
└── test/java/de/example/mavenbuildcacheproxy
    ├── http/
    │   └── CacheControllerTest.java
    └── storage/
        └── InMemoryCacheStorageTest.java
```

## Current scope

Chunk 1 covers:

* Java/Maven Spring Boot application
* minimal HTTP GET/PUT API
* `CacheStorage` abstraction
* in-memory storage implementation
* automated tests
* containerized development environment
* IntelliJ remote debugging

Not yet included:

* AWS SDK
* S3 storage
* LocalStack
* production service Docker image
* Maven Build Cache test client
* AWS deployment
