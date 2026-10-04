#!/usr/bin/env bash
# Shared helpers for the CI check scripts. Source it, do not execute it.

# The F-Droid recipe (fdroiddata metadata/de.ptrlx.oneshot.yml) builds `subdir: app`, and
# `fdroid checkupdates` reads versionCode/versionName with regexes from this file. Keep both literal.
app_gradle_file() {
  if [ -f app/build.gradle.kts ]; then echo app/build.gradle.kts; else echo app/build.gradle; fi
}

version_code() {
  grep -E '^\s*versionCode\s*=?\s*[0-9]+' "$(app_gradle_file)" | grep -oE '[0-9]+' | head -n 1
}

version_name() {
  grep -E "^\s*versionName\s*=?\s*[\"'][^\"']+[\"']" "$(app_gradle_file)" \
    | sed -E "s/^[^\"']*[\"']([^\"']+)[\"'].*/\1/" | head -n 1
}

error() {
  echo "::error::$*"
  FAILED=1
}
