#!/usr/bin/env bash
# List changed files that belong to the persisted-data contract (.github/data-contract-paths.txt).
# Usage: scripts/ci/check-data-contract.sh <base-sha> <head-sha>
# Exit 0 when nothing in the contract changed, 2 when something did.
set -euo pipefail
cd "$(dirname "$0")/../.."
base="${1:?base sha}"
head="${2:?head sha}"
mapfile -t paths < <(grep -vE '^\s*(#|$)' .github/data-contract-paths.txt)
changed=$(git diff --name-only "$base...$head" -- "${paths[@]}")
if [ -n "$changed" ]; then
  echo "Changed data-contract paths:"
  echo "$changed" | sed 's/^/  /'
  exit 2
fi
echo "No data-contract paths changed."
