#!/usr/bin/env bash
# Run after a Gradle build: Room re-exports its schema into app/db-schema on every compile.
# Any difference means the persisted database format changed (see docs/modernization/DATA_CONTRACT.md).
set -euo pipefail
cd "$(dirname "$0")/../.."
FAILED=0
. scripts/ci/lib.sh

if [ -n "$(git status --porcelain -- app/db-schema)" ]; then
  git status --porcelain -- app/db-schema
  git --no-pager diff -- app/db-schema
  error "the build regenerated app/db-schema differently: the Room schema changed. Bump the DB version, add a Migration + MigrationTestHelper test and commit the new schema (label data-change-approved)."
fi

# The schema of version 1 is what every existing installation has on disk. It must never change.
v1=$(ls app/db-schema/*/1.json)
grep -q '"identityHash": "78c91e56607bf67a0e206c573e5cd700"' "$v1" || error "identityHash of schema version 1 changed in $v1"

[ "$FAILED" = 0 ] && echo "Room schema unchanged."
exit "$FAILED"
