# Maven Build Cache Service

[![CI Tests](https://github.com/dec1/maven-build-cache-service/actions/workflows/ci.yml/badge.svg)](https://github.com/dec1/maven-build-cache-service/actions/workflows/ci.yml)
## Motivation

[Maven](https://maven.apache.org/what-is-maven.html) automates Java builds, including compilation, tests and packaging. Large builds can take a long time, and developers and continuous integration (CI) jobs may repeat work already completed by another build.

The [Apache Maven Build Cache Extension](https://maven.apache.org/extensions/maven-build-cache-extension/) reduces repeated work by storing build outputs and reusing them when the configured inputs and execution checks match. It can use a local cache; an optional shared remote cache lets different machines reuse those results, including CI workers that start with an empty local cache.

Sharing results requires a backend that stores and serves cache entries. This project provides that HTTP service with replaceable storage implementations. Its S3 backend supports [Amazon S3](https://aws.amazon.com/s3/), AWS's object storage service, and is tested with LocalStack; in-memory storage supports local development without an external storage service.

## Using the service

The application can run with JDK 25 and Maven on the host or through the `dev` service defined in `compose.yaml`. Docker is needed for `dev` and for the S3 integration tests.

1. Start the service using the [application and test commands](docs/readme-dev.md). The default storage is in-memory; use [S3 configuration](docs/readme-s3.md) for Amazon S3 or LocalStack.
2. [Configure the Maven client](docs/readme-client.md) in the project whose builds should be cached, pointing it at the local or deployed service.

## Reference

* [Architecture, storage choices and HTTP API](docs/readme-arch.md)
* [Maven client configuration](docs/readme-client.md)
* [Running the application and tests](docs/readme-dev.md)
* [Development environment with Docker Compose](docs/readme-docker.md)
* [S3 configuration and LocalStack](docs/readme-s3.md)
* [Health and logging](docs/readme-monitoring.md)
