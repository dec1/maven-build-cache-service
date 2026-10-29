## Docker command semantics

`dev` is the development service in `compose.yaml`. Its container stays running with `sleep infinity`; start the application or tests separately as described in the [development runbook](readme-dev.md).

| Command | Effect / when to use |
| --- | --- |
| `docker compose up -d dev` | Create/start `dev`, recreating it if its configuration or image changed; use after `down` or changes to `compose.yaml` |
| `docker compose up -d --build dev` | Build the image, then create/start or recreate `dev`; use after changing `Dockerfile.dev` |
| `docker compose restart dev` | Restart the existing container and stop its processes; does not apply changed Compose configuration or rebuild the image |
| `docker compose down` | Remove Compose containers and networks; retain the Maven cache volume |
| `docker compose down -v` | Also remove the Maven cache volume; dependencies must be downloaded again |

`up` can build a missing image; use `--build` to request a build before starting. `restart` requires an existing container. After a restart or recreation, rerun the required Maven command.

Java source and `pom.xml` are bind-mounted from the checkout. Their changes require rerunning Maven, not rebuilding or restarting the container.

