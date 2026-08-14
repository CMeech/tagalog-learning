# Milestone 1 — Project Foundation

Goal: create a runnable Kotlin CLI application.

## 1.1 Create Gradle project

- [x] Create Gradle Kotlin DSL project.
- [x] Configure Kotlin JVM plugin.
- [x] Configure Java version.
- [x] Add application plugin.
- [x] Document the container-managed JDK version.
- [x] Confirm the OrbStack build container uses the supported JDK.
- [x] Confirm the Gradle build succeeds in OrbStack.

## 1.2 Configure dependencies

- [x] Add Picocli.
- [x] Add SQLite JDBC.
- [x] Add Flyway.
- [x] Add Exposed.
- [x] Add JUnit 5.
- [x] Add a logging framework.
- [ ] Confirm dependencies resolve.
- [ ] Confirm the application compiles.

## 1.3 Create CLI

Implement the `tagalog` application with `init`, `version`, `validate`, and `migrate` commands.

- [x] CLI starts.
- [x] Help output works.
- [x] Commands execute.

## 1.4 Add Docker support

- [x] Use OrbStack to run Docker and Docker Compose.
- [x] Create `Dockerfile`.
- [x] Create `docker-compose.yml`.
- [x] Confirm `docker compose up` passes.

## Definition of Done

- [x] Project builds.
- [x] Tests pass in Docker through OrbStack.
- [x] CLI launches.
- [x] Docker works through OrbStack.
- [ ] CI passes.
