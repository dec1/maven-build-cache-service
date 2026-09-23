## Docker command semantics

The commands below differ mainly in whether they **start**, **recreate**, **rebuild**, or **remove** the development container.


- ~~~bash
    docker compose up -d dev
    ~~~
    Start the existing dev service.
    Recreates the container if its Compose configuration changed.
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
    Restart the existing container in place.
    Does not rebuild the image and does not recreate the container.
    Useful for clearing a stuck Java/Spring process.

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

- `restart` = restart same container

- `up` = ensure service/container is running

    -  `up --build` = rebuild image + ensure service/container is running

-  `down` = remove running Compose environment

    -  `down -v` = remove environment + persistent Maven cache


