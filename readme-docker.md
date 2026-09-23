## Docker command semantics

The commands below differ mainly in whether they **start**, **recreate**, **rebuild**, or **remove** the development container.

`dev` is the name of the development service defined under `services:` in `compose.yaml` (it is not a Docker keyword).

###
- ~~~bash
    docker compose up -d dev
    ~~~
    Ensure the `dev` service exists and is running, based on the current `compose.yaml`.
    Creates the container if it does not exist, starts it if it exists but is stopped, and recreates it if its Compose configuration changed.
    If it is already running and nothing relevant changed, it is left running.
    Use for initial start, after `docker compose down`, or after changing `compose.yaml`.
    Does not rebuild the image.

    ###
    - ~~~bash
        docker compose up -d --build dev
        ~~~
        Superset of the command above:
        rebuild the dev image first, then start/recreate the container.
        Use after changing Dockerfile.dev.

###
- ~~~bash
    docker compose restart dev
    ~~~
    Restart an **existing** `dev` container.
    If that existing container is running, it is stopped and started again; if it exists but is stopped, it is started again.
    If the container does not exist at all (for example after `docker compose down`), this command cannot restart it; use `docker compose up -d dev` instead.
    Unlike `up`, this always restarts the existing container, but does not create/recreate it or apply changed Compose configuration.
    Does not rebuild the image. Use only to reset the container/processes inside it (for example a stuck Java/Spring process).
    After restarting the container, rerun the required Maven command (for example `mvn spring-boot:run` or `mvn test`).

###
-   ~~~bash
    docker compose down
    ~~~
    Stop and remove the Compose containers/network.
    Does not remove the Maven cache volume.

    ###
    - ~~~bash
        docker compose down -v
        ~~~

        Superset of docker compose down:
        also r_emoves the Maven cache volume_.

----
### Summary

- `restart` = force-restart/start the same existing container; use to _reset a stuck container/process_,
    then rerun the required Maven command
    - fails if the container does not already exist (ie _`up`_ already called)

- `up` = ensure the service/container exists and is running; use for initial start, after `down`, or after changing `compose.yaml`

    -  `up --build` = rebuild image + ensure service/container is running; use after changing `Dockerfile.dev`

-  `down` = remove running Compose environment

    -  `down -v` = remove environment + persistent Maven cache


----
#### Source-code changes:

- Java source or `pom.xml` changes are immediately visible inside the container via the bind mount, but the running Java process must be rerun for those changes to take effect.

    - application: stop the current `mvn spring-boot:run` with `Ctrl+C`, then run it again
    - tests: run `docker compose exec dev mvn test` (or `mvn verify`) again
    - no Docker container restart/rebuild is required for these changes

