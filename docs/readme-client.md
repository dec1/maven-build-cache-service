# Maven client configuration

Configure the **project whose builds should be cached**, using Maven 3.9.0 or later. The cache is a Maven extension, declared in `.mvn/extensions.xml` (merge into an existing `<extensions>` element):

```xml
<extensions>
    <extension>
        <groupId>org.apache.maven.extensions</groupId>
        <artifactId>maven-build-cache-extension</artifactId>
        <version>1.3.0</version>
    </extension>
</extensions>
```

In that project's `.mvn/maven-build-cache-config.xml`, enable remote reads and set the service URL:

```xml
<cache xmlns="http://maven.apache.org/BUILD-CACHE-CONFIG/1.4.0">
    <configuration>
        <remote enabled="true">
            <url>http://localhost:8080/cache</url>
        </remote>
    </configuration>
</cache>
```

Use `localhost` when Maven runs on the same host as the locally exposed service. For a deployed service, replace the URL with its reachable address, for example `https://cache.example.org/cache`. A Maven process inside another container needs an address reachable from that container; its `localhost` refers to itself.

Remote uploads are disabled by default. To populate the cache from a designated build, run:

```bash
mvn verify -Dmaven.build.cache.remote.save.enabled=true
```

Other builds can read it with `mvn verify`. Check the extension's logs for uploads and cache hits; configure tracked inputs and plugin parameters for the client project and validate the resulting builds. These snippets establish the connection, not a complete cache policy; see Apache's [setup guide](https://maven.apache.org/extensions/maven-build-cache-extension/) and [remote cache configuration](https://maven.apache.org/extensions/maven-build-cache-extension/remote-cache.html).

These examples follow the extension documentation; this project does not yet include a real Maven client integration test. See [architecture and limits](readme-arch.md#current-scope).
