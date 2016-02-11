# How to Contribute

Instructions on how to contribute to Quill project.

## Building the project

The only dependency you need to build Quill locally is [Docker](https://www.docker.com/).
Instructions on how to install Docker can be found in this [page](https://docs.docker.com/mac/).

After installing Docker, you have to run the command bellow in order to setup the
databases' schemas. If you don't change any schemas, you will only need to this once.

`docker-compose run --rm setup`

After that, just run the command bellow to build and test the project.

`docker-compose run --rm sbt sbt test`

## Changing database schema

If you have changed any file that creates a database schema, you will
 have to setup the databases again. To do this, just run the command bellow.

`docker-compose stop && docker-compose rm && docker-compose run --rm setup`

## Pull Request

In order to contribute to the project, just do as follows:

1. Fork the project
2. Build it locally
3. Code
4. Run the tests through `docker-compose run sbt sbt test`
5. If everything is ok, commit and push to your fork
6. Create a Pull Request, we'll be glad to review it


## Running locally

### For Mac users

#### Improve build performance with Docker

Please, install [docker-machine-nfs](https://github.com/adlogix/docker-machine-nfs). It will change the default sharing
of your Docker-Machine from Virtual Box Shared Folders to NFS, which is a lot faster. 

#### Build locally, without docker

Run the following command, it will restart your database service with random ports exposed to your host machine. 

`docker-compose stop && docker-compose rm && docker-compose run --rm --service-ports setup`

After that, you have to find the ports that were associated with the services with the following command.

`docker ps`

With that, just export the following variables with the corresponding ports from the previous command.

```
export CASSANDRA_PORT_9042_TCP_ADDR=192.168.99.100
export CASSANDRA_PORT_9042_TCP_PORT=<Cassandra exposed port> 
export MYSQL_PORT_3306_TCP_ADDR=192.168.99.100
export MYSQL_PORT_3306_TCP_PORT=<MySQL exposed port> 
export POSTGRES_PORT_5432_TCP_ADDR=192.168.99.100 
export POSTGRES_PORT_5432_TCP_PORT=<Postgres exposed port>
```

*Note on the 192.168.99.100 address, usually [docker-machine](https://docs.docker.com/machine/) creates a Virtual Machine
with this address. You can check it by running `docker-machine ps`.*

Finally, you can use `sbt` locally.

### For Linux users
#### Build locally, without docker

Run the following command, it will restart your database service with random ports exposed to your host machine. 

`docker-compose stop && docker-compose rm && docker-compose run --rm --service-ports setup`

After that, you have to find the ports that were associated with the database services with the following command.

`docker ps`

With that, just export the following variables with the corresponding ports from the previous command.

```
export CASSANDRA_PORT_9042_TCP_ADDR=127.0.0.1
export CASSANDRA_PORT_9042_TCP_PORT=<Cassandra exposed port> 
export MYSQL_PORT_3306_TCP_ADDR=127.0.0.1
export MYSQL_PORT_3306_TCP_PORT=<MySQL exposed port> 
export POSTGRES_PORT_5432_TCP_ADDR=127.0.0.1 
export POSTGRES_PORT_5432_TCP_PORT=<Postgres exposed port>
```

Finally, you can use `sbt` locally.