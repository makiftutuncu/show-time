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

Here is an overview of the APIs:

| Method | URL               | Link                          |
|--------|-------------------|-------------------------------|
| GET    | /inventory/{date} | [Jump](#get-inventorydate)    |
| POST   | /inventory/order  | [Jump](#post-inventory-order) |

Errors return a human-readable message as plain text with a non-OK HTTP status code.

All successful responses will have `200 OK` status unless explicitly mentioned.

---

### GET /inventory/{date}

This endpoint can be used to list the availabilities of shows for given `date`. Shows are grouped by their genre and sorted by

* `tickets_avaliable` in ascending order, except for sold-out tickets which would be at the end of the list
* `price` in ascending order
* `title` in ascending order

#### Parameters

| Parameter | Data Type | Description                                                     | Required | Example    |
|-----------|-----------|-----------------------------------------------------------------|----------|------------|
| date      | LocalDate | ISO formatted date for which show availabilities will be listed | Yes      | 2022-09-15 |

#### Example Successful Response

```json
{
  "inventory": [
    {
      "genre": "drama",
      "shows": [
        {
          "price": 40,
          "tickets_available": 100,
          "title": "EVENING AT THE TALK HOUSE"
        },
        {
          "price": 40,
          "tickets_available": 100,
          "title": "FATHER, THE"
        },
        {
          "price": 40,
          "tickets_available": 100,
          "title": "LORD OF THE FLIES, THE"
        },
        {
          "price": 40,
          "tickets_available": 100,
          "title": "RICHARD II"
        },
        {
          "price": 40,
          "tickets_available": 100,
          "title": "THREE DAYS IN THE COUNTRY"
        }
      ]
    },
    {
      "genre": "musical",
      "shows": [
        {
          "price": 70,
          "tickets_available": 100,
          "title": "BEAUTIFUL - THE CAROLE KING MUSICAL"
        },
        {
          "price": 70,
          "tickets_available": 100,
          "title": "COMMITMENTS, THE"
        },
        {
          "price": 70,
          "tickets_available": 100,
          "title": "GYPSY"
        },
        {
          "price": 70,
          "tickets_available": 100,
          "title": "SINATRA AT THE LONDON PALLADIUM"
        }
      ]
    }
  ]
}
```

### POST /inventory/order

This endpoint can be used to place an order for a show on a date for given number of tickets. It returns a summary of the operation, including remaining available tickets if the order was successful.

#### Request Payload

```json
{
  "show": "EVENING AT THE TALK HOUSE",
  "performance_date": "2022-09-15",
  "tickets": 1
}
```

#### Example Successful Response

```json
{
    "performance_date": "2022-10-13",
    "show": "1984",
    "status": "success",
    "tickets_available": 12,
    "tickets_bought": 2
}
```

#### Example Failed Response

```json
{
    "message": "Cannot place order for show '1984' on date '2022-10-13', 5 tickets requested but the show has 2 tickets left",
    "performance_date": "2022-10-13",
    "show": "1984",
    "status": "failure"
}
```

## Contributing

All contributions are welcome. Please feel free to send a pull request. Thank you.

## License

Show Time is licensed with [MIT License](LICENSE.md).
