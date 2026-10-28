#!/usr/bin/env bash
# Prints the CHANGELOG.md section for one version, without its heading.
# Usage: changelog-section.sh <version> <changelog>
set -euo pipefail

version="$1"
file="$2"

notes="$(awk -v v="$version" '
  /^## \[/ {
    if (found) exit
    if (index($0, "## [" v "]") == 1) { found = 1; next }
  }
  found { print }
' "$file")"

if [ -z "${notes//[$'\t\r\n ']/}" ]; then
  echo "No CHANGELOG.md entry for $version" >&2
  exit 1
fi

printf '%s\n' "$notes"
