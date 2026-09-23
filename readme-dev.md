# Development Runbook

## Docker command semantics

_See also: [readme-docker](readme-docker.md)_


## 1. Initialize development environment

Run after checkout, or after changing `Dockerfile.dev`:

```bash
docker compose up -d --build dev
```

Check tools:

```bash
docker compose exec dev java -version
docker compose exec dev mvn -version
```

Keep the `dev` container running while working.

---

## 2. Run application

### 2.1 Normally

```bash
docker compose exec dev mvn spring-boot:run
```

Service:

```text
http://localhost:8080
```

Stop with `Ctrl+C`.

If the next start reports that port `8080` is already in use:

```bash
docker compose restart dev
```

Then repeat **2.1**.

### 2.2 With IntelliJ debugger

Start the application with remote debugging enabled:

```bash
docker compose exec dev mvn spring-boot:run \
  '-Dspring-boot.run.jvmArguments=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005'
```

Then in IntelliJ start the existing **Remote JVM Debug** configuration:

```text
Host: localhost
Port: 5005
```

Set breakpoints in the Java source normally.

If port `8080` is still occupied:

```bash
docker compose restart dev
```

Then repeat **2.2**.

---

## 3. Run tests

### 3.1 Quick test run

Use during normal development:

```bash
docker compose exec dev mvn test
```

This compiles changed source/test code and runs the tests.

### 3.2 Debug tests with IntelliJ breakpoints

Make sure no application/debug session is currently using port `5005`.

Start the test JVM suspended and waiting for IntelliJ:

```bash
docker compose exec dev mvn \
  '-Dmaven.surefire.debug=-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005' \
  test
```

Maven will pause before executing the tests.

Then in IntelliJ:

```text
1. Set breakpoint(s) in the test or production Java code.
2. Start the existing Remote JVM Debug configuration.
   Host: localhost
   Port: 5005
3. The tests continue and stop at matching breakpoints.
```

To debug only one test class:

```bash
docker compose exec dev mvn \
  -Dtest=CacheControllerTest \
  '-Dmaven.surefire.debug=-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005' \
  test
```

To debug one test method:

```bash
docker compose exec dev mvn \
  '-Dtest=CacheControllerTest#putThenGetRoundTripPreservesBinaryContent' \
  '-Dmaven.surefire.debug=-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005' \
  test
```

### 3.3 Full Maven verification

Use before considering a change/chunk complete:

```bash
docker compose exec dev mvn verify
```

This runs the Maven lifecycle through `verify`: compile, test compilation, tests, packaging, and any additional verification steps configured in `pom.xml`.

---

## 4. After editing files

### 4.1 Changed Java source

No Docker rebuild required.

```text
run normally       -> 2.1
debug application  -> 2.2
run tests          -> 3.1
debug tests        -> 3.2
full verification  -> 3.3
```

### 4.2 Changed `pom.xml`

No Docker rebuild required. Maven resolves changed dependencies automatically.

```text
run normally       -> 2.1
debug application  -> 2.2
run tests          -> 3.1
debug tests        -> 3.2
full verification  -> 3.3
```

### 4.3 Changed `Dockerfile.dev`

```bash
docker compose up -d --build dev
```

Then continue from **2** or **3**.

### 4.4 Changed `compose.yaml`

```bash
docker compose up -d dev
```

Then continue from **2** or **3**.

---

## 5. Cleanup

Normal Java or `pom.xml` changes do **not** require cleanup.

Stop/remove the development container when you want to tear down the environment:

```bash
docker compose down
```

The Maven cache is retained.

To also delete the Maven cache:

```bash
docker compose down -v
```

After `docker compose down`:

```bash
docker compose up -d dev
```

If `Dockerfile.dev` changed:

```bash
docker compose up -d --build dev
```
