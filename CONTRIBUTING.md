# How to Contribute

Instructions on how to contribute to Quill project.

## Building the project

The only dependency you need to build Quill locally is [Docker](https://www.docker.com/).
Instructions on how to install Docker can be found in this [page](https://docs.docker.com/mac/).

After installing Docker, you have to run the command bellow in order to setup the
databases' schemas. If you don't change any schemas, you will only need to this once.

`docker-compose run setup`

After that, just run the command bellow to build and test the project.

`docker-compose run sbt sbt test`

## Changing database schema

If you have changed any file that creates a database schema, you will
 have to setup the databases again. To do this, just run the command bellow.

`docker-compose stop && docker-compose rm && docker-compose run setup`

## Pull Request

In order to contribute to the project, just do as follows:

1. Fork the project
2. Build it locally
3. Code
4. Run the tests through `docker-compose run sbt sbt test`
5. If everything is ok, commit and push to your fork
6. Create a Pull Request, we'll be glad to review it

