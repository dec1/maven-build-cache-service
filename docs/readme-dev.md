# Development Runbook

## 1. Choose where Maven runs

Run commands from the project root. The application uses in-memory storage by default; running it does not require Docker or S3.

| Maven runs in | Required tools | Docker's role |
| --- | --- | --- |
| Host | JDK 25 and Maven | Needed only when running the S3 integration tests or a local LocalStack service |
| Development container (`dev`) | Docker with Docker Compose | Supplies JDK 25 and Maven without installing them on the host; also runs LocalStack for integration tests |

For the container option, Compose uses `Dockerfile.dev` to build the image and creates a container for the `dev` service defined in `../compose.yaml`:

```bash
docker compose up -d --build dev
```

Keep `dev` running while working. This starts the tool environment, not the application. The checkout is bind-mounted into it, and Maven dependencies are retained in a named volume. Rebuild after changing `Dockerfile.dev`; see [environment changes and cleanup](readme-docker.md) for other changes and cleanup.

## 2. Run application

Choose the command for your Maven environment:

| Maven runs in | Command |
| --- | --- |
| Host | `mvn spring-boot:run` |
| Development container | `docker compose exec dev mvn spring-boot:run` |

The default backend is in-memory. See [S3 configuration](readme-s3.md) to run against LocalStack or Amazon S3.

| Endpoint | URL |
| --- | --- |
| Cache API | `http://localhost:8080/cache/{key}` |
| Swagger UI | `http://localhost:8080/swagger-ui/index.html` |
| Health | `http://localhost:8080/actuator/health` |

Stop with `Ctrl+C`. Rerun the Maven command after Java or `../pom.xml` changes. For the container option, the bind mount makes edits visible without rebuilding the image.

If a leftover application process occupies port `8080`, stop that process before rerunning Maven. For an application running inside `dev`, `docker compose restart dev` resets the container and its processes.

## 3. Run tests

Tests start the application components they need; do not start the application separately.

### Full suite (Docker required)

| Maven runs in | Command |
| --- | --- |
| Host | `mvn test` |
| Development container | `docker compose exec dev mvn test` |

Both commands include the S3 integration tests. Those tests use Testcontainers to ask Docker to start separate LocalStack containers, providing an S3 endpoint without an AWS account. Each integration class creates its test bucket and stops its container afterward. They do not use the Compose `localstack` service.

Docker must be running and accessible to the test JVM. For Maven inside `dev`, Compose mounts the host Docker socket so Testcontainers can start sibling containers; LocalStack does not run inside `dev`.

### Tests without Docker

With JDK 25 and Maven on the host, run the memory, MockMvc and mocked-health tests only:

```bash
mvn '-Dtest=InMemoryCacheStorageTest,CacheControllerTest,S3HealthIndicatorTest' test
```

This omits the two S3 integration classes. Use the default in-memory configuration for `CacheControllerTest`.

Use `clean test` instead of `test` to discard compiled output first, or `verify` to run through packaging and verification. Without a test selector, `verify` also runs the full suite and needs Docker. None of these commands requires removing containers or the Maven cache.

### Coverage

| Tests | Boundary covered |
| --- | --- |
| `InMemoryCacheStorageTest` | Byte preservation, missing keys, existence and defensive copies |
| `CacheControllerTest` | MockMvc GET/PUT/HEAD with memory storage |
| `S3HealthIndicatorTest` | Bucket-check success/failure with a mocked S3 client |
| `S3CacheStorageIntegrationTest` | Spring-configured storage against LocalStack: PUT/GET and existence checks |
| `CacheControllerS3IntegrationTest` | MockMvc PUT/GET through S3 storage: all byte values and a 1 MiB payload |

## 4. Remote debugging with IntelliJ

The commands below use the development container. Create a **Remote JVM Debug** configuration with host `localhost` and port `5005`. Use one debug session at a time.

### Application

```bash
docker compose exec dev mvn spring-boot:run \
  '-Dspring-boot.run.jvmArguments=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005'
```

Then attach IntelliJ. The application starts without waiting for the debugger.

### Tests

Start the test JVM suspended and waiting for IntelliJ:

```bash
docker compose exec dev mvn \
  '-Dmaven.surefire.debug=-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005' \
  test
```

Set breakpoints and attach IntelliJ to continue. Add `-Dtest=CacheControllerTest` to select a class, or `'-Dtest=CacheControllerTest#putThenGetRoundTripPreservesBinaryContent'` to select a method.
