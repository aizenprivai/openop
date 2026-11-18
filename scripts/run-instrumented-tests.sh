#!/usr/bin/env bash
# Runs instrumented tests on the connected device/emulator and checks that
# they really ran. Deletes old result files first so a failed run can never
# pass on stale results.
#
# Usage: scripts/run-instrumented-tests.sh [module ...]
# Default modules: app core-bridge core-ui
set -euo pipefail

cd "$(dirname "$0")/.."
modules=("$@")
[ ${#modules[@]} -eq 0 ] && modules=(app core-bridge core-ui)

tasks=()
for module in "${modules[@]}"; do
  rm -rf "$module/build/outputs/androidTest-results/connected"
  tasks+=(":$module:connectedDebugAndroidTest")
done

# Gradle's exit status is kept, but the results check always runs too.
gradle_status=0
./gradlew --console=plain "${tasks[@]}" || gradle_status=$?
check_status=0
scripts/check-instrumented-results.sh "${modules[@]}" || check_status=$?

[ $gradle_status -eq 0 ] && [ $check_status -eq 0 ]
