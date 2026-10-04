#!/usr/bin/env bash
# Compare two unsigned APKs byte by byte; on mismatch list the entries whose CRC differs.
# Usage: scripts/ci/compare-apks.sh <a.apk> <b.apk>
set -euo pipefail
a="${1:?a.apk}"
b="${2:?b.apk}"
if cmp -s "$a" "$b"; then
  echo "APKs are byte-identical: $(sha256sum "$a" | cut -d' ' -f1)"
  exit 0
fi
echo "::error::APKs differ: the build is not reproducible across environments."
entries() { unzip -v "$1" | awk 'NF == 8 && $7 ~ /^[0-9a-f]{8}$/ { print $7, $8 }' | sort -k2; }
diff <(entries "$a") <(entries "$b") || true
exit 1
