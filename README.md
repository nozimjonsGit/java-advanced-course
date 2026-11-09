# Java Advanced Backend Course

Homework for the Java advanced backend course. Each module lives in its own self-contained
folder with its own build, and is submitted as a pull request.

## Modules

| Module | Topic | Status |
| ------ | ----- | ------ |
| [Module 1](module-1/README.md) | Spring Boot | Ready for review |
| [Module 2](module-2/README.md) | gRPC and Avro | Ready for review |

## Repository layout

```text
.
├── .github/             # PR template
└── module-N/            # One folder per module
    ├── Homework.md      # Task description, as given by the course
    ├── README.md        # Task status and how to run this module's work
    └── ...              # The module's projects, with their own build
```

## Prerequisites

- Java 25
- Docker, to run PostgreSQL (module 1) and Kafka (module 2)
- Python 3.10 or later, only for module 2's Python gRPC client

Each module includes the Maven wrapper, so Maven does not need to be installed.
