#!/usr/bin/env bash
# Runs a command against the booted emulator and then makes sure the emulator process is gone.
# Usage: scripts/ci/with-emulator.sh <command> [args...]
#
# android-emulator-runner spawns the emulator as a child of the step, so the step only finishes once
# that process releases its output pipe. The action's own cleanup just logs failures of
# `adb emu kill`, and on API 29 the emulator answers that console command with "stop: Not
# implemented" and keeps running, so the step hung until the job timeout even though the tests had
# already passed. Stopping the process here, while the step is still ours, avoids that.
set -uo pipefail

"$@"
status=$?
echo "Command exited with $status, stopping the emulator"

# Matched on the process name, not the full command line: `pgrep -f` would also match any shell
# whose arguments happen to mention the emulator, including this step's own.
emulator_running() { pgrep '^qemu-system' >/dev/null; }

adb emu kill || true
for _ in $(seq 1 30); do
  emulator_running || break
  sleep 1
done
if emulator_running; then
  echo "Emulator ignored the console kill, sending SIGKILL"
  pkill -9 '^qemu-system' || true
fi
adb kill-server || true

exit "$status"
