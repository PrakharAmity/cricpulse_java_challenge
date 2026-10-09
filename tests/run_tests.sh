#!/usr/bin/env bash
set -e
cd "$(dirname "$0")/.."

if grep -qi microsoft /proc/version 2>/dev/null; then
  cmd.exe /c "mvn -q test-compile" 1>&2
  exec cmd.exe /c "java -cp target/classes;target/test-classes com.cricpulse.RunTests"
fi

mvn -q test-compile 1>&2
CP="target/classes:target/test-classes"
if [[ "$OSTYPE" == "msys"* || "$OSTYPE" == "cygwin"* || "$OSTYPE" == "win32"* ]]; then
  CP="target/classes;target/test-classes"
fi
exec java -cp "$CP" com.cricpulse.RunTests
