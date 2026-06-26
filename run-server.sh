#!/usr/bin/env bash
# run-server.sh — Convenience wrapper to launch the Pub/Sub broker.
#
# Usage:
#   ./run-server.sh <port>
#
# Example:
#   ./run-server.sh 5000
#
# The 'out/' directory must already exist and contain the compiled .class files.
# Build first with:  mkdir -p out && javac -d out src/*.java

set -euo pipefail

java -cp out Server "$@"
