#!/bin/bash

# From 'All In One' of Quill CONTRIBUTING.md
docker compose down && docker compose build && docker compose run --rm --service-ports setup

# echo "Adding 50ms latency to protoquill_postgres_1"
# docker exec protoquill_postgres_1 tc qdisc add dev eth0 root netem delay 50ms

# echo "Adding 50ms latency to protoquill_mysql_1"
# docker exec protoquill_mysql_1 tc qdisc add dev eth0 root netem delay 50ms
