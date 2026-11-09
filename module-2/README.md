# Module 2 - gRPC and Avro

A gRPC server with a Java client and a Python client, and a Kafka producer and consumer that
serialize messages with Avro and Schema Registry. The full task description is in
[Homework.md](Homework.md).

- `grpc-api` holds `ping.proto`, the one proto file the server and both clients use.
- `grpc-server` answers `Ping` with `Pong` on port 8080.
- `grpc-client` is the Java client. It sends one ping, prints the response and exits.
- `grpc-client-python` is the Python client. It does the same.
- `avro-producer` sends one message to the `messages` topic, with version 1 or 2 of the schema.
- `avro-consumer` prints every message on the topic, with the schema ID from its header.

## Stack

- Java 25, Spring Boot 4.1.2
- Spring Boot's gRPC starters, grpc-java 1.84.1 and protobuf 4
- Python 3.10 or later, with grpcio 1.84.1
- Spring for Apache Kafka, Kafka 4.2
- Avro 1.12 and Confluent Schema Registry 8.2

## Tasks

| Task | Title | Status | Where to look |
| ---- | ----- | ------ | ------------- |
| 1 | gRPC: server on port 8080 | Done | `grpc-server`: `PingService` |
| 1 | gRPC: Java client, same proto file | Done | `grpc-client`: `PingClient`, `PingRunner` |
| 1 | gRPC: client in another language | Done | `grpc-client-python`: `ping_client.py` |
| 1 | gRPC: clients talk to the server | Done | [Running the gRPC part](#running-the-grpc-part) |
| 2 | Avro: producer | Done | `avro-producer`: `MessageFactory`, `MessageProducer` |
| 2 | Avro: consumer | Done | `avro-consumer`: `MessageListener`, `MessageDecoder` |
| 2 | Avro: Kafka, topic, producer and consumer running | Done | `compose.yaml`, `scripts/create-topic.sh` |
| 2 | Avro: different schemas, changed ID and version | Done | [Watching the schema change](#watching-the-schema-change) |

## Project structure

```text
module-2/
├── pom.xml                         # Parent: Java, Spring Boot, Confluent and Avro versions
├── compose.yaml                    # Kafka and Schema Registry
├── scripts/create-topic.sh         # Creates the messages topic
├── grpc-api/
│   └── src/main/protobuf/ping.proto
├── grpc-server/                    # com.epam.module2.grpcserver
│   ├── config/                     # Clock
│   └── service/                    # PingService
├── grpc-client/                    # com.epam.module2.grpcclient
│   ├── PingRunner                  # Pings once at startup and prints the response
│   ├── config/                     # Channel, stub and client properties
│   └── service/                    # PingClient
├── grpc-client-python/
│   ├── generate.sh                 # Generates the stubs from grpc-api's ping.proto
│   ├── ping_client.py
│   └── test_ping_client.py
├── avro-producer/                  # com.epam.module2.avroproducer
│   ├── MessageProducerRunner       # Sends one message at startup
│   ├── config/                     # Producer properties, clock
│   ├── schema/                     # Loads the two schema versions
│   ├── service/                    # MessageFactory, MessageProducer
│   └── src/main/resources/avro/    # message-v1.avsc, message-v2.avsc
└── avro-consumer/                  # com.epam.module2.avroconsumer
    ├── config/                     # Consumer properties, error handling
    ├── listener/                   # MessageListener
    ├── service/                    # MessageDecoder
    └── src/main/avro/message.avsc  # Reader schema, generates the Message class
```

## Building

Requires Java 25 (`JAVA_HOME` must point to it). Run the commands in this README from this folder.

```bash
./mvnw install    # build all applications and run their tests
```

Run `install` once before `-pl <application> spring-boot:run`: the gRPC applications depend on
`grpc-api`, which Maven must find in the local repository. Run it again after changing
`ping.proto`.

The tests need neither Docker nor a running server. The gRPC tests use an in-process transport or
a server on a free port, and the Kafka tests use an embedded broker and an in-memory Schema
Registry (`mock://` URLs).

## Running the gRPC part

### Server

```bash
./mvnw -pl grpc-server spring-boot:run
```

The server listens on port 8080. Besides `module2.ping.v1.PingService`, it serves the standard
gRPC health and reflection services, so tools such as `grpcurl` can list its methods.

| Request `message` | Result |
| ----------------- | ------ |
| `Ping` | `Pong`, and the time the server handled the request |
| Anything else | `INVALID_ARGUMENT` with a description |

### Java client

In a second terminal:

```bash
./mvnw -pl grpc-client spring-boot:run
```

```text
... c.e.m.grpcclient.PingRunner : Received 'Pong' (server time 2026-11-09T17:35:07.100251Z)
```

The client exits with code 0 after a response, or 1 after an error. These properties change what
it sends:

| Property | Default | Purpose |
| -------- | ------- | ------- |
| `spring.grpc.client.channel.ping-server.target` | `localhost:8080` | Server address. Also set by `PING_SERVER_TARGET` |
| `app.ping.message` | `Ping` | Message to send |
| `app.ping.client` | `java-client` | Name the server logs |
| `app.ping.deadline` | `5s` | How long to wait for the response |

```bash
./mvnw -pl grpc-client spring-boot:run -Dspring-boot.run.arguments=--app.ping.message=Hello
# io.grpc.StatusRuntimeException: INVALID_ARGUMENT: Expected message 'Ping' but got 'Hello'
```

### Python client

Set up a virtual environment and generate the stubs from `grpc-api/src/main/protobuf/ping.proto`,
the file the Java applications use:

```bash
cd grpc-client-python
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
./generate.sh
```

Then, with the server running:

```bash
python ping_client.py
# Received 'Pong' (server time 2026-11-09T17:36:12.744995+00:00)

python ping_client.py --message Hello
# Ping to localhost:8080 failed: INVALID_ARGUMENT: Expected message 'Ping' but got 'Hello'

python -m unittest -v    # tests, against a fake server
```

`python ping_client.py --help` lists the options: `--target`, `--message`, `--client` and
`--timeout`. The generated `ping_pb2*.py` files are not committed; run `generate.sh` again after
changing the proto file.

The server log shows both clients:

```text
... PingService : Received 'Ping' from 'java-client'
... PingService : Received 'Ping' from 'python-client'
```

## Running the Avro part

### 1. Start Kafka and create the topic

```bash
docker compose up -d --wait
scripts/create-topic.sh
```

This starts Kafka on port 9092 and Schema Registry on port 8081, and creates the `messages` topic
with one partition. Kafka does not create topics on its own here, so the producer fails if the
topic is missing. If a port is taken, set `KAFKA_PORT` or `SCHEMA_REGISTRY_PORT` for
`docker compose`, and `KAFKA_BOOTSTRAP_SERVERS` or `SCHEMA_REGISTRY_URL` for the applications.

### 2. Start the consumer

```bash
./mvnw -pl avro-consumer spring-boot:run
```

It keeps running and prints each message as it arrives. As a new consumer group, it starts from
the beginning of the topic.

### 3. Send messages with both schema versions

In a second terminal:

```bash
./mvnw -pl avro-producer spring-boot:run
./mvnw -pl avro-producer spring-boot:run -Dspring-boot.run.arguments="--app.producer.schema-version=v2 --app.producer.text=Hello"
```

| Property | Default | Purpose |
| -------- | ------- | ------- |
| `app.producer.topic` | `messages` | Topic to send to |
| `app.producer.schema-version` | `v1` | `v1` or `v2` |
| `app.producer.text` | `Hello, Avro!` | The message's text |
| `app.producer.sender` | `avro-producer` | The message's sender, only in `v2` |

## Watching the schema change

The applications use three schemas, all for the record `com.epam.module2.avro.Message`:

| Schema | Used by | Fields |
| ------ | ------- | ------ |
| `message-v1.avsc` | Producer, to write | `id`, `text`, `sentAt` |
| `message-v2.avsc` | Producer, to write | Version 1 plus `sender`, which defaults to `unknown` |
| `message.avsc` | Consumer, to read | The same fields as version 2 |

The producer's Avro serializer registers the schema it writes with under the subject
`messages-value`, and puts its ID in front of every payload. Schema Registry accepts version 2
because its default compatibility mode is `BACKWARD`: version 2 only adds a field with a default,
so it can still read version 1 data.

The consumer reads the ID, fetches that writer schema, and lets Avro resolve the data to its own
reader schema. Messages written with version 1 get the default sender.

### In the consumer's output

```text
Received messages-0 at offset 0: header 0000000001, schema id 1, schema version 1, message {"id": "44f1605c-...", "text": "Hello, Avro!", "sentAt": "2026-11-09T17:51:05.737Z", "sender": "unknown"}
Received messages-0 at offset 1: header 0000000002, schema id 2, schema version 2, message {"id": "b04771ff-...", "text": "Hello", "sentAt": "2026-11-09T17:51:08.159Z", "sender": "avro-producer"}
```

`header` is the first 5 bytes of the payload in hex: the magic byte `00`, then the schema ID as a
4-byte big-endian integer.

### In Schema Registry

```bash
curl localhost:8081/subjects                          # ["messages-value"]
curl localhost:8081/subjects/messages-value/versions  # [1,2]
curl localhost:8081/subjects/messages-value/versions/2
curl localhost:8081/schemas/ids/1
```

### In the raw payload

```bash
docker compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server kafka:19092 --topic messages --from-beginning --max-messages 2 | xxd
```

```text
00000000: 0000 0000 0148 3434 6631 3630 3563 2d64  .....H44f1605c-d
...
00000030: 2c20 4176 726f 2192 98fb 96b8 680a 0000  , Avro!.....h...
00000040: 0000 0248 6230 3437 3731 6666 2d39 3636  ...Hb04771ff-966
```

The first message starts with `00 00000001` (schema 1), and the second, after the console's line
break `0a`, with `00 00000002` (schema 2).

### Stopping

```bash
docker compose down    # also deletes the topic and the registered schemas
```

## Error handling

| Situation | Result |
| --------- | ------ |
| gRPC request other than `Ping` | `INVALID_ARGUMENT`; both clients print it and exit with code 1 |
| gRPC server unreachable or slow | `UNAVAILABLE`, or `DEADLINE_EXCEEDED` after the deadline |
| Producer cannot reach Kafka or the topic | Fails after 10 seconds and exits with code 1 |
| Consumer gets a payload that is not Confluent Avro | Logs the error, skips the record and carries on |
| Consumer fails on a message for another reason | Retries twice, one second apart, then logs it and skips it |

## Tests

| Application | Tests |
| ----------- | ----- |
| `grpc-server` | `PingServiceTests`: calls through gRPC, in process |
| `grpc-client` | `PingClientTests`, `GrpcClientApplicationTests`: against a fake server on a free port |
| `grpc-client-python` | `test_ping_client.py`: against a fake server on a free port |
| `avro-producer` | `MessageSchemasTests`, `MessageFactoryTests`, `MessageProducerFailureTests`, `MessageProducerTests` (embedded Kafka) |
| `avro-consumer` | `MessageDecoderTests`, `MessageListenerTests` (embedded Kafka) |
