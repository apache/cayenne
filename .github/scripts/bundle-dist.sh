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

# Checks the distribution files of a release, writes a .sha512 next to each of them and prints a Markdown summary
# with the next steps for the release manager to stdout. Diagnostics go to stderr.
#
# Usage: bundle-dist.sh <dir>
# Env:   VERSION (required), TAG, COMMIT, RUN_URL, RUN_ID, REPOSITORY, MAC_SIGNED (optional, for the summary)

set -euo pipefail

DIR="$1"
: "${VERSION:?VERSION is required}"

SRC="cayenne-$VERSION-src.tar.gz"
GENERIC="cayenne-$VERSION.tar.gz"
MAC="cayenne-$VERSION-macosx.dmg"
WIN="cayenne-$VERSION-win.zip"
FILES=("$SRC" "$GENERIC" "$MAC" "$WIN")

cd "$DIR"

FAILED=0
fail() {
    echo "::error::$1" >&2
    FAILED=1
}

for f in "${FILES[@]}"; do
    [ -s "$f" ] || fail "$f is missing or empty"
done

for f in *; do
    case "$f" in
        "$SRC" | "$GENERIC" | "$MAC" | "$WIN") ;;
        *) fail "Unexpected file in the bundle: $f" ;;
    esac
done

[ "$FAILED" -eq 0 ] || exit 1

# Binary assemblies must carry the legal and release documents in their top directory
BIN_DOCS=(LICENSE.txt NOTICE.txt RELEASE-NOTES.txt UPGRADE.md KEYS)
check_listing() {
    local file="$1" listing="$2" top
    top=$(head -n1 <<< "$listing" | cut -d/ -f1)
    for doc in "${BIN_DOCS[@]}"; do
        grep -qx "$top/$doc" <<< "$listing" || fail "$file has no $top/$doc"
    done
}
check_listing "$GENERIC" "$(tar -tzf "$GENERIC")"
check_listing "$WIN" "$(unzip -Z1 "$WIN")"

[ "$FAILED" -eq 0 ] || exit 1

# "sha512sum" format, can be checked with "sha512sum -c" or "shasum -a 512 -c"
for f in "${FILES[@]}"; do
    sha512sum "$f" > "$f.sha512"
done
sha512sum -c ./*.sha512 >&2

cat <<MD
## Cayenne $VERSION distribution

Built from tag \`${TAG:-?}\`, commit \`${COMMIT:-?}\`.

| File | Size | SHA-512 |
|---|---:|---|
MD
for f in "${FILES[@]}"; do
    size=$(du -h "$f" | cut -f1)
    sum=$(cut -d' ' -f1 "$f.sha512")
    echo "| \`$f\` | $size | \`${sum:0:16}…\` |"
done

cat <<MD

### Next steps

\`\`\`bash
# 1. Download the bundle artifact "cayenne-$VERSION-dist" of ${RUN_URL:-this run}
gh run download ${RUN_ID:-<run-id>} --repo ${REPOSITORY:-apache/cayenne} --name cayenne-$VERSION-dist --dir cayenne-$VERSION-dist

# 2. Verify the checksums, sign, verify the signatures
build-tools/release/sign-and-stage.sh cayenne-$VERSION-dist $VERSION [your-gpg-key-id]
\`\`\`
MD