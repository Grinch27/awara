#!/usr/bin/env bash

# TODO(user): Decide whether wrapper sync should run before each release cut or only when the Android toolchain changes.
# TODO(agent): If the Gradle versions API schema changes, update fetch_latest_gradle_version.
# TODO(agent): If AGP stops accepting the latest stable Gradle, re-pin to the newest compatible release before changing app code.

set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
readonly SCRIPT_DIR
readonly REPO_ROOT
readonly WRAPPER_PROPERTIES="${REPO_ROOT}/gradle/wrapper/gradle-wrapper.properties"
readonly GRADLE_VERSIONS_URL="https://services.gradle.org/versions/current"
readonly MINIMUM_GRADLE_MAJOR="9"

fetch_latest_gradle_version() {
  python3 - <<'PY'
import json
import urllib.request

with urllib.request.urlopen("https://services.gradle.org/versions/current") as response:
    payload = json.load(response)

print(payload["version"])
PY
}

update_wrapper_properties() {
  local gradle_version="$1"

  python3 - "${WRAPPER_PROPERTIES}" "${gradle_version}" <<'PY'
from pathlib import Path
import re
import sys

path = Path(sys.argv[1])
gradle_version = sys.argv[2]
content = path.read_text(encoding="utf-8")
updated = re.sub(
    r"gradle-[0-9]+(?:\.[0-9]+)*-[a-z]+\.zip",
    f"gradle-{gradle_version}-bin.zip",
    content,
    count=1,
)

if updated == content:
  print(content, end="")
  raise SystemExit(0)

path.write_text(updated, encoding="utf-8")
print(updated, end="")
PY
}

main() {
  local gradle_version
  local gradle_major

  gradle_version="$(fetch_latest_gradle_version)"
  gradle_major="${gradle_version%%.*}"

  if [[ -z "${gradle_version}" ]]; then
    echo "Failed to resolve the latest stable Gradle version." >&2
    exit 1
  fi

  if (( gradle_major < MINIMUM_GRADLE_MAJOR )); then
    echo "Expected Gradle ${MINIMUM_GRADLE_MAJOR}+ but got ${gradle_version}." >&2
    exit 1
  fi

  echo "Syncing gradle-wrapper.properties to ${gradle_version}"
  echo "Using versions endpoint: ${GRADLE_VERSIONS_URL}"
  update_wrapper_properties "${gradle_version}"
}

main "$@"