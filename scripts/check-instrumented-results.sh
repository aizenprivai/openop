#!/usr/bin/env bash
# Fails if instrumented tests did not really run.
#
# AGP's connected test task can report BUILD SUCCESSFUL when an APK fails to
# install and 0 tests run. This script reads the JUnit XML results and fails
# unless every expected module ran at least one test with no failures.
#
# Usage: scripts/check-instrumented-results.sh [module ...]
# Default modules: app core-bridge
set -euo pipefail

cd "$(dirname "$0")/.."
modules=("$@")
[ ${#modules[@]} -eq 0 ] && modules=(app core-bridge)

status=0
for module in "${modules[@]}"; do
  dir="$module/build/outputs/androidTest-results/connected"
  mapfile -d '' files < <(find "$dir" -name 'TEST-*.xml' -print0 2>/dev/null || true)
  if [ ${#files[@]} -eq 0 ]; then
    echo "FAIL $module: no instrumented test results in $dir"
    status=1
    continue
  fi
  for f in "${files[@]}"; do
    tests=$(grep -o 'tests="[0-9]*"' "$f" | head -1 | grep -o '[0-9]*')
    failures=$(grep -o 'failures="[0-9]*"' "$f" | head -1 | grep -o '[0-9]*')
    errors=$(grep -o 'errors="[0-9]*"' "$f" | head -1 | grep -o '[0-9]*')
    name=$(basename "$f")
    if [ "${tests:-0}" -eq 0 ]; then
      echo "FAIL $module ($name): 0 tests ran (APK install failure?)"
      status=1
    elif [ "${failures:-0}" -ne 0 ] || [ "${errors:-0}" -ne 0 ]; then
      echo "FAIL $module ($name): $failures failures, $errors errors"
      status=1
    else
      echo "OK   $module ($name): $tests tests"
    fi
  done
done
exit $status
