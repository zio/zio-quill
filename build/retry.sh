#!/usr/bin/env bash

attempts=0
max_attempts=$1
until [ "$attempts" -ge "$max_attempts" -a "$max_attempts" -ne 0 ]
do
    attempts=$(($attempts+1))
    echo $attempts
    "${@:2}" && break
done

exit $?