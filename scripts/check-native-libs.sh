#!/usr/bin/env bash
# Checks native libraries inside an APK:
#  - every LOAD segment is 16 KB-aligned (required by 16 KB page devices);
#  - only the allowed ABIs are present.
#
# Usage: scripts/check-native-libs.sh <apk> <allowed-abi> [allowed-abi ...]
# Needs ANDROID_NDK_HOME (or ANDROID_HOME with the pinned NDK installed).
set -euo pipefail

apk=$1; shift
allowed=("$@")
[ ${#allowed[@]} -gt 0 ] || { echo "usage: $0 <apk> <abi>..." >&2; exit 2; }

root="$(cd "$(dirname "$0")/.." && pwd)"
if [ -z "${ANDROID_HOME:-}" ] && [ -f "$root/local.properties" ]; then
  ANDROID_HOME=$(grep -E '^sdk.dir=' "$root/local.properties" | cut -d= -f2-)
fi
if [ -z "${ANDROID_NDK_HOME:-}" ]; then
  ndk_version=$(grep -E '^ndk *=' "$(dirname "$0")/../gradle/libs.versions.toml" | cut -d'"' -f2)
  ANDROID_NDK_HOME="${ANDROID_HOME:?set ANDROID_HOME or ANDROID_NDK_HOME}/ndk/$ndk_version"
fi
readelf=$(find "$ANDROID_NDK_HOME/toolchains/llvm/prebuilt" -name llvm-readelf | head -1)
[ -x "$readelf" ] || { echo "llvm-readelf not found in $ANDROID_NDK_HOME" >&2; exit 2; }

work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
unzip -q "$apk" 'lib/*' -d "$work" 2>/dev/null || true

status=0
found=0
for so in "$work"/lib/*/*.so; do
  [ -e "$so" ] || continue
  found=1
  abi=$(basename "$(dirname "$so")")
  name="$abi/$(basename "$so")"
  if [[ ! " ${allowed[*]} " == *" $abi "* ]]; then
    echo "FAIL $name: ABI not allowed (allowed: ${allowed[*]})"
    status=1
  fi
  bad=""
  for align in $("$readelf" -lW "$so" | awk '$1=="LOAD" {print $NF}'); do
    [ $((align)) -lt 16384 ] && bad="$bad $align"
  done
  if [ -n "$bad" ]; then
    echo "FAIL $name: LOAD alignment $bad is below 16 KB"
    status=1
  else
    echo "OK   $name"
  fi
done
[ $found -eq 1 ] || { echo "FAIL: no native libraries in $apk"; status=1; }
exit $status
