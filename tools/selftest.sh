#!/usr/bin/env bash
# Runs the dev self-test client on a virtual display (no window on your desktop) and prints the
# PASS/FAIL lines. Screenshots land in run-selftest/screenshots/.
set -euo pipefail
cd "$(dirname "$0")/.."
export __GLX_VENDOR_LIBRARY_NAME=mesa LIBGL_ALWAYS_SOFTWARE=1
log=run-selftest/selftest.log
mkdir -p run-selftest
xvfb-run -a -s "-screen 0 1920x1080x24" ./gradlew runSelftest --console=plain > "$log" 2>&1 || true
grep -E "RELAY-SELFTEST" "$log" || { echo "no self-test output, see $log"; exit 1; }
! grep -q "RELAY-SELFTEST FAIL" "$log"
