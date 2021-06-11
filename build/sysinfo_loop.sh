#!/bin/bash

rm -rf done.txt
sleep 1

while [[ ! -e done.txt ]]; do
  free -m | awk 'NR==2{printf "== Memory Usage: %s/%sMB (%.2f%%)\n", $3,$2,$3*100/$2 }'
  df -h | awk '$NF=="/"{printf "== Disk Usage: %d/%dGB (%s)\n", $3,$2,$5}'
  top -bn1 | grep load | awk '{printf "== CPU Load: %.2f\n", $(NF-2)}'
  if [[ -e done.txt ]]; then
    echo "=== Done File Found! Breaking Out of sysinfo_loop ==="
    exit 0
  else
    echo "== No Done File Yet =="
  fi
  sleep 10
done
