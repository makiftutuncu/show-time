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

Show Time is a web backend application, providing APIs to manage tickets to shows and musicals. It is built using [Scala 2.13](https://www.scala-lang.org), [ZIO](https://zio.dev) and [ZIO HTTP](https://dream11.github.io/zio-http/).

## Configuration

Application can be configured via [application.conf](src/main/resources/application.conf). You can also override config values with following environment variables.

| Variable Name         | Data Type | Description                                                              | Required               |
|-----------------------|-----------|--------------------------------------------------------------------------|------------------------|
| PORT                  | Int       | Running port of the application                                          | No, defaults to `8080` |
| SHOW_DURATION_IN_DAYS | Int       | How many days do shows run                                               | No, defaults to `100`  |
| MUSICAL_PRICE         | Int       | Ticket price for musicals                                                | No, defaults to `70`   |
| COMEDY_PRICE          | Int       | Ticket price for comedies                                                | No, defaults to `50`   |
| DRAMA_PRICE           | Int       | Ticket price for dramas                                                  | No, defaults to `40`   |
| DISCOUNT_AFTER_DAYS   | Int       | After how many days of a show's opening day should a discount be applied | No, defaults to `80`   |
| DISCOUNT_PERCENTAGE   | Int       | Discount percentage to apply                                             | No, defaults to `20`   |
| THEATER_SIZE          | Int       | How many seats are there in the theater                                  | No, defaults to `100`  |

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

There is a [`run.sh`](run.sh) script included to build and run the application in Docker. So you can just run

```bash
./run.sh
```

If you want to do it manually, you may use included [`docker-compose.yml`](docker-compose.yml).

First build an image locally with

```bash
sbt clean 'Docker / publishLocal'
```

Then start the application with

```bash
docker-compose up
```

Environment variables mentioned in [Configuration](#configuration) are passed to the container except for `PORT`. `PORT` variable only affects the port on host machine while running the application in Docker. The application in the container will still run on its default port which will be mapped to given `PORT` on the host machine. For example, if you run

```bash
PORT=9000 ./run.sh
```

then the port `9000` on the host machine will be mapped to the port `8080` in the container which is the application's default port.

## API Documentation

TODO

## Contributing

All contributions are welcome. Please feel free to send a pull request. Thank you.

## License

Show Time is licensed with [MIT License](LICENSE.md).
