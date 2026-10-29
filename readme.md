# Maven Build Cache S3 Proxy

## Motivation

[Maven](https://maven.apache.org/what-is-maven.html) automates Java builds, including compilation, tests and packaging. Large builds can take a long time, and developers and continuous integration (CI) jobs may repeat work already completed by another build.

The [Apache Maven Build Cache Extension](https://maven.apache.org/extensions/maven-build-cache-extension/) reduces repeated work by storing build outputs and reusing them when the configured inputs and execution checks match. It can use a local cache; an optional shared remote cache lets different machines reuse those results, including CI workers that start with an empty local cache.

Sharing results requires a backend that stores and serves cache entries. This project provides an HTTP service for that role, backed by S3: the extension uploads and downloads entries over HTTP, and this service stores and retrieves their bytes using the S3 SDK. S3 keeps the entries independently of the service process and build workers.

Users must enable and configure the extension in the project being built and point its remote cache at this service; running the service alone does not enable caching. See Apache's [remote cache setup](https://maven.apache.org/extensions/maven-build-cache-extension/remote-cache.html).

## Current architecture

`CacheController` handles GET/PUT/HEAD and calls `CacheStorage`. Configuration selects one implementation:

| Storage | Selection | Stored state |
| --- | --- | --- |
| `InMemoryCacheStorage` | `cache.storage.type=memory` (default) | Per-process byte arrays; lost when the process stops |
| `S3CacheStorage` | `cache.storage.type=s3` | Objects in an existing bucket; each cache key becomes the object key |

The service stores opaque bytes. It does not calculate Maven cache keys or decide whether a build can reuse an entry; those decisions belong to the Maven client.

## Requirements

For local development, use Docker with Docker Compose. The development container provides JDK 25 and Maven; IntelliJ is optional for remote debugging.

* [Development and tests](readme-dev.md)
* [Docker container lifecycle](readme-docker.md)
* [S3 configuration and LocalStack](readme-s3.md)
* [Health and logging](readme-monitoring.md)

## HTTP API

`{key}` is the path after `/cache/` and may contain slashes, for example `com/example/demo/hash/data`. The controller removes the leading slash before calling storage.

| Request | Result |
| --- | --- |
| `PUT /cache/{key}` with a binary body (`Content-Type: application/octet-stream`) | Stores/replaces the bytes; returns `204 No Content` |
| `GET /cache/{key}` | Returns `200 OK` with `application/octet-stream`, or `404 Not Found` for a missing entry |
| `HEAD /cache/{key}` | Returns `200 OK` for an existing entry or `404 Not Found`; no response body |

`GET /cache/hello` is a separate diagnostic route returning the text `hello`; avoid using `hello` as a cache key.

Swagger UI: `http://localhost:8080/swagger-ui/index.html`.

## Current scope

S3 storage, bucket health checks and LocalStack integration tests are implemented. The tests exercise storage and HTTP handling through MockMvc, but do not run a Maven Build Cache Extension client or verify real network transport.

The supplied project has no production service image, AWS deployment, application authentication or cache eviction. PUT and GET buffer complete objects in JVM memory, including with S3 storage.


