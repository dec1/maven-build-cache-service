# Development environment with Docker Compose

`compose.yaml` defines an optional `dev` service with JDK 25 and Maven, so these tools do not need to be installed on the host. `Dockerfile.dev` defines its image. The commands below build, start, update or remove this development environment.

The container stays running so you can execute Maven commands inside it; the application and tests are started separately. For run commands and Docker's role in S3 tests, see the [development runbook](readme-dev.md).

| Command | Effect / when to use |
| --- | --- |
| `docker compose up -d dev` | Create/start `dev`, recreating it if its configuration or image changed; use after `down` or changes to `../compose.yaml` |
| `docker compose up -d --build dev` | Build the image, then create/start or recreate `dev`; use after changing `Dockerfile.dev` |
| `docker compose restart dev` | Restart the existing container and stop its processes; does not apply changed Compose configuration or rebuild the image |
| `docker compose down` | Remove Compose containers and networks; retain the Maven cache volume |
| `docker compose down -v` | Also remove the Maven cache volume; dependencies must be downloaded again |

`up` can build a missing image; use `--build` to request a build before starting. `restart` requires an existing container. After a restart or recreation, rerun the required Maven command.

Java source and `../pom.xml` are bind-mounted from the checkout. Their changes require rerunning Maven, not rebuilding or restarting the container.

