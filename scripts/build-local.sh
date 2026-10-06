#!/usr/bin/env bash
# Builds the Spring Boot jar and a Docker image for local running.
#
# Usage: scripts/build-local.sh [--with-tests] [--run]
# Env:   IMAGE_NAME (default: invento-core-service)
#        IMAGE_TAG  (default: local)
#        HOST_PORT  (default: 9090, used with --run)
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

IMAGE_NAME="${IMAGE_NAME:-invento-core-service}"
IMAGE_TAG="${IMAGE_TAG:-local}"
HOST_PORT="${HOST_PORT:-9090}"
RUN_TESTS=false
RUN_CONTAINER=false

for arg in "$@"; do
  case "$arg" in
    --with-tests) RUN_TESTS=true ;;
    --run) RUN_CONTAINER=true ;;
    -h|--help) sed -n '2,6p' "$0"; exit 0 ;;
    *) echo "Unknown option: $arg" >&2; exit 1 ;;
  esac
done

command -v docker >/dev/null 2>&1 || { echo "docker is not installed or not on PATH" >&2; exit 1; }

echo "==> Building jar"
if [ "$RUN_TESTS" = true ]; then
  ./gradlew clean bootJar test
else
  ./gradlew clean bootJar
fi

JAR="build/libs/invento-core-service-0.0.1-SNAPSHOT.jar"
[ -f "$JAR" ] || { echo "Expected jar not found: $JAR" >&2; exit 1; }

echo "==> Building docker image ${IMAGE_NAME}:${IMAGE_TAG}"
docker build -t "${IMAGE_NAME}:${IMAGE_TAG}" .

echo "==> Built ${IMAGE_NAME}:${IMAGE_TAG}"

if [ "$RUN_CONTAINER" = true ]; then
  echo "==> Running ${IMAGE_NAME}:${IMAGE_TAG} on port ${HOST_PORT}"
  docker run --rm -p "${HOST_PORT}:9090" --name "${IMAGE_NAME}" "${IMAGE_NAME}:${IMAGE_TAG}"
else
  echo "Run with: docker run --rm -p ${HOST_PORT}:9090 ${IMAGE_NAME}:${IMAGE_TAG}"
fi
