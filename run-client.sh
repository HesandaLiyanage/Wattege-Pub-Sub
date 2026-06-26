#!/usr/bin/env bash
# run-client.sh — Convenience wrapper to launch a Pub/Sub client.
#
# Usage:
#   ./run-client.sh <server-ip> <port> <ROLE> [TOPIC]
#
# Examples:
#   ./run-client.sh 127.0.0.1 5000 PUBLISHER  TOPIC_A
#   ./run-client.sh 127.0.0.1 5000 SUBSCRIBER TOPIC_A
#   ./run-client.sh 127.0.0.1 5000 PUBLISHER          # defaults to TOPIC=GLOBAL
#
# The 'out/' directory must already exist and contain the compiled .class files.
# Build first with:  mkdir -p out && javac -d out src/*.java

set -euo pipefail

java -cp out Client "$@"
