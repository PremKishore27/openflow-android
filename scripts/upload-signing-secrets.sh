#!/usr/bin/env bash
# One-time setup, run by the repository owner: stores the release signing key as encrypted GitHub secrets,
# so every release is signed with the same key and installs over the previous one.
# Needs the GitHub CLI (gh), logged in with access to the repository.
set -euo pipefail

DIR="${1:-$HOME/openflow-signing}"
REPO="PremKishore27/openflow-android"
JKS="$DIR/openflow-release.jks"
INFO="$DIR/KEEP-SAFE-passwords.txt"

[[ -f "$JKS" && -f "$INFO" ]] || { echo "Can't find the key files in $DIR" >&2; exit 1; }
PASS="$(grep '^store password:' "$INFO" | cut -d' ' -f3)"

base64 < "$JKS" | tr -d '\n' | gh secret set OPENFLOW_KEYSTORE_B64 -R "$REPO"
printf '%s' "$PASS"    | gh secret set OPENFLOW_KEYSTORE_PASSWORD -R "$REPO"
printf '%s' "openflow" | gh secret set OPENFLOW_KEY_ALIAS -R "$REPO"
printf '%s' "$PASS"    | gh secret set OPENFLOW_KEY_PASSWORD -R "$REPO"
echo "Done. Releases built from now on are signed with your key."
