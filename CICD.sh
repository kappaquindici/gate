#!/bin/bash -xe

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

#############
# Environment

ARCH_RAW="$(uname -m)"

if [ "$ARCH_RAW" = "aarch64" ] || [ "$ARCH_RAW" = "arm64" ]; then
  ARCH="arm64"
elif [ "$ARCH_RAW" = "x86_64" ]; then
  ARCH="x86_64"
else
  ARCH="$ARCH_RAW"
fi

APP_NAME="${APP_NAME:-gate}"
DOCKER_ORG="${DOCKER_ORG:-}"
DOCKERFILE_PATH="${DOCKERFILE_PATH:-Dockerfile}"
IMAGE_NAME="${IMAGE_NAME:-$APP_NAME}"
IMAGE_PREFIX=""

if [ -n "$DOCKER_ORG" ]; then
  IMAGE_PREFIX="$DOCKER_ORG/"
fi

GIT_SHA="$(git rev-parse --short HEAD 2>/dev/null || echo local)"
CANDIDATE_TAG="${CANDIDATE_TAG:-candidate-$GIT_SHA-$ARCH}"
PROMOTE_TAGS="${PROMOTE_TAGS:-latest $ARCH}"

######################
# Supporting functions

build_candidate_image() {
  local image_ref="${IMAGE_PREFIX}${IMAGE_NAME}:${CANDIDATE_TAG}"
  echo "[ci] build immagine Docker ${image_ref}"
  echo "[ci] compilazione applicativa eseguita dentro il Dockerfile"
  docker build -t "$image_ref" -f "$DOCKERFILE_PATH" .
}

promote_candidate_image() {
  local source_ref="${IMAGE_PREFIX}${IMAGE_NAME}:${CANDIDATE_TAG}"
  local promote_tag

  for promote_tag in $PROMOTE_TAGS; do
    local target_ref="${IMAGE_PREFIX}${IMAGE_NAME}:${promote_tag}"
    docker tag "$source_ref" "$target_ref"
    echo "[ci] promoted ${source_ref} -> ${target_ref}"
  done
}

push_images() {
  local image_ref="${IMAGE_PREFIX}${IMAGE_NAME}:${CANDIDATE_TAG}"
  local promote_tag

  docker push "$image_ref"

  for promote_tag in $PROMOTE_TAGS; do
    docker push "${IMAGE_PREFIX}${IMAGE_NAME}:${promote_tag}"
  done
}

###############
# Docker login

if [ ! -f ~/.docker/config.json ] && [ -n "${DOCKER_LOGIN:-}" ]; then
  mkdir -p ~/.docker
  cat << EOF > ~/.docker/config.json
{
  "experimental": "enabled",
  "auths": {
    "https://index.docker.io/v1/": {
      "auth": "$DOCKER_LOGIN"
    }
  }
}
EOF
fi

########
# Build

echo "[ci] architettura rilevata: $ARCH_RAW -> $ARCH"
echo "[ci] candidate tag: $CANDIDATE_TAG"

build_candidate_image
promote_candidate_image

if [ "${DOCKER_PUSH:-0}" = "1" ]; then
  echo "[ci] push immagini Docker"
  push_images
else
  echo "[ci] push disabilitato: imposta DOCKER_PUSH=1 per pubblicare le immagini"
fi

############
# Cleanup

if [ "${DOCKER_CLEANUP:-0}" = "1" ]; then
  docker image prune -f
  docker container prune -f
fi
