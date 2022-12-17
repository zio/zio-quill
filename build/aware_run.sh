#!/bin/bash
SBT_COMMAND=$1

if [ -z "$SBT_COMMAND" ]
then
  	echo "== Command Not Specified =="
  	exit 1
fi

echo "==Starting> $SBT_COMMAND"
{ $SBT_COMMAND >> log.txt; } &
RESULT=$!
{ ./build/sysinfo_loop.sh >> log.txt; } &
{ tail -f log.txt; } &
wait $RESULT
SBT_EXIT=$?
touch done.txt
echo "========== Writing Done ======"
pkill -P $$
ps -eaf | grep sysinfo_loop | grep -v grep | awk '{print $2}' | xargs kill -9
ps -eaf | grep "tail -f log.txt" | grep -v grep | awk '{print $2}' | xargs kill -9
echo "SBT EXIT $SBT_EXIT"
exit $SBT_EXIT
