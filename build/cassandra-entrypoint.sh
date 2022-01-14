#!/usr/bin/env bash
##
## This script will generate a patched docker-entrypoint.sh that:
## - executes any *.sh script found in /docker-entrypoint-initdb.d
## - boots cassandra up
## - executes any *.cql script found in docker-entrypoint-initdb.d
##
## It is compatible with any cassandra:* image
##


## Create script that executes files found in docker-entrypoint-initdb.d/

cat <<'EOF' >> /run-init-scripts.sh
#!/usr/bin/env bash

LOCK=/var/lib/cassandra/_init.done
INIT_DIR=docker-entrypoint-initdb.d

if [ -f "$LOCK" ]; then
    echo "@@ Initialization already performed."
    exit 0
fi

cd $INIT_DIR

echo "@@ Executing bash scripts found in $INIT_DIR"

# execute scripts found in INIT_DIR
for f in $(find . -type f -name "*.sh" -executable -print | sort); do
    echo "$0: sourcing $f"
    . "$f"
    echo "$0: $f executed."
done

# wait for cassandra to be ready and execute cql in background
(
    while ! cqlsh -e 'describe cluster' > /dev/null 2>&1; do sleep 6; done
    echo "$0: Cassandra cluster ready: executing cql scripts found in $INIT_DIR"
    for f in $(find . -type f -name "*.cql" -print | sort); do
        echo "$0: running $f"
        cqlsh -f "$f"
        echo "$0: $f executed"
    done
    # mark things as initialized (in case /var/lib/cassandra was mapped to a local folder)
    touch $LOCK
) &

EOF

## Patch existing entrypoint to call our script in the background
# This has been inspired by https://www.thetopsites.net/article/51594713.shtml
EP=/patched-entrypoint.sh
sed '$ d' /docker-entrypoint.sh > $EP
cat <<'EOF' >> $EP
/run-init-scripts.sh &
exec "$@"
EOF

# Make both scripts executable
chmod +x /run-init-scripts.sh
chmod +x $EP

# Call the new entrypoint
$EP "$@"