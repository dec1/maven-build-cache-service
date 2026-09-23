## Docker command semantics

The commands below differ mainly in whether they **start**, **recreate**, **rebuild**, or **remove** the development container.

`dev` is the name of the development service defined under `services:` in `compose.yaml`; it is not a Docker keyword.


- ~~~bash
    docker compose up -d dev
    ~~~
    Ensure the `dev` service exists and is running, based on the current `compose.yaml`.
    Creates the container if it does not exist, starts it if it is stopped, and recreates it if its Compose configuration changed.
    Does not rebuild the image.

- ~~~bash
    docker compose up -d --build dev
    ~~~
    Superset of the command above:
    rebuild the dev image first, then start/recreate the container.
    Use after changing Dockerfile.dev.

- ~~~bash
    docker compose restart dev
    ~~~
    Restart the already-created `dev` container in place (whether currently running or stopped).
    Unlike `up`, this does not create a missing container or apply changed Compose configuration.
    Does not rebuild the image and does not recreate the container.
    Useful for clearing a stuck Java/Spring process. After `docker compose down`, use `up` instead because the container has been removed.

-   ~~~bash
    docker compose down
    ~~~
    Stop and remove the Compose containers/network.
    Does not remove the Maven cache volume.

- ~~~bash
    docker compose down -v
    ~~~

    Superset of docker compose down:
    also removes the Maven cache volume.

----
In short:

- `restart` = restart the same already-created container

- `up` = use `compose.yaml` to ensure the service/container exists and is running

    -  `up --build` = rebuild image + ensure service/container is running

-  `down` = remove running Compose environment

    -  `down -v` = remove environment + persistent Maven cache


