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
    Unlike `up`, this does not create/recreate the container or apply changed Compose configuration.
    Does not rebuild the image. Useful for clearing a stuck Java/Spring process.

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
In short:

- `restart` = restart/start the same existing container - fails if the container does not already exist (ie _`up`_ already called)

- `up` = use `compose.yaml` to ensure the service/container exists (and is running)

    -  `up --build` = rebuild image + ensure service/container is running

-  `down` = remove running Compose environment

    -  `down -v` = remove environment + persistent Maven cache
