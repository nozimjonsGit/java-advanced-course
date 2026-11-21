# Module 3 - Reactive programming

A reactive sport API: a Spring WebFlux application that imports sports on startup, saves them in
PostgreSQL through R2DBC, and serves them through router functions. The sports come from Wikidata,
which needs no keys, or from the Rakuten Ichiba item search API that the homework names. The full
task description is in [Homework.md](Homework.md).

## Stack

- Java 25, Spring Boot 4.2.0, Gradle 9.9 (wrapper included)
- Spring WebFlux with router functions, and WebClient for Wikidata and Rakuten
- Spring Data R2DBC with PostgreSQL 18
- Reactor, JUnit 5, Testcontainers

## Tasks

| Task | Title | Status | Where to look |
| ---- | ----- | ------ | ------------- |
| 1 | ETL: data model | Done | `entity/Sport`, `schema.sql` |
| 1 | ETL: reactive repository | Done | `repository/SportRepository` |
| 1 | ETL: setup class that requests the Rakuten item search | Done | `setup/SportSetup`, `source/RakutenSportSource`, `client/RakutenItemSearchClient` |
| 1 | ETL: parse the response in a reactive pipeline and save it | Done | `service/SportImportService` |
| 2 | API: `POST /api/v1/sport/{sportName}`, conflict if the sport exists | Done | `router/SportRouter`, `handler/SportHandler` |
| 2 | API: `GET /api/v1/sport?q=...` | Done | `router/SportRouter`, `handler/SportHandler` |
| 2 | API: repository methods for the API | Done | `repository/SportRepository`, `service/SportService` |
| 3 | Backpressure: request 20 elements at a time with `request()` | Done | `setup/BatchRequestSubscriber` |
| 3 | Backpressure: log `request(20)`, 20 `onNext()`, `request(20)` | Done | `setup/SportSetup`, [Watching the backpressure](#watching-the-backpressure) |
| Extra | Wikidata as a source that needs no keys | Done | `source/WikidataSportSource`, `client/WikidataSportClient` |

## Sport sources

The import takes sport names from a `SportSource`, saves a sport for each one and skips the ones
that already exist. `app.sport-import.source` chooses the source:

| Source | Keys | What becomes a sport |
| ------ | ---- | -------------------- |
| `wikidata` (default) | None | The 500 best-known types of sport on Wikidata, by English name: association football, chess, basketball, tennis... |
| `rakuten` | Application ID and access key | The child genres of a Rakuten Ichiba genre, by default Sports & Outdoors: ゴルフ (golf), テニス (tennis)... |

```text
SportSource ──► Sport.named(name) ──► repository.save ──► BatchRequestSubscriber
 (WebClient)        (map)               (concatMap)          (request(20) at a time)
```

**Wikidata.** The client sends one SPARQL query to the
[Wikidata Query Service](https://query.wikidata.org) for items that are a type of sport
(`wd:Q31629`), best-known first: those with articles in the most Wikipedia editions.

**Rakuten.** The item search API returns products, not sports. With `genreInformationFlag=1`, the
response also lists the child genres of the searched genre, and each becomes a sport. The client
asks for a single item, since the items themselves are not used.

The database generates the IDs. Names are unique regardless of case, so importing again saves only
new sports.

## Project structure

```text
module-3/
├── settings.gradle.kts             # Includes sport-api
├── compose.yaml                    # PostgreSQL on port 55433
└── sport-api/                      # com.epam.module3.sportapi
    ├── build.gradle.kts
    ├── client/                     # Wikidata and Rakuten clients, their response records
    ├── config/                     # Properties, and the choice of sport source
    ├── dto/                        # SportResponse
    ├── entity/                     # Sport
    ├── exception/                  # Invalid name, sport already exists
    ├── handler/                    # SportHandler
    ├── repository/                 # SportRepository
    ├── router/                     # SportRouter: routes and problem responses
    ├── service/                    # SportService, SportImportService
    ├── setup/                      # SportSetup, BatchRequestSubscriber
    ├── source/                     # SportSource and its Wikidata and Rakuten versions
    └── src/main/resources/         # application.yml, schema.sql
```

## Building

Requires Java 25 and Docker. Run the commands in this README from this folder.

```bash
./gradlew build    # build and run the tests
```

The repository, API and import tests run against PostgreSQL in Docker, through Testcontainers.
Wikidata and Rakuten are never called: the tests answer the clients' requests with a stub.

## Running

### 1. Start PostgreSQL and the application

```bash
docker compose up -d --wait
./gradlew bootRun
```

This imports sports from Wikidata, which needs nothing else. PostgreSQL listens on port 55433, so
it does not clash with a local PostgreSQL on 5432. To use another port, set `DB_PORT` for both
`docker compose` and the application. The application creates the table from `schema.sql` when it
starts, and serves on http://localhost:8080.

### 2. Optionally, import from Rakuten instead

Register an application at [webservice.rakuten.co.jp](https://webservice.rakuten.co.jp/app/list).
The item search API needs both its application ID and its access key:

```bash
export RAKUTEN_APPLICATION_ID=...
export RAKUTEN_ACCESS_KEY=...
SPORT_SOURCE=rakuten ./gradlew bootRun
```

Without the keys the application still starts and serves the API, and logs that the import failed
because the credentials are not set.

| Property | Default | Purpose |
| -------- | ------- | ------- |
| `app.sport-import.enabled` | `true` | Whether to import on startup |
| `app.sport-import.source` | `wikidata` | `wikidata` or `rakuten`. Also set by `SPORT_SOURCE` |
| `app.sport-import.batch-size` | `20` | How many sports to request at a time |
| `app.wikidata.limit` | `500` | How many sports to fetch from Wikidata |
| `app.wikidata.user-agent` | `sport-api/0.0.1 (Spring WebClient)` | Wikimedia asks clients to name themselves, and to add contact details if they can |
| `app.wikidata.timeout` | `30s` | How long to wait for Wikidata |
| `app.wikidata.rate-limit-retries` | `2` | How many times to try again after a 429 from Wikidata |
| `app.wikidata.rate-limit-retry-delay` | `60s` | How long to wait before trying again |
| `app.rakuten.application-id` | `RAKUTEN_APPLICATION_ID` | Rakuten application ID |
| `app.rakuten.access-key` | `RAKUTEN_ACCESS_KEY` | Rakuten access key |
| `app.rakuten.genre-id` | `101070` | Genre whose child genres become sports |
| `app.rakuten.base-url` | `https://openapi.rakuten.co.jp` | Rakuten API host |
| `app.rakuten.timeout` | `10s` | How long to wait for Rakuten |

### 3. Call the API

```bash
curl -i -X POST localhost:8080/api/v1/sport/Quidditch
# HTTP/1.1 201 Created
# {"id":1001,"name":"Quidditch"}

curl -i -X POST localhost:8080/api/v1/sport/TENNIS
# HTTP/1.1 409 Conflict
# Content-Type: application/problem+json
# {"detail":"Sport \"TENNIS\" already exists","instance":"/api/v1/sport/TENNIS","status":409,"title":"Conflict"}

curl "localhost:8080/api/v1/sport?q=tennis"
# [{"id":256,"name":"beach tennis"},{"id":187,"name":"football tennis"},...,{"id":4,"name":"tennis"},...]

curl localhost:8080/api/v1/sport    # all sports, sorted by name
```

| Request | Result |
| ------- | ------ |
| `POST /api/v1/sport/{sportName}` | `201` and the new sport. Whitespace around the name is removed |
| Same, with the name of an existing sport, in any case | `409` problem document |
| Same, with a blank name or one longer than 100 characters | `400` problem document |
| `GET /api/v1/sport?q=text` | `200` and the sports whose name contains `text`, ignoring case, sorted by name |
| `GET /api/v1/sport` | `200` and all sports, sorted by name |

`%` and `_` in `q` match themselves. The unique index on the name decides whether a sport exists,
so two requests that create the same sport at the same time cannot both succeed. Each insert it
rejects still uses up an ID, so IDs have gaps: above, the second start of the import skipped all
500 sports, and Quidditch got ID 1001.

### Stopping

```bash
docker compose down       # keeps the data
docker compose down -v    # also deletes the data, so the next start imports every sport again
```

## Watching the backpressure

`SportSetup` subscribes to the import with a `BatchRequestSubscriber`. It calls `request(20)` when
it subscribes, and again each time it has received 20 sports, so the import never has more than
20 sports in flight. The `.log()` operator just above the subscriber logs each request and each
sport under the logger `sport-import`. On an empty database, the import from Wikidata logs:

```text
sport-import  : onSubscribe(FluxConcatMapNoPrefetch.FluxConcatMapNoPrefetchSubscriber)
sport-import  : request(20)
sport-import  : onNext(Sport[id=1, name=association football])
...                                                     # 20 onNext in all
sport-import  : onNext(Sport[id=20, name=fencing])
sport-import  : request(20)
sport-import  : onNext(Sport[id=21, name=gymnastics])
...                                                     # 26 request(20) and 500 onNext in all
sport-import  : onComplete()
SportSetup    : Imported 500 new sports from Wikidata
```

`SportSetupTests` starts the application with a source of 45 sports, and checks this exact
sequence: `request(20)`, 20 `onNext`, `request(20)`, 20 `onNext`, `request(20)`, 5 `onNext`,
`onComplete`.

## Error handling

| Situation | Result |
| --------- | ------ |
| Unknown `app.sport-import.source` | The application does not start, and names the value |
| Wikidata answers 429 Too Many Requests | The client waits 60 seconds and tries again, twice, logging a warning each time |
| Wikidata or Rakuten answers with another error | The import logs the status and the response body |
| Wikidata or Rakuten does not answer within the timeout | The import logs a timeout |
| Rakuten credentials not set | The import fails at once and logs why |
| The response has no results or no genre information | The import fails and logs it, instead of importing nothing |
| A sport from the source already exists | It is skipped; the rest are imported |
| A name from the source is blank or longer than 100 characters | It is skipped with a warning; the rest are imported |
| The import fails partway | The sports saved so far stay saved; the log says how many |

In every case the API keeps working.

The Wikidata client sends no `Accept` header, and selects JSON with `format=json` instead. While
part of the query service was down, Wikimedia limited this client's requests to one a minute when
they had an `Accept` header, and let the same requests without one through.

The Wikidata import was run against the live service. The Rakuten client was checked against
Rakuten's error responses, but not against a successful response, which needs valid keys. It
expects `GenreInformation` as format version 2 sends it, and also accepts it as a single object, as
format version 1 sends it.

## Tests

| Level | Tests |
| ----- | ----- |
| Unit | `SportTests`, `BatchRequestSubscriberTests` |
| Clients | `WikidataSportClientTests`, `RakutenItemSearchClientTests`: against stubbed HTTP responses |
| Sources | `RakutenSportSourceTests`, `SportSourceConfigTests`: which source each setting chooses |
| Repository | `SportRepositoryTests` |
| Service | `SportImportServiceTests` |
| API | `SportApiTests`: every route through `WebTestClient` |
| Application | `SportApiApplicationTests`, `SportSetupTests`: the backpressure sequence in the log |
