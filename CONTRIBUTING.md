# How to Contribute

Instructions on how to contribute to Quill project.

## Building the project

The only dependency you need to build Quill locally is [Docker](https://www.docker.com/).
Instructions on how to install Docker can be found in this [page](https://docs.docker.com/mac/).

If you are running Linux, you should also install Docker Compose separately, as described
[here](https://docs.docker.com/compose/install/).

After installing Docker and Docker Compose, you have to run the command bellow in
order to setup the databases' schemas. If you don't change any schemas, you will
only need to do this once.

```
docker-compose run --rm setup
```

After that, just run the command bellow to build and test the project.

```
docker-compose run --rm sbt sbt test
```

### Improve build performance with Docker *(for Mac users only)*

Use [Docker for mac](https://docs.docker.com/engine/installation/mac/#/docker-for-mac).

## Building Scala.js targets

The Scala.js targets are disabled by default, use `sbt "project quill-with-js"` to enable them.
The CI build also sets this `project quill-with-js` to force the Scala.js compilation.

## Changing database schema

If you have changed any file that creates a database schema, you will
have to setup the databases again. To do this, just run the command bellow.

```
docker-compose stop && docker-compose rm && docker-compose run --rm setup
```

## Tests

### Running tests

Run all tests:
```
docker-compose run --rm sbt sbt test
```

Run specific test:
```
docker-compose run --rm sbt sbt "test-only io.getquill.context.sql.SqlQuerySpec"
```

Run all tests in specific sub-project:
```
docker-compose run --rm sbt sbt "project quill-async" test
```

Run specific test in specific sub-project:
```
docker-compose run --rm sbt sbt "project quill-sqlJVM" "test-only io.getquill.context.sql.SqlQuerySpec"
```

### Debugging tests
1. Run sbt in interactive mode with docker container ports mapped to the host: 
```
docker-compose run --service-ports --rm sbt
```

2. Attach debugger to port 15005 of your docker host. In IntelliJ IDEA you should create Remote Run/Debug Configuration, 
change it port to 15005.
3. In sbt command line run tests with `test` or test specific spec by passing full name to `test-only`:
```
> test-only io.getquill.context.sql.SqlQuerySpec
```

## Pull Request

In order to contribute to the project, just do as follows:

1. Fork the project
2. Build it locally
3. Code
4. Compile (file will be formatted)
5. Run the tests through `docker-compose run sbt sbt test`
6. If everything is ok, commit and push to your fork
7. Create a Pull Request, we'll be glad to review it

## File Formatting 

[Scalariform](http://mdr.github.io/scalariform/) is used as file formatting tool in this project.
Every time you compile the project in sbt, file formatting will be triggered.

## Building locally without Docker

Run the following command, it will restart your database service with database ports exposed to your host machine. 

```
docker-compose stop && docker-compose rm && docker-compose run --rm --service-ports setup
```

After that, we need to set some environment variables in order to run `sbt` locally.  

```
export CASSANDRA_HOST=<docker host address>
export CASSANDRA_PORT=19042
export MYSQL_HOST=<docker host address>
export MYSQL_PORT=13306
export POSTGRES_HOST=<docker host address>
export POSTGRES_PORT=15432
```

For Mac users, the docker host address is the address of the [docker-machine](https://docs.docker.com/machine/),
it's usually 192.168.99.100. You can check it by running `docker-machine ps`. For Linux users, the host address
is your localhost.

Therefore, for Mac users the environment variables should be:

```
export CASSANDRA_HOST=192.168.99.100
export CASSANDRA_PORT=19042
export MYSQL_HOST=192.168.99.100
export MYSQL_PORT=13306
export POSTGRES_HOST=192.168.99.100
export POSTGRES_PORT=15432
```

For Linux users, the environment variables should be:

```
export CASSANDRA_HOST=127.0.0.1
export CASSANDRA_PORT=19042
export MYSQL_HOST=127.0.0.1
export MYSQL_PORT=13306
export POSTGRES_HOST=127.0.0.1
export POSTGRES_PORT=15432
```

Finally, you can use `sbt` locally.
