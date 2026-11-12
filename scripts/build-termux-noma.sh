#!/data/data/com.termux/files/usr/bin/bash
# Noma Reborn: free on-device build path for Android ARM64 + Termux.
# Does not modify GitHub billing, main, or application data.
set -Eeuo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

EXPECTED="feature/noma-replica-notes-rebirth"
ACTUAL="$(git branch --show-current)"
if [[ "$ACTUAL" != "$EXPECTED" ]]; then
  printf 'ERROR: branch is %s. Use %s before compiling.\n' "$ACTUAL" "$EXPECTED" >&2
  exit 2
fi

export ANDROID_HOME="${ANDROID_HOME:-$HOME/android-sdk}"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
if [[ ! -f "$ANDROID_HOME/platforms/android-36/android.jar" ]]; then
  echo 'ERROR: Android SDK platform 36 missing (install with sdkmanager).' >&2
  exit 2
fi

if [[ -z "${JAVA_HOME:-}" && -x "${PREFIX:-/data/data/com.termux/files/usr}/lib/jvm/java-21-openjdk/bin/java" ]]; then
  export JAVA_HOME="$PREFIX/lib/jvm/java-21-openjdk"
fi

AAPT2="$(command -v aapt2 || true)"
if [[ -z "$AAPT2" ]]; then
  echo 'ERROR: Native ARM64 aapt2 missing. Install with pkg install aapt2.' >&2
  exit 2
fi

printf 'sdk.dir=%s\n' "$ANDROID_HOME" > local.properties

GRADLE_HOME="$HOME/tools/gradle-8.13"
if [[ ! -x "$GRADLE_HOME/bin/gradle" ]]; then
  command -v curl >/dev/null || { echo 'Install curl first'; exit 2; }
  command -v unzip >/dev/null || { echo 'Install unzip first'; exit 2; }
  mkdir -p "$HOME/tools"
  ZIP="$HOME/tools/gradle-8.13-bin.zip"
  curl --fail --location --retry 3 \
    'https://services.gradle.org/distributions/gradle-8.13-bin.zip' -o "$ZIP"
  unzip -oq "$ZIP" -d "$HOME/tools"
fi

TASKS=(:app:testDebugUnitTest :app:assembleDebug)
if [[ "${1:-}" == '--full' ]]; then
  TASKS=(:app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:lintDebug :app:assembleDebug)
fi

LOG="$HOME/noma-reborn-build.log"
printf '\nNoma branch: %s\nSDK: %s\nAAPT2: %s\n' "$ACTUAL" "$ANDROID_HOME" "$AAPT2"
echo "Running Gradle 8.13; see $LOG for diagnostics."
set +e
"$GRADLE_HOME/bin/gradle" --no-daemon --console=plain --max-workers=2 \
  -Dorg.gradle.jvmargs='-Xmx1536m -Dfile.encoding=UTF-8' \
  "-Pandroid.aapt2FromMavenOverride=$AAPT2" \
  "${TASKS[@]}" 2>&1 | tee "$LOG"
RESULT=${PIPESTATUS[0]}
set -e

if [[ "$RESULT" -ne 0 ]]; then
  echo 'FAIL: Gradle did not finish. Error excerpts:'
  grep -nE 'FAILURE:|error:|What went wrong|Could not|Execution failed|BUILD FAILED' "$LOG" | tail -n 40 || true
  exit "$RESULT"
fi
OUT='app/build/outputs/apk/debug/app-debug.apk'
if [[ ! -f "$OUT" ]]; then
  echo 'ERROR: Gradle succeeded but the APK is missing.' >&2
  exit 3
fi
printf '\nPASS: %s\n' "$OUT"
ls -lh "$OUT"
sha256sum "$OUT"
echo 'Preview uses a separate application ID and does not replace the release app.'
