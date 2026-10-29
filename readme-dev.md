# Development Runbook

## 1. Initialize development environment

Run from the project root after checkout, or after changing `Dockerfile.dev`:

```bash
docker compose up -d --build dev
```

Keep the `dev` container running while working. Commands below run Maven inside it. For host-based Maven, use JDK 25 and Maven; the integration tests also need Docker.

## 2. Run application

```bash
docker compose exec dev mvn spring-boot:run
```

The default backend is in-memory. See [S3 configuration](readme-s3.md) to run against LocalStack or Amazon S3.

| Endpoint | URL |
| --- | --- |
| Cache API | `http://localhost:8080/cache/{key}` |
| Swagger UI | `http://localhost:8080/swagger-ui/index.html` |
| Health | `http://localhost:8080/actuator/health` |

Stop with `Ctrl+C`. Rerun the Maven command after Java or `pom.xml` changes; the bind mount makes edits visible without rebuilding the image.

If a leftover application process occupies port `8080`, `docker compose restart dev` resets the container and its processes. Then rerun Maven. See [Docker lifecycle](readme-docker.md) for configuration changes and cleanup.

## 3. Run tests

```bash
docker compose exec dev mvn test
```

Use `mvn clean test` in the same command to discard compiled output first, or `mvn verify` to run through packaging and verification. Neither requires removing containers or the Maven cache.

| Tests | Boundary covered |
| --- | --- |
| `InMemoryCacheStorageTest` | Byte preservation, missing keys, existence and defensive copies |
| `CacheControllerTest` | MockMvc GET/PUT/HEAD with memory storage |
| `S3HealthIndicatorTest` | Bucket-check success/failure with a mocked S3 client |
| `S3CacheStorageIntegrationTest` | Spring-configured storage against LocalStack: PUT/GET and existence checks |
| `CacheControllerS3IntegrationTest` | MockMvc PUT/GET through S3 storage: all byte values and a 1 MiB payload |

Both integration classes start their own LocalStack container and create their own bucket. They do not use the Compose `localstack` service. Docker must be running and accessible to the test JVM; Compose mounts the host Docker socket into `dev`.

## 4. Remote debugging with IntelliJ

Create a **Remote JVM Debug** configuration with host `localhost` and port `5005`. Use one debug session at a time.

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
