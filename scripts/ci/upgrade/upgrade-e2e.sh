#!/usr/bin/env bash
# In-place upgrade test (docs/modernization/TESTING.md): install the old release, give it realistic
# data the way it stores it, upgrade to the new release with `adb install -r`, and require that the
# database, the DataStore settings and the images are unchanged and that the new app shows every entry.
# Needs an emulator image with root (google_apis). Usage: upgrade-e2e.sh <old.apk> <new.apk>
set -euo pipefail
trap 'echo "::error::upgrade-e2e.sh line $LINENO failed: $BASH_COMMAND"' ERR
old_apk=$1
new_apk=$2
here=$(cd "$(dirname "$0")" && pwd)
pkg=de.ptrlx.oneshot
activity=$pkg/.feature_diary.presentation.diary.MainActivity
data=/data/data/$pkg
folder=/sdcard/OneShot
tree_uri='content://com.android.externalstorage.documents/tree/primary%3AOneShot'
sdk=$(adb shell getprop ro.build.version.sdk | tr -d '\r')
out=app/build/reports/upgrade/api-$sdk
rm -rf "$out" && mkdir -p "$out"

step() { echo "::group::$*"; }
end() { echo "::endgroup::"; }
wait_boot() {
  adb wait-for-device
  until [ "$(adb shell getprop sys.boot_completed | tr -d '\r')" = 1 ]; do sleep 2; done
  sleep 5
}
launch() { adb shell am start -W -n "$activity" | grep -E "Status|Error|LaunchState" || true; sleep "${1:-6}"; }
tap() {
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  adb pull /sdcard/ui.xml "$out/ui.xml" >/dev/null 2>&1
  local bounds
  bounds=$(grep -o "content-desc=\"$1\"[^>]*bounds=\"[^\"]*\"" "$out/ui.xml" | head -1 \
    | sed -E 's/.*bounds="\[([0-9]+),([0-9]+)\]\[([0-9]+),([0-9]+)\]".*/\1 \2 \3 \4/')
  [ -n "$bounds" ] || { echo "::error::no element '$1' on screen"; return 1; }
  set -- $bounds
  adb shell input tap $(( ($1 + $3) / 2 )) $(( ($2 + $4) / 2 ))
}
cards() {
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  adb pull /sdcard/ui.xml "$out/ui.xml" >/dev/null 2>&1
  grep -o 'content-desc="Diary entry: [^"]*"' "$out/ui.xml" | wc -l
}
wait_app() {
  # After a framework restart the package manager can need a while before it resolves the activity.
  for _ in $(seq 1 30); do
    adb shell cmd package resolve-activity --brief -n "$activity" 2>/dev/null | grep -q "$pkg/" && return 0
    sleep 3
  done
  echo "::error::$activity does not resolve"
  adb shell pm path "$pkg" || true
  adb shell dumpsys package "$pkg" | grep -E "versionName|enabled|installed|stopped|suspended|hidden" | head -20 || true
  return 1
}
app_owner() { adb shell stat -c %u "$data" | tr -d '\r'; }
dump() {
  local dir=$out/$1
  mkdir -p "$dir"
  adb pull "$data/databases" "$dir/" >/dev/null
  adb pull "$data/files/datastore" "$dir/" >/dev/null
  {
    "$here/fixture.py" dump-db "$dir/databases/diary_entry_db"
    echo "datastore $(sha256sum < "$dir/datastore/diary_settings.preferences_pb" | cut -d' ' -f1)"
    adb shell "cd $folder && sha256sum *" | tr -d '\r' | sort
  } > "$dir/state.txt"
}

step "Root and install the old release"
adb root >/dev/null; wait_boot
adb uninstall $pkg >/dev/null 2>&1 || true
adb install "$old_apk"
adb shell dumpsys package $pkg | grep -m2 -E "versionName|targetSdk"
launch                                        # the old app creates its database
adb shell am force-stop $pkg
adb shell ls "$data/databases/diary_entry_db" >/dev/null || { echo "::error::old app did not create its database"; exit 1; }
end

step "Seed data like v1.1.1 stores it"
mkdir -p "$out/seed"
mkdir -p "$out/seed"
adb pull "$data/databases" "$out/seed/" >/dev/null
"$here/fixture.py" seed-db "$out/seed/databases/diary_entry_db"
adb shell rm -f "$data/databases/diary_entry_db-wal" "$data/databases/diary_entry_db-shm"
adb push "$out/seed/databases/diary_entry_db" "$data/databases/diary_entry_db" >/dev/null
"$here/fixture.py" datastore "$out/seed/diary_settings.preferences_pb" "$tree_uri"
adb shell mkdir -p "$data/files/datastore"
adb push "$out/seed/diary_settings.preferences_pb" "$data/files/datastore/" >/dev/null
uid=$(app_owner)
adb shell chown -R "$uid:$uid" "$data/databases" "$data/files"
adb shell restorecon -R "$data" >/dev/null
"$here/fixture.py" images "$out/seed/images"
adb shell mkdir -p "$folder"
adb push "$out/seed/images/." "$folder/" >/dev/null
# The persisted folder permission the system picker would have granted (read + write, tree prefix).
adb shell cat /data/system/urigrants.xml 2>/dev/null > "$out/urigrants-before.xml" || true
cat > "$out/urigrants.xml" <<XML
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<uri-grants>
<uri-grant sourceUserId="0" targetUserId="0" sourcePkg="com.android.externalstorage" targetPkg="$pkg" uri="$tree_uri" prefix="true" modeFlags="3" createdTime="1700000000000" />
</uri-grants>
XML
adb push "$out/urigrants.xml" /data/system/urigrants.xml >/dev/null
adb shell chown system:system /data/system/urigrants.xml
adb shell restorecon /data/system/urigrants.xml
adb shell stop; adb shell start; wait_boot     # the system reads the grants at boot
adb root >/dev/null; wait_boot
wait_app
adb shell dumpsys activity permissions | grep -A3 "$pkg" | head -8 || true
end

step "Old release runs with the data"
adb logcat -c || true   # some images refuse to clear a buffer
launch
tap Diary && sleep 4
adb exec-out screencap -p > "$out/1-old-diary.png"
old_cards=$(cards)
echo "old app shows $old_cards entries"
adb shell am force-stop $pkg
dump a-old
cat "$out/a-old/state.txt"
end

step "Diagnostics: image folder and permission"
doc_uri="${tree_uri}/document/primary%3AOneShot%2F$("$here/fixture.py" first-image)"
echo "document uri: $doc_uri"
echo "provider serves $(adb shell content read --uri "$doc_uri" 2>&1 | wc -c) bytes (as root)"
adb shell content query --uri "$tree_uri/document/primary%3AOneShot/children" --projection _display_name:mime_type 2>&1 | head -8 || true
adb shell dumpsys activity permissions 2>/dev/null | grep -B3 -A6 "$pkg" | head -20 || true
adb shell ls -lZ "$folder" | head -4 || true
end

step "Upgrade in place"
adb install -r "$new_apk"
adb shell dumpsys package $pkg | grep -m2 -E "versionName|targetSdk"
dump b-upgraded
end

step "New release runs with the data"
launch 8
tap Diary && sleep 4
adb exec-out screencap -p > "$out/2-new-diary.png"
new_cards=$(cards)
echo "new app shows $new_cards entries"
adb shell am force-stop $pkg
dump c-new-ran
adb logcat -d > "$out/logcat.txt"
end

failed=0
if grep -q "FATAL EXCEPTION" "$out/logcat.txt"; then
  grep -A30 "FATAL EXCEPTION" "$out/logcat.txt"
  echo "::error::the app crashed"; failed=1
fi
for state in b-upgraded c-new-ran; do
  if ! diff -u "$out/a-old/state.txt" "$out/$state/state.txt"; then
    echo "::error::data changed between the old release and $state"; failed=1
  fi
done
rows=$(grep -c '^(' "$out/a-old/state.txt")
[ "$rows" = 6 ] || { echo "::error::expected 6 seeded entries, found $rows"; failed=1; }
if [ "$new_cards" -lt "$old_cards" ] || [ "$new_cards" -eq 0 ]; then
  echo "::error::the new app shows $new_cards entries, the old one $old_cards"; failed=1
fi
[ "$failed" = 0 ] && echo "Upgrade kept all data: $rows entries, settings and images unchanged; new app shows $new_cards entries."
exit "$failed"
