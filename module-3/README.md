# Module 3 - Reactive programming

A reactive sport API: a Spring WebFlux application that imports sports from the Rakuten Ichiba
item search API on startup, saves them in PostgreSQL through R2DBC, and serves them through
router functions. The full task description is in [Homework.md](Homework.md).

## Stack

- Java 25, Spring Boot 4.2.0, Gradle 9.9 (wrapper included)
- Spring WebFlux with router functions, and WebClient for the Rakuten API
- Spring Data R2DBC with PostgreSQL 18
- Reactor, JUnit 5, Testcontainers

## Tasks

| Task | Title | Status | Where to look |
| ---- | ----- | ------ | ------------- |
| 1 | ETL: data model | Done | `entity/Sport`, `schema.sql` |
| 1 | ETL: reactive repository | Done | `repository/SportRepository` |
| 1 | ETL: setup class that requests the Rakuten item search | Done | `setup/SportSetup`, `client/RakutenItemSearchClient` |
| 1 | ETL: parse the response in a reactive pipeline and save it | Done | `service/SportImportService` |
| 2 | API: `POST /api/v1/sport/{sportName}`, conflict if the sport exists | Done | `router/SportRouter`, `handler/SportHandler` |
| 2 | API: `GET /api/v1/sport?q=...` | Done | `router/SportRouter`, `handler/SportHandler` |
| 2 | API: repository methods for the API | Done | `repository/SportRepository`, `service/SportService` |
| 3 | Backpressure: request 20 elements at a time with `request()` | Done | `setup/BatchRequestSubscriber` |
| 3 | Backpressure: log `request(20)`, 20 `onNext()`, `request(20)` | Done | `setup/SportSetup`, [Watching the backpressure](#watching-the-backpressure) |

## How sports come from Rakuten

The item search API returns products, not sports. With `genreInformationFlag=1`, the response
also lists the child genres of the searched genre. The import searches genre `101070`, Sports &
Outdoors, and each of its child genres, such as ゴルフ (golf) or テニス (tennis), becomes a sport
named after the genre. It asks for a single item, since the items themselves are not used.

```text
Rakuten item search ──► GenreInformation.children ──► Sport.named(genreName) ──► repository.save
    (WebClient)            (bodyToMono, flatMapIterable)     (map)                  (concatMap)
```

The database generates the IDs. Names are unique regardless of case, so importing again saves only
genres that are new, and skips the others.

## Project structure

```text
module-3/
├── settings.gradle.kts             # Includes sport-api
├── compose.yaml                    # PostgreSQL on port 55433
└── sport-api/                      # com.epam.module3.sportapi
    ├── build.gradle.kts
    ├── client/                     # RakutenItemSearchClient and the response records
    ├── config/                     # Rakuten and import properties
    ├── dto/                        # SportResponse
    ├── entity/                     # Sport
    ├── exception/                  # Invalid name, sport already exists
    ├── handler/                    # SportHandler
    ├── repository/                 # SportRepository
    ├── router/                     # SportRouter: routes and problem responses
    ├── service/                    # SportService, SportImportService
    ├── setup/                      # SportSetup, BatchRequestSubscriber
    └── src/main/resources/         # application.yml, schema.sql
```

## Building

Requires Java 25 and Docker. Run the commands in this README from this folder.

```bash
./gradlew build    # build and run the tests
```

The repository, API and import tests run against PostgreSQL in Docker, through Testcontainers. The
Rakuten API is never called: the tests answer the client's requests with a stub.

## Running

### 1. Get Rakuten keys

Register an application at [webservice.rakuten.co.jp](https://webservice.rakuten.co.jp/app/list).
The item search API needs both its application ID and its access key:

```bash
export RAKUTEN_APPLICATION_ID=...
export RAKUTEN_ACCESS_KEY=...
```

Without them the application still starts and serves the API, and logs that the import failed
because the credentials are not set.

### 2. Start PostgreSQL and the application

```bash
docker compose up -d --wait
./gradlew bootRun
```

PostgreSQL listens on port 55433, so it does not clash with a local PostgreSQL on 5432. To use
another port, set `DB_PORT` for both `docker compose` and the application. The application creates
the table from `schema.sql` when it starts, and serves on http://localhost:8080.

| Property | Default | Purpose |
| -------- | ------- | ------- |
| `app.rakuten.application-id` | `RAKUTEN_APPLICATION_ID` | Rakuten application ID |
| `app.rakuten.access-key` | `RAKUTEN_ACCESS_KEY` | Rakuten access key |
| `app.rakuten.base-url` | `https://openapi.rakuten.co.jp` | Rakuten API host |
| `app.rakuten.timeout` | `10s` | How long to wait for Rakuten |
| `app.sport-import.enabled` | `true` | Whether to import on startup |
| `app.sport-import.genre-id` | `101070` | Genre whose child genres become sports |
| `app.sport-import.batch-size` | `20` | How many sports to request at a time |

### 3. Call the API

```bash
curl -i -X POST localhost:8080/api/v1/sport/Golf
# HTTP/1.1 201 Created
# {"id":1,"name":"Golf"}

curl -i -X POST localhost:8080/api/v1/sport/golf
# HTTP/1.1 409 Conflict
# Content-Type: application/problem+json
# {"detail":"Sport \"golf\" already exists","instance":"/api/v1/sport/golf","status":409,"title":"Conflict"}

curl -X POST "localhost:8080/api/v1/sport/Table%20tennis"
curl "localhost:8080/api/v1/sport?q=TEN"
# [{"id":2,"name":"Table tennis"}]

curl localhost:8080/api/v1/sport
# [{"id":1,"name":"Golf"},{"id":2,"name":"Table tennis"}]
```

| Request | Result |
| ------- | ------ |
| `POST /api/v1/sport/{sportName}` | `201` and the new sport. Whitespace around the name is removed |
| Same, with the name of an existing sport, in any case | `409` problem document |
| Same, with a blank name or one longer than 100 characters | `400` problem document |
| `GET /api/v1/sport?q=text` | `200` and the sports whose name contains `text`, ignoring case, sorted by name |
| `GET /api/v1/sport` | `200` and all sports, sorted by name |

`%` and `_` in `q` match themselves. The unique index on the name decides whether a sport exists,
so two requests that create the same sport at the same time cannot both succeed.

### Stopping

```bash
docker compose down       # keeps the data
docker compose down -v    # also deletes the data, so the next start imports every sport again
```

## Watching the backpressure

`SportSetup` subscribes to the import with a `BatchRequestSubscriber`. It calls `request(20)` when
it subscribes, and again each time it has received 20 sports, so the import never has more than
20 sports in flight. The `.log()` operator just above the subscriber logs each request and each
sport under the logger `sport-import`. On an empty database the log shows:

```text
sport-import  : onSubscribe(FluxConcatMapNoPrefetch.FluxConcatMapNoPrefetchSubscriber)
sport-import  : request(20)
sport-import  : onNext(Sport[id=1, name=...])
...                                             # 20 onNext in all
sport-import  : onNext(Sport[id=20, name=...])
sport-import  : request(20)
sport-import  : onNext(Sport[id=21, name=...])
...
sport-import  : onComplete()
SportSetup    : Imported 45 new sports from Rakuten
```

`SportSetupTests` starts the application with a stub that returns 45 genres, and checks this exact
sequence: `request(20)`, 20 `onNext`, `request(20)`, 20 `onNext`, `request(20)`, 5 `onNext`,
`onComplete`. The number of real genres depends on Rakuten. With fewer than 20, the log shows a
single `request(20)`.

## Error handling

| Situation | Result |
| --------- | ------ |
| Rakuten credentials not set | The import fails at once and logs why; the API keeps working |
| Rakuten answers with an error, such as `403` for a wrong key | The import logs the status and the response body |
| Rakuten does not answer within the timeout | The import logs a timeout |
| Rakuten's response has no genre information | The import fails and logs it, instead of importing nothing |
| A sport from Rakuten already exists | It is skipped; the rest are imported |
| The import fails partway | The sports saved so far stay saved; the log says how many |

The response format was checked against Rakuten's error responses, but not against a successful
response, which needs valid keys. The client expects `GenreInformation` as format version 2 sends
it, and also accepts it as a single object, as format version 1 sends it.

## Tests

| Level | Tests |
| ----- | ----- |
| Unit | `SportTests`, `RakutenItemSearchClientTests` (stubbed HTTP), `BatchRequestSubscriberTests` |
| Repository | `SportRepositoryTests` |
| Service | `SportImportServiceTests` |
| API | `SportApiTests`: every route through `WebTestClient` |
| Application | `SportApiApplicationTests`, `SportSetupTests`: the backpressure sequence in the log |
