#!/usr/bin/env bash
# Checks native libraries inside an APK:
#  - each allowed ABI contains the OPENOP Rust library (libopenop_ffi.so);
#  - no other ABI is present;
#  - every LOAD segment of every library is 16 KB-aligned.
# Fails closed: any tool error or missing data is a failure.
#
# Usage: scripts/check-native-libs.sh <apk> <allowed-abi> [allowed-abi ...]
# Needs ANDROID_NDK_HOME, ANDROID_HOME, or sdk.dir in local.properties.
set -euo pipefail

apk=$1; shift
allowed=("$@")
[ ${#allowed[@]} -gt 0 ] || { echo "usage: $0 <apk> <abi>..." >&2; exit 2; }
[ -f "$apk" ] || { echo "FAIL: APK not found: $apk"; exit 1; }

root="$(cd "$(dirname "$0")/.." && pwd)"
if [ -z "${ANDROID_HOME:-}" ] && [ -f "$root/local.properties" ]; then
  ANDROID_HOME=$(grep -E '^sdk.dir=' "$root/local.properties" | cut -d= -f2- || true)
fi
if [ -z "${ANDROID_NDK_HOME:-}" ]; then
  ndk_version=$(grep -E '^ndk *=' "$root/gradle/libs.versions.toml" | cut -d'"' -f2)
  ANDROID_NDK_HOME="${ANDROID_HOME:?set ANDROID_HOME or ANDROID_NDK_HOME}/ndk/$ndk_version"
fi
readelf=$(find "$ANDROID_NDK_HOME/toolchains/llvm/prebuilt" -name llvm-readelf 2>/dev/null | head -1)
[ -n "$readelf" ] && [ -x "$readelf" ] || { echo "llvm-readelf not found in $ANDROID_NDK_HOME" >&2; exit 2; }

work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
# unzip exits 11 when nothing matches 'lib/*'; that is reported below as "no libraries".
set +e
unzip -q "$apk" 'lib/*' -d "$work"
rc=$?
set -e
if [ $rc -ne 0 ] && [ $rc -ne 11 ]; then
  echo "FAIL: could not extract $apk (unzip exit $rc)"
  exit 1
fi

status=0

# 1. Each allowed ABI must contain the Rust library.
for abi in "${allowed[@]}"; do
  if [ ! -f "$work/lib/$abi/libopenop_ffi.so" ]; then
    echo "FAIL $abi/libopenop_ffi.so: missing"
    status=1
  fi
done

# 2. and 3. ABI allow-list and alignment for every library.
shopt -s nullglob
libs=("$work"/lib/*/*.so)
[ ${#libs[@]} -gt 0 ] || { echo "FAIL: no native libraries in $apk"; exit 1; }

for so in "${libs[@]}"; do
  abi=$(basename "$(dirname "$so")")
  name="$abi/$(basename "$so")"
  if [[ ! " ${allowed[*]} " == *" $abi "* ]]; then
    echo "FAIL $name: ABI not allowed (allowed: ${allowed[*]})"
    status=1
    continue
  fi
  if ! headers=$("$readelf" -lW "$so" 2>&1); then
    echo "FAIL $name: llvm-readelf failed: $headers"
    status=1
    continue
  fi
  aligns=$(awk '$1=="LOAD" {print $NF}' <<< "$headers")
  if [ -z "$aligns" ]; then
    echo "FAIL $name: no LOAD segments found"
    status=1
    continue
  fi
  bad=""
  for align in $aligns; do
    [ $((align)) -lt 16384 ] && bad="$bad $align"
  done
  if [ -n "$bad" ]; then
    echo "FAIL $name: LOAD alignment$bad is below 16 KB"
    status=1
  else
    echo "OK   $name"
  fi
done
exit $status
