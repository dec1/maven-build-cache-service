# Chunk 1 development environment

This setup keeps the project-specific JDK and Maven inside Docker.

## Host requirements

Only these are required on the host:

- Docker with Docker Compose
- IntelliJ IDEA

A host JDK is optional. IntelliJ can use it for local code analysis if already installed, but Maven builds and tests below run inside Docker.

The development container uses:

- Java 25 (Eclipse Temurin)
- Maven 3.9.16

`Eclipse Temurin` is a JDK distribution. It does not mean you are using the Eclipse IDE.

## Files

Place these files in the project root, next to `pom.xml`:

```text
project/
├── pom.xml
├── Dockerfile.dev
├── compose.yaml
├── .dockerignore
└── src/
```

## 1. Start the development container

From the project root:

```bash
docker compose up -d --build dev
```

Check the tools inside it:

```bash
docker compose exec dev java -version
docker compose exec dev mvn -version
```

## 2. Run the tests

```bash
docker compose exec dev mvn test
```

For the fuller lifecycle check:

```bash
docker compose exec dev mvn verify
```

The Maven dependency cache is stored in the Docker named volume `maven-cache`.

## 3. Run the Spring Boot application normally

```bash
docker compose exec dev mvn spring-boot:run
```

The service is available on:

```text
http://localhost:8080
```

Stop the application with Ctrl+C. The dev container remains running.

## 4. Run with IntelliJ remote debugging

Run:

```bash
docker compose exec dev mvn spring-boot:run \
  '-Dspring-boot.run.jvmArguments=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005'
```

Then in IntelliJ:

1. Open **Run -> Edit Configurations**.
2. Click **+**.
3. Choose **Remote JVM Debug**.
4. Host: `localhost`
5. Port: `5005`
6. Save the configuration.
7. Start the Spring Boot application with the Docker command above.
8. Start the IntelliJ Remote JVM Debug configuration.
9. Use breakpoints normally.

The project directory is bind-mounted to `/workspace`, so IntelliJ and the container use the same source files.

If you want the JVM to wait for IntelliJ before running application code, change:

```text
suspend=n
```

to:

```text
suspend=y
```

## 5. Manual GET/PUT check

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

## 6. Stop the development environment

```bash
docker compose down
```

This removes the container but retains the Maven cache volume.

To also delete the Maven cache:

```bash
docker compose down -v
```

## Normal workflow

Start once:

```bash
docker compose up -d dev
```

Edit in IntelliJ on the host.

Run tests:

```bash
docker compose exec dev mvn test
```

Run or debug the application from the container when needed.

At this stage no Maven installation is required on the host.
