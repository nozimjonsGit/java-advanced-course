# Module 1 - Spring Boot

Two separate Spring Boot applications. The full task description is in [Homework.md](Homework.md).

- `hello-world` prints `hello world` when the Spring context starts.
- `product-api` is a product catalog REST API, secured with OAuth2 and JWT.

## Stack

- Java 25
- Spring Boot 4.1.2
- Spring Data JPA with H2 (local, tests) and PostgreSQL (dev, stg, prod)
- Flyway for database migrations
- Spring Security with its OAuth2 authorization server and resource server
- Spring Boot Actuator with Micrometer and Prometheus

## Tasks

| Task | Title | Required | Status | Where to look |
| ---- | ----- | -------- | ------ | ------------- |
| 1 | Hello-world application | Yes | Done | `hello-world` |
| 2 | CRUD REST application | Yes | Done | `product-api`: `controller`, `service`, `repository` |
| 3 | Security | Yes | Done | `product-api`: `config`, `AppUserDetailsService` |
| 4 | Externalized configuration | Optional | Done | `application-{profile}.yml` |
| 5 | Data migrating | Optional | Done | `db/migration` |
| 6 | Actuator | Yes | Done | `product-api`: `health`, `metrics` |
| 7 | Testing | Optional | Done | `src/test` |

## Project structure

```text
module-1/
├── pom.xml                  # Parent: Java and Spring Boot versions, list of applications
├── hello-world/             # Task 1
└── product-api/             # Tasks 2 to 7
    ├── compose.yaml         # PostgreSQL for the dev profile
    ├── scripts/get-token.sh # Gets an access token with curl
    └── src/main/java/com/epam/module1/productapi/
        ├── config/          # Security, OAuth2 and seed users
        ├── controller/      # REST endpoints
        ├── dto/             # Request and response bodies
        ├── entity/          # JPA entities
        ├── exception/       # Exceptions and error responses
        ├── health/          # Custom health indicators
        ├── metrics/         # Custom Prometheus metrics
        ├── repository/      # Spring Data repositories
        └── service/         # Business logic
```

## Running

Requires Java 25 (`JAVA_HOME` must point to it). Run these commands from this folder.

```bash
./mvnw verify                             # build and run the tests of both applications
./mvnw -pl hello-world spring-boot:run    # print hello world and exit
./mvnw -pl product-api spring-boot:run    # start on http://localhost:8080 with the local profile
```

The local profile needs nothing else: it uses an in-memory database.

## Trying the API

The local and dev profiles create two users:

| Username | Password | Role | Can |
| -------- | -------- | ---- | --- |
| `admin` | `admin-password` | `ADMIN` | Read and change products |
| `user` | `user-password` | `USER` | Read products |

### With cURL

`product-api/scripts/get-token.sh` runs the OAuth2 login with curl and prints an access token.

```bash
TOKEN=$(product-api/scripts/get-token.sh admin admin-password)

curl -i -X POST localhost:8080/api/v1/products \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name": "Laptop", "description": "14-inch", "price": 999.99}'

curl -H "Authorization: Bearer $TOKEN" "localhost:8080/api/v1/products?name=lap&size=10&sort=price,desc"
```

### With Postman

Open the **Authorization** tab, choose **OAuth 2.0**, and use these settings:

| Setting | Value |
| ------- | ----- |
| Grant type | Authorization Code (With PKCE) |
| Callback URL | `https://oauth.pstmn.io/v1/callback` |
| Auth URL | `http://localhost:8080/oauth2/authorize` |
| Access Token URL | `http://localhost:8080/oauth2/token` |
| Client ID | `module1-client` |
| Client Secret | `local-secret` |
| Code Challenge Method | SHA-256 |
| Client Authentication | Send as Basic Auth header |

## API

| Method | Path | Role | Success |
| ------ | ---- | ---- | ------- |
| `GET` | `/api/v1/products` | `USER`, `ADMIN` | `200` |
| `GET` | `/api/v1/products/{id}` | `USER`, `ADMIN` | `200` |
| `POST` | `/api/v1/products` | `ADMIN` | `201` with `Location` |
| `PUT` | `/api/v1/products/{id}` | `ADMIN` | `200` |
| `DELETE` | `/api/v1/products/{id}` | `ADMIN` | `204` |

The list accepts `name` (part of the name, ignoring case), `page`, `size` (at most 100) and `sort`
(by `id`, `name`, `description` or `price`).

API errors, including `401` and `403`, are [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457)
problem documents. Validation errors also list each invalid field under `errors`. Requests that
are rejected before they reach the API, such as a malformed URL, get Spring Boot's standard error
body.

## Security

The application is both the OAuth2 authorization server and the API it protects.

1. A client sends the user to `/oauth2/authorize`, using the authorization code flow with PKCE.
2. The user logs in with a username and password stored in the database, as a bcrypt hash.
3. The client exchanges the returned code at `/oauth2/token` for a signed JWT, valid for 15 minutes.
4. The client calls the API with that token. The API checks its signature, expiry and issuer, and
   reads the user's role from its `roles` claim.

## Profiles

`spring-boot:run` uses `local` unless told otherwise:

```bash
./mvnw -pl product-api spring-boot:run -Dspring-boot.run.profiles=dev
```

The packaged jar has no default profile, so a deployment cannot start with the local credentials
by accident. Select one with `spring.profiles.active`:

```bash
java -jar product-api/target/product-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

| Profile | Database | Credentials and settings |
| ------- | -------- | ------------------------ |
| `local` | In-memory H2 | Built in |
| `dev` | PostgreSQL from `compose.yaml` | Built in, can be overridden |
| `stg` | PostgreSQL, `module1_stg` | From the environment |
| `prod` | PostgreSQL, `module1_prod` | From the environment |

To start the dev database, run `docker compose up -d --wait` in `product-api`. If port 5432 is
taken, set `DB_PORT` to a free port for both this command and the application.

`stg` and `prod` have no defaults for these variables, and startup fails without them:

| Variable | Purpose |
| -------- | ------- |
| `DB_HOST` | Database host. `DB_PORT` and `DB_NAME` are optional |
| `DB_USERNAME`, `DB_PASSWORD` | Database credentials |
| `APP_ISSUER` | Public base URL of the application, used as the token issuer |
| `OAUTH_CLIENT_SECRET` | Client secret with its encoding, such as `{bcrypt}<hash>` |
| `OAUTH_REDIRECT_URIS` | Comma-separated redirect URIs allowed for the client |

These profiles create no users. Provide the first admin once, through the environment:

```bash
APP_SECURITY_SEEDUSERS_0_USERNAME=<username>
APP_SECURITY_SEEDUSERS_0_PASSWORD=<password>
APP_SECURITY_SEEDUSERS_0_ROLE=ADMIN
```

## Database migrations

Flyway applies the scripts in `product-api/src/main/resources/db/migration` on startup. Hibernate
only validates the schema against the entities, so every schema change needs a new migration.
Never edit a migration that has already run.

## Monitoring

| Endpoint | Access | Content |
| -------- | ------ | ------- |
| `/actuator/health` | Public, details for `ADMIN` | Overall status |
| `/actuator/prometheus` | `ADMIN` | Metrics in Prometheus format |

Custom health indicators:

- `productCatalog` reads the catalog and reports the number of products.
- `adminAccount` reports `WARNING` when no user has the `ADMIN` role. The overall status stays
  `UP`, because the instance can still serve requests.

Custom metrics:

- `product_operations_total{operation="created|updated|deleted"}` counts successful changes.
- `products_count` is the current number of products.

## Tests

`./mvnw verify` runs all tests on in-memory H2, under the `test` profile.

| Level | Tests |
| ----- | ----- |
| Repository | `ProductRepositoryTests` |
| Service and unit | `ProductServiceTests`, `ProductMetricsTests`, `*HealthIndicatorTests` |
| Controller | `ProductControllerTests` |
| Integration | `ProductApiIntegrationTests`, `AuthorizationCodeFlowTests`, `ProductAccessTests`, `ActuatorEndpointsTests`, `MissingAdminHealthTests`, `RejectedRequestTests` |
| Configuration | `ProfileConfigurationTests` |
