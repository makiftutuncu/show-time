# Show Time

## Table of Contents

1. [Introduction](#introduction)
2. [Configuration](#configuration)
3. [Development and Testing](#development-and-testing)
4. [Docker](#docker)
5. [API Documentation](#api-documentation)
6. [Contributing](#contributing)
7. [License](#license)

## Introduction

Show Time is a web backend application, providing APIs to manage tickets to shows and musicals. It is built using [Scala 2.13](https://www.scala-lang.org), [ZIO](https://zio.dev) and [http4s](https://http4s.org/).

## Configuration

Application can be configured via [application.conf](src/main/resources/application.conf). You can also override config values with following environment variables.

TODO

For log configuration, see [logback.xml](src/main/resources/logback.xml).

## Development and Testing

Application is built with SBT. So, standard SBT tasks like `clean`, `compile` and `run` can be used. To run the application locally:

```bash
sbt run
```

To run automated tests, you can use `test` and `testOnly` tasks of SBT. To run all tests:

```bash
sbt test
```

To run specific test(s):

```bash
sbt 'testOnly fullyQualifiedTestClassName1 fullyQualifiedTestClassName2 ...'
```

## Docker

You may also run the application in a Docker container. Environment variables mentioned in [Configuration](#configuration) are passed to the container while building image so you don't have to pass them with `-e` while creating the container with `docker run`.

First build an image locally with

```bash
sbt 'Docker / publishLocal'
```

Then start a container from generated Docker image with

```bash
docker run --rm show-time:latest
```

## API Documentation

TODO

## Contributing

All contributions are welcome. Please feel free to send a pull request. Thank you.

## License

Show Time is licensed with [MIT License](LICENSE.md).
