#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"
exec mvn spring-boot:run
