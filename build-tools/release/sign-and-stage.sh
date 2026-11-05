#!/usr/bin/env bash
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements. See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License. You may obtain a copy of the License at
#
# https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# Signs the release bundle built by the "release dist" GitHub workflow with the release manager's own GPG key, as
# the ASF release policy requires. Run on the release manager's machine, never in CI.
#
#   1. checks the .sha512 files made by the workflow
#   2. writes an ASCII-armored detached signature (.asc) for each distribution file and verifies it
#   3. checks that the signing key is in the KEYS file
#   4. prints (does not run) the svn commands to stage the files in dist/dev for the vote
#
# Usage: sign-and-stage.sh <bundle-dir> <version> [gpg-key-id]

set -euo pipefail

if [ $# -lt 2 ] || [ $# -gt 3 ]; then
    echo "Usage: $0 <bundle-dir> <version> [gpg-key-id]" >&2
    exit 2
fi

BUNDLE=$(cd "$1" && pwd)
VERSION="$2"
KEY="${3:-}"
REPO_ROOT=$(cd "$(dirname "$0")/../.." && pwd)
KEYS_FILE="$REPO_ROOT/KEYS"
DIST_DEV="https://dist.apache.org/repos/dist/dev/cayenne"

FILES=(
    "cayenne-$VERSION-src.tar.gz"
    "cayenne-$VERSION.tar.gz"
    "cayenne-$VERSION-macosx.dmg"
    "cayenne-$VERSION-win.zip"
)

if command -v sha512sum > /dev/null; then
    SHA512=(sha512sum)
else
    SHA512=(shasum -a 512)
fi

GPG_KEY_OPTS=()
if [ -n "$KEY" ]; then
    GPG_KEY_OPTS=(--local-user "$KEY")
fi

cd "$BUNDLE"

echo "== Checking checksums"
for f in "${FILES[@]}"; do
    if [ ! -s "$f" ] || [ ! -s "$f.sha512" ]; then
        echo "ERROR: $f or $f.sha512 is missing in $BUNDLE" >&2
        exit 1
    fi
    "${SHA512[@]}" -c "$f.sha512"
done

echo "== Signing"
for f in "${FILES[@]}"; do
    rm -f "$f.asc"
    # the odd expansion keeps an empty array working with "set -u" in the macOS bash 3.2
    gpg --armor --detach-sign ${GPG_KEY_OPTS[@]+"${GPG_KEY_OPTS[@]}"} --output "$f.asc" "$f"
done

echo "== Verifying signatures"
FINGERPRINT=""
for f in "${FILES[@]}"; do
    STATUS=$(gpg --status-fd 1 --verify "$f.asc" "$f" 2> /dev/null || true)
    if ! grep -q '^\[GNUPG:\] GOODSIG' <<< "$STATUS"; then
        echo "ERROR: bad signature for $f" >&2
        exit 1
    fi
    FINGERPRINT=$(awk '$2 == "VALIDSIG" {print $NF; exit}' <<< "$STATUS")
    echo "$f: good signature"
done

echo "== Checking KEYS"
if [ -z "$FINGERPRINT" ]; then
    echo "WARNING: could not read the signing key fingerprint, check KEYS manually"
elif [ ! -f "$KEYS_FILE" ]; then
    echo "WARNING: $KEYS_FILE not found, check KEYS manually"
elif gpg --with-colons --import-options show-only --import "$KEYS_FILE" 2> /dev/null \
        | awk -F: '$1 == "fpr" {print $10}' | grep -qx "$FINGERPRINT"; then
    echo "Key $FINGERPRINT is in KEYS"
else
    echo "WARNING: key $FINGERPRINT is NOT in $KEYS_FILE. Add it and publish KEYS before the vote:"
    echo "  (gpg --list-sigs $FINGERPRINT && gpg --armor --export $FINGERPRINT) >> KEYS"
fi

cat <<EOT

== Done. To stage the release candidate in dist/dev for the vote, run:

svn checkout --depth=empty $DIST_DEV cayenne-dist-dev
cd cayenne-dist-dev
svn mkdir $VERSION
cp "$BUNDLE"/cayenne-$VERSION* $VERSION/
svn add $VERSION/*
svn commit -m "Cayenne $VERSION release candidate"

Then check $DIST_DEV/$VERSION/ in a browser.
EOT
