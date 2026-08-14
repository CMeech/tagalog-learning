# Tagalog Learning Platform

A Kotlin CLI for managing structured Tagalog learning material, with SQLite as the source of truth and Anki as an export target.

## Prerequisites

- OrbStack for Docker-compatible development and testing on macOS

Java 21 and Gradle are supplied by the container images; no host Java installation is required.

The installed application is named `tagalog` and currently provides these foundation commands:

```text
init
version
validate
migrate
```

## Development with OrbStack

Start OrbStack, ensure its Docker CLI integration is on your `PATH`, then run:

```shell
docker compose build
docker compose run --rm app version
```

`docker compose build` runs the complete Gradle build and test suite. OrbStack is the authoritative development test environment.

Running the application without an explicit command initializes or migrates its SQLite database:

```shell
docker compose up
```

The database is stored in the Compose-managed `tagalog-data` volume and survives container replacement. Repeating `docker compose up` is safe; Flyway applies only pending migrations. To verify the persisted schema, run:

```shell
docker compose run --rm app validate
```

For implementation work, start with the small [task context](docs/task-context.md), then read only the relevant [milestone](docs/milestones/). Stable decisions, conventions, and deferred work are linked from the task context.
