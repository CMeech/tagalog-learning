# Development Conventions

## Code

- Prefer immutable data classes.
- Keep domain logic outside infrastructure.
- Avoid unnecessary frameworks.
- Add tests with features.

## Database

- Apply every schema change with Flyway.
- Never modify an existing migration; add a new migration.

## Testing

- Unit tests validate domain behavior and business rules.
- Integration tests validate the database, migrations, and CLI workflows.
- With OrbStack running, `docker compose build` is the authoritative build and runs the Gradle build and test suite.
- Smoke-test with `docker compose run --rm app --help` and `docker compose run --rm app version`.

## Git

Example commit messages:

```text
feat: add vocabulary entity
test: add migration tests
docs: update architecture
```
