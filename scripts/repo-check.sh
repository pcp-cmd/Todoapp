#!/usr/bin/env bash
set -euo pipefail

bad_files="$(git ls-files | grep -Ei '\.(apk|aab|jks|keystore)$|local\.properties|keystore\.properties' || true)"

if [[ -n "$bad_files" ]]; then
  echo "Do not commit generated packages or local signing files:"
  echo "$bad_files"
  exit 1
fi

echo "Repository check passed."
