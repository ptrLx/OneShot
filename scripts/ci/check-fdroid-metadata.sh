#!/usr/bin/env bash
# Static checks for everything the F-Droid build recipe and `fdroid checkupdates` rely on.
# See docs/modernization/RELEASING.md for the background of each rule.
set -euo pipefail
cd "$(dirname "$0")/../.."
. scripts/ci/lib.sh
FAILED=0

gradle_file=$(app_gradle_file)
fastlane=fastlane/metadata/android/en-US

# Exactly one literal versionCode / versionName (F-Droid parses them with regexes).
[ "$(grep -cE '^\s*versionCode\b' "$gradle_file")" = 1 ] || error "$gradle_file must contain exactly one literal versionCode"
[ "$(grep -cE '^\s*versionName\b' "$gradle_file")" = 1 ] || error "$gradle_file must contain exactly one literal versionName"
vc=$(version_code || true)
vn=$(version_name || true)
[[ "$vc" =~ ^[0-9]+$ ]] || error "versionCode is not a literal integer in $gradle_file"
[[ "$vn" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] || error "versionName '$vn' is not X.Y.Z (tags must be v<versionName> for UpdateCheckMode: Tags)"
echo "versionName=$vn versionCode=$vc"

# Existing installs only upgrade in place with the same applicationId.
grep -qE "applicationId\s*=?\s*[\"']de\.ptrlx\.oneshot[\"']" "$gradle_file" || error "applicationId must stay de.ptrlx.oneshot"

# F-Droid shows fastlane/.../changelogs/<versionCode>.txt (max 500 chars).
changelog="$fastlane/changelogs/$vc.txt"
if [ -f "$changelog" ]; then
  len=$(wc -m < "$changelog")
  [ "$len" -le 500 ] || error "$changelog has $len characters (F-Droid limit: 500)"
else
  error "missing $changelog for versionCode $vc"
fi
[ "$(wc -m < "$fastlane/short_description.txt")" -le 80 ] || error "short_description.txt exceeds 80 characters"

# F-Droid builds with its own Gradle matching the wrapper version: only official distributions.
grep -qE '^distributionUrl=https\\://services\.gradle\.org/distributions/gradle-[0-9.]+-(bin|all)\.zip$' \
  gradle/wrapper/gradle-wrapper.properties || error "gradle wrapper must use an official services.gradle.org release"

# No proprietary dependencies (F-Droid scanner would reject the build).
build_files=$(git ls-files '*.gradle' '*.gradle.kts' 'gradle/*.toml')
if grep -nE 'com\.google\.android\.gms|com\.google\.firebase|crashlytics|play-services|com\.google\.android\.play' $build_files; then
  error "proprietary Google dependencies are not allowed (F-Droid)"
fi

# Toolchain auto-provisioning downloads JDKs at build time, which breaks reproducible F-Droid builds.
if grep -nE 'foojay-resolver' $build_files; then
  error "do not use the foojay toolchain resolver; F-Droid provides the JDK"
fi

exit "$FAILED"
