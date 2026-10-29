# Development environment with Docker Compose

`compose.yaml` defines an optional `dev` service with JDK 25 and Maven, so these tools do not need to be installed on the host. `Dockerfile.dev` defines its image. The commands below build, start, update or remove this development environment.

The container stays running so you can execute Maven commands inside it; the application and tests are started separately. For run commands and Docker's role in S3 tests, see the [development runbook](readme-dev.md).

| Command | Effect / when to use |
| --- | --- |
| `docker compose up -d dev` | Create/start `dev`, recreating it if its configuration or image changed; use after `down` or changes to `../compose.yaml` |
| `docker compose up -d --build dev` | Build the image, then create/start or recreate `dev`; use after changing `Dockerfile.dev` |
| `docker compose restart dev` | Restart the existing container and stop its processes; does not apply changed Compose configuration or rebuild the image |

`up` can build a missing image; use `--build` to request a build before starting. `restart` requires an existing container. After a restart or recreation, rerun the required Maven command.

Java source and `../pom.xml` are bind-mounted from the checkout. Their changes require rerunning Maven, not rebuilding or restarting the container.

## Cleanup

Run from the project root after application and test commands have finished. Choose how much to retain:

| Command | Removed / retained |
| --- | --- |
| `docker compose stop` | Stops this project's Compose containers; retains containers, images and Maven cache for reuse |
| `docker compose down` | Removes Compose containers and networks; retains images and the Maven cache volume |
| `docker compose down --volumes --rmi all --remove-orphans` | Also removes the Maven cache volume, service images and obsolete containers belonging to this Compose project |

The last option is for a longer break: Maven dependencies must be downloaded again, the `dev` image rebuilt and LocalStack pulled again when needed. The checkout and files bind-mounted from it are retained. Images still used by other containers may remain.

Compose cleanup does not remove all downloaded base/helper images or Docker's build cache. To reclaim more disk space, this optional command removes unused images, stopped containers, unused networks and build cache **across the Docker daemon, including other projects**. It retains volumes:

```bash
docker system prune -a
```

Docker asks for confirmation; review the scope before accepting.

To resume after stopping or removing the environment:

```bash
docker compose up -d --build dev
```

Then rerun the app or tests from the [quick start](readme-dev.md#quick-start-docker-compose).

