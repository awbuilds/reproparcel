#!/usr/bin/env bash
# Public repository hygiene gate.
#
# Scans staged files by default, or all tracked files with --all.
# Private identifiers must not be committed. Put them in the untracked file
# .git/info/private-denylist.txt, one pattern per line. That file is never
# committed and is read only when present.

set -euo pipefail

MODE="${1:---staged}"
FAIL=0
TMP_LIST="$(mktemp)"
trap 'rm -f "$TMP_LIST"' EXIT

if [[ "$MODE" == "--all" ]]; then
  git ls-files -z > "$TMP_LIST"
elif [[ "$MODE" == "--staged" ]]; then
  git diff --cached --name-only --diff-filter=ACMR -z > "$TMP_LIST"
else
  echo "usage: $0 [--staged|--all]" >&2
  exit 2
fi

# Generic personal/secret/path patterns. These must stay project-neutral.
GENERIC_PATTERNS=(
  '/Users/'
  '/home/[^/]+/'
  'C:\\Users\\'
  '-----BEGIN [A-Z ]*PRIVATE KEY-----'
  'ghp_[A-Za-z0-9]{20,}'
  'github_pat_[A-Za-z0-9_]{20,}'
  'AKIA[0-9A-Z]{16}'
  'AIza[0-9A-Za-z_-]{30,}'
  'xox[baprs]-[A-Za-z0-9-]{10,}'
  'sk-[A-Za-z0-9]{20,}'
)

scan_file() {
  local file="$1"
  [[ -f "$file" ]] || return 0
  # The scanner necessarily contains the patterns it searches for.
  [[ "$file" == "scripts/public-hygiene.sh" ]] && return 0
  # Skip binary files.
  if ! grep -Iq . "$file" 2>/dev/null; then
    return 0
  fi
  for pattern in "${GENERIC_PATTERNS[@]}"; do
    if grep -nEI "$pattern" "$file" >/dev/null 2>&1; then
      echo "PUBLIC-HYGIENE: generic pattern '$pattern' in $file" >&2
      FAIL=1
    fi
  done
}

while IFS= read -r -d '' file; do
  scan_file "$file"
done < "$TMP_LIST"

DENYLIST=".git/info/private-denylist.txt"
if [[ -f "$DENYLIST" ]]; then
  while IFS= read -r term; do
    [[ -z "$term" || "$term" == \#* ]] && continue
    while IFS= read -r -d '' file; do
      [[ -f "$file" ]] || continue
      if grep -Iq . "$file" 2>/dev/null && grep -niF "$term" "$file" >/dev/null 2>&1; then
        echo "PUBLIC-HYGIENE: private term in $file" >&2
        FAIL=1
      fi
    done < "$TMP_LIST"
  done < "$DENYLIST"
fi

if [[ "$FAIL" -ne 0 ]]; then
  echo "public-hygiene: FAIL" >&2
  exit 1
fi
echo "public-hygiene: PASS ($MODE)"
