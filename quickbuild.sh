#!/bin/bash
set -e -o pipefail -u
mvn clean install --no-snapshot-updates -DskipTests -T4
