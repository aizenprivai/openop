#!/usr/bin/env bash
# Fails if instrumented tests did not really run.
#
# AGP's connected test task can report BUILD SUCCESSFUL when an APK fails to
# install and 0 tests run. This script reads the JUnit XML results and fails
# unless every expected module ran at least one test with no failures.
#
# Old result files from an earlier run would hide a failed run, so use
# scripts/run-instrumented-tests.sh, which deletes them first.
#
# Usage: scripts/check-instrumented-results.sh [module ...]
# Default modules: app core-bridge core-ui
set -euo pipefail

cd "$(dirname "$0")/.."
modules=("$@")
[ ${#modules[@]} -eq 0 ] && modules=(app core-bridge core-ui)

# Prints the first value of XML attribute $2 in file $1, or nothing.
attr() { grep -o "$2=\"[0-9]*\"" "$1" | head -1 | grep -o '[0-9]*' || true; }

status=0
for module in "${modules[@]}"; do
  dir="$module/build/outputs/androidTest-results/connected"
  files=()
  if [ -d "$dir" ]; then
    mapfile -d '' files < <(find "$dir" -name 'TEST-*.xml' -print0)
  fi
  if [ ${#files[@]} -eq 0 ]; then
    echo "FAIL $module: no instrumented test results in $dir"
    status=1
    continue
  fi
  for f in "${files[@]}"; do
    name=$(basename "$f")
    tests=$(attr "$f" tests)
    failures=$(attr "$f" failures)
    errors=$(attr "$f" errors)
    if [ -z "$tests" ] || [ -z "$failures" ] || [ -z "$errors" ]; then
      echo "FAIL $module ($name): result file is malformed"
      status=1
    elif [ "$tests" -eq 0 ]; then
      echo "FAIL $module ($name): 0 tests ran (APK install failure?)"
      status=1
    elif [ "$failures" -ne 0 ] || [ "$errors" -ne 0 ]; then
      echo "FAIL $module ($name): $failures failures, $errors errors"
      status=1
    else
      echo "OK   $module ($name): $tests tests"
    fi
  done
done
exit $status
