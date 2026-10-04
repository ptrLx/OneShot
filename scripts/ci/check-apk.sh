#!/usr/bin/env bash
# Checks on a built APK: package name, versionCode and the "no internet" promise of the app.
# Usage: scripts/ci/check-apk.sh <path/to.apk>
set -euo pipefail
apk=$(realpath "${1:?usage: check-apk.sh <apk>}")
cd "$(dirname "$0")/../.."
. scripts/ci/lib.sh
FAILED=0

aapt2="$(ls -d "${ANDROID_HOME:?ANDROID_HOME not set}"/build-tools/* | sort -V | tail -n 1)/aapt2"
badging=$("$aapt2" dump badging "$apk")

pkg=$(sed -nE "s/^package: name='([^']+)'.*/\1/p" <<<"$badging")
vc=$(sed -nE "s/^package: .*versionCode='([0-9]+)'.*/\1/p" <<<"$badging")
echo "APK package=$pkg versionCode=$vc"
[ "$pkg" = de.ptrlx.oneshot ] || error "APK package is '$pkg', expected de.ptrlx.oneshot"
[ "$vc" = "$(version_code)" ] || error "APK versionCode $vc does not match $(app_gradle_file)"

# The store description promises that OneShot has no internet permission.
if "$aapt2" dump permissions "$apk" | grep -q 'android.permission.INTERNET'; then
  error "APK requests android.permission.INTERNET (merged from a dependency?)"
fi

exit "$FAILED"
