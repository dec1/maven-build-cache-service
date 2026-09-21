# Development Environment

This setup keeps the project-specific JDK and Maven inside Docker.

## Host requirements

Only these are required on the host:

* Docker with Docker Compose
* IntelliJ IDEA

A host JDK is optional. IntelliJ can use it for local code analysis if already installed, but Maven builds and tests run inside Docker.

The development container uses:

* Java 25 (Eclipse Temurin)
* Maven 3.9.16

`Eclipse Temurin` is a JDK distribution. It does not mean you are using the Eclipse IDE.

## Project files

The relevant files are in the project root:

```text
project/
├── pom.xml
├── Dockerfile.dev
├── compose.yaml
├── .dockerignore
└── src/
```

## Start the development container

From the project root:

```bash
docker compose up -d --build dev
```

Check the tools inside it:

```bash
docker compose exec dev java -version
docker compose exec dev mvn -version
```

Normally this container stays running while you work.

## Development workflow

The project directory is bind-mounted into `/workspace`.

Changes made in IntelliJ are therefore immediately visible inside the container.

Changing Java source code does **not** require rebuilding the Docker image.

```text
Changed Java source code?
|
+-- Want to run the tests?
|   |
|   +--> docker compose exec dev mvn test
|
+-- Want to run the application normally?
|   |
|   +--> Stop an already-running instance with Ctrl+C
|   |
|   +--> docker compose exec dev mvn spring-boot:run
|
+-- Want to debug with IntelliJ breakpoints?
    |
    +--> Stop an already-running instance with Ctrl+C
    |
    +--> Start Spring Boot with remote debugging:
         docker compose exec dev mvn spring-boot:run \
           '-Dspring-boot.run.jvmArguments=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005'
    |
    +--> Start the IntelliJ "Remote JVM Debug" configuration
         Host: localhost
         Port: 5005
```

Maven automatically recompiles source files that have changed.

## Run tests

For the normal edit/test cycle:

```bash
docker compose exec dev mvn test
```

This:

```text
compiles main source
    ↓
compiles test source
    ↓
runs the tests
```

Use this after normal source-code changes.

## Full project verification

For a more complete project check:

```bash
docker compose exec dev mvn verify
```

This runs the Maven lifecycle through the `verify` phase:

```text
compile
    ↓
compile tests
    ↓
run tests
    ↓
package application
    ↓
run additional verification steps configured in pom.xml
```

Use `mvn test` during normal development.

Use `mvn verify` before considering a chunk or change complete.

Later, integration tests such as the LocalStack tests may also be attached to the verification lifecycle.

## Run the application normally

Start the service with:

```bash
docker compose exec dev mvn spring-boot:run
```

The service is then available at:

```text
http://localhost:8080
```

Stop it with:

```text
Ctrl+C
```

The development container itself remains running.

After changing source code, restart `spring-boot:run` to run the changed application.

## Run with IntelliJ remote debugging

Start Spring Boot inside the container with JVM debugging enabled:

```bash
docker compose exec dev mvn spring-boot:run \
  '-Dspring-boot.run.jvmArguments=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005'
```

Then configure IntelliJ:

1. Open **Run -> Edit Configurations**.
2. Click **+**.
3. Choose **Remote JVM Debug**.
4. Set:

```text
Host: localhost
Port: 5005
```

5. Save the configuration.
6. Start the Spring Boot command above.
7. Start the IntelliJ Remote JVM Debug configuration.
8. Set and use breakpoints normally.

IntelliJ and the container see the same source files because the project directory is bind-mounted into `/workspace`.

If you want the JVM to wait for IntelliJ before executing application code, change:

```text
suspend=n
```

to:

```text
suspend=y
```

After changing source code, stop the running application and restart the debug-enabled `spring-boot:run` command.

## Manual GET/PUT check

With the application running:

```bash
curl -i -X PUT \
  -H 'Content-Type: application/octet-stream' \
  --data-binary 'hello-cache' \
  http://localhost:8080/cache/com/example/demo/hash/data
```

Then:

```bash
curl -i \
  http://localhost:8080/cache/com/example/demo/hash/data
```

The response body should contain:

```text
hello-cache
```

## Other file changes

### Changed `pom.xml`

No Docker image rebuild is required.

Run whichever Maven command you need:

```bash
docker compose exec dev mvn test
```

or:

```bash
docker compose exec dev mvn verify
```

or:

```bash
docker compose exec dev mvn spring-boot:run
```

Maven resolves changed dependencies automatically.

### Changed `Dockerfile.dev`

Rebuild the development image:

```bash
docker compose up -d --build dev
```

### Changed `compose.yaml`

Recreate/update the development container:

```bash
docker compose up -d dev
```

## Stop the development environment

Stop and remove the development container:

```bash
docker compose down
```

The Maven dependency cache is retained in the Docker named volume `maven-cache`.

To remove that cache as well:

```bash
docker compose down -v
```

## Typical daily workflow

```text
Start once:
    docker compose up -d dev

Edit:
    IntelliJ

Quick test:
    docker compose exec dev mvn test

Run normally:
    docker compose exec dev mvn spring-boot:run

Or debug:
    start Spring Boot with JDWP on port 5005
    then attach IntelliJ Remote JVM Debug

Before considering the work complete:
    docker compose exec dev mvn verify
```

No Maven installation is required on the host.
