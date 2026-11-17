#!/usr/bin/env bash
# Downloads the sherpa-onnx Android library (Apache-2.0) used for on-device Whisper.
# Pinned to one version and verified by SHA-256, so every build uses the exact same file.
set -euo pipefail

VERSION="2.0.1"
SHA256="a8d09d1d32f3eb8520482c6d3d43fae674681c5b050385967c70a3c2c9bf0ee7"
URL="https://github.com/k2-fsa/sherpa-onnx/releases/download/v${VERSION}/sherpa-onnx-${VERSION}.aar"

DEST_DIR="$(cd "$(dirname "$0")/.." && pwd)/app/libs"
DEST="${DEST_DIR}/sherpa-onnx-${VERSION}.aar"

sha_of() { if command -v sha256sum >/dev/null 2>&1; then sha256sum "$1" | cut -d' ' -f1; else shasum -a 256 "$1" | cut -d' ' -f1; fi; }

mkdir -p "${DEST_DIR}"
if [[ -f "${DEST}" && "$(sha_of "${DEST}")" == "${SHA256}" ]]; then
  echo "sherpa-onnx ${VERSION} already present."
  exit 0
fi

echo "Downloading sherpa-onnx ${VERSION} (about 49 MB)..."
curl -fL --retry 3 -o "${DEST}.part" "${URL}"
if [[ "$(sha_of "${DEST}.part")" != "${SHA256}" ]]; then
  rm -f "${DEST}.part"
  echo "Checksum mismatch for sherpa-onnx ${VERSION}. Refusing to use the download." >&2
  exit 1
fi
mv "${DEST}.part" "${DEST}"
echo "Saved ${DEST}"
