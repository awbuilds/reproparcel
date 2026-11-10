#!/usr/bin/env bash
# SPDX-License-Identifier: AGPL-3.0-only
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
  # Include tracked files and untracked non-ignored files so a pre-commit
  # review covers the whole public tree, not only the last staged set.
  { git ls-files -z; git ls-files -z -o --exclude-standard; } > "$TMP_LIST"
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
  '/private/var/folders/'
  'C:\\Users\\'
  '-----BEGIN [A-Z ]*PRIVATE KEY-----'
  'ghp_[A-Za-z0-9]{20,}'
  'github_pat_[A-Za-z0-9_]{20,}'
  'AKIA[0-9A-Z]{16}'
  'AIza[0-9A-Za-z_-]{30,}'
  'xox[baprs]-[A-Za-z0-9-]{10,}'
  'sk-[A-Za-z0-9]{20,}'
  '[A-Za-z0-9._%+-]+@(gmail|outlook|yahoo|hotmail|icloud)\.[A-Za-z]{2,}'
)

# Binary files are not inspected by grep. Only reviewed, explicitly allowed
# paths may be committed. Add one glob per line to this optional file.
ALLOWED_BINARY="scripts/allowed-binary.txt"

is_allowed_binary() {
  local file="$1"
  [[ -f "$ALLOWED_BINARY" ]] || return 1
  while IFS= read -r pattern; do
    [[ -z "$pattern" || "$pattern" == \#* ]] && continue
    # shellcheck disable=SC2254
    case "$file" in
      $pattern) return 0 ;;
    esac
  done < "$ALLOWED_BINARY"
  return 1
}

scan_file() {
  local file="$1"
  [[ -f "$file" ]] || return 0
  # The scanner necessarily contains the patterns it searches for.
  [[ "$file" == "scripts/public-hygiene.sh" ]] && return 0
  # Skip binary files, but require an explicit allowlist entry for them.
  if ! grep -Iq . "$file" 2>/dev/null; then
    if is_allowed_binary "$file"; then
      return 0
    fi
    echo "PUBLIC-HYGIENE: unreviewed binary file $file (add to $ALLOWED_BINARY)" >&2
    FAIL=1
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

# Private identifiers in file contents and file names.
DENYLIST=".git/info/private-denylist.txt"
if [[ -f "$DENYLIST" ]]; then
  while IFS= read -r term; do
    [[ -z "$term" || "$term" == \#* ]] && continue
    while IFS= read -r -d '' file; do
      [[ -f "$file" ]] || continue
      if [[ "$(basename "$file")" == *"$term"* ]]; then
        echo "PUBLIC-HYGIENE: private term in file name $file" >&2
        FAIL=1
      fi
      if grep -Iq . "$file" 2>/dev/null && grep -niF "$term" "$file" >/dev/null 2>&1; then
        echo "PUBLIC-HYGIENE: private term in $file" >&2
        FAIL=1
      fi
    done < "$TMP_LIST"
  done < "$DENYLIST"
fi

# Private identifiers must not survive in history either.
if git rev-parse --is-inside-work-tree >/dev/null 2>&1 && [[ -f "$DENYLIST" ]]; then
  while IFS= read -r term; do
    [[ -z "$term" || "$term" == \#* ]] && continue
    if git log -p --all -- . 2>/dev/null | grep -iF "$term" >/dev/null 2>&1; then
      echo "PUBLIC-HYGIENE: private term found in Git history" >&2
      FAIL=1
    fi
  done < "$DENYLIST"
fi

# Commit identity must be project-neutral.
if git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  while IFS='|' read -r an ae cn ce; do
    for identity in "$an" "$ae" "$cn" "$ce"; do
      [[ -z "$identity" ]] && continue
      if [[ "$identity" =~ @(gmail|outlook|yahoo|hotmail|icloud)\. ]]; then
        echo "PUBLIC-HYGIENE: personal email in commit identity" >&2
        FAIL=1
      fi
    done
  done < <(git log --format='%an|%ae|%cn|%ce' 2>/dev/null || true)
fi

if [[ "$FAIL" -ne 0 ]]; then
  echo "public-hygiene: FAIL" >&2
  exit 1
fi
echo "public-hygiene: PASS ($MODE)"
