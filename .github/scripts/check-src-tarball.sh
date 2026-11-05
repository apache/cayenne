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

# Checks that the source release has exactly the files tracked by git: nothing untracked got in (build output, IDE
# files), and nothing tracked is missing except for the deliberate exclusions. Run from the root of the git checkout
# the tarball was built from.
#
# Usage: check-src-tarball.sh <cayenne-X-src.tar.gz> <version>

set -euo pipefail

TARBALL="$1"
VERSION="$2"
TOP="cayenne-$VERSION-src"

# Added by the build, not in git
GENERATED='^(LICENSE\.txt|NOTICE\.txt)$'
# In git, deliberately left out of the source release: binaries (the Gradle wrapper) and git metadata
EXCLUDED='(^|/)gradle/|^\.gitignore$|^\.gitattributes$'

WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT

tar -tzf "$TARBALL" | grep -v '/$' | sort > "$WORK/tar-raw.txt"

if grep -v "^$TOP/" "$WORK/tar-raw.txt" > "$WORK/outside.txt"; then
    echo "::error::Files outside of the $TOP/ top directory:"
    cat "$WORK/outside.txt"
    exit 1
fi

sed "s#^$TOP/##" "$WORK/tar-raw.txt" | sort > "$WORK/tar.txt"
git ls-files | sort > "$WORK/git.txt"

FAILED=0

comm -23 "$WORK/tar.txt" "$WORK/git.txt" | grep -Ev "$GENERATED" > "$WORK/extra.txt" || true
if [ -s "$WORK/extra.txt" ]; then
    echo "::error::The source release has files not tracked by git:"
    cat "$WORK/extra.txt"
    FAILED=1
fi

comm -13 "$WORK/tar.txt" "$WORK/git.txt" | grep -Ev "$EXCLUDED" > "$WORK/missing.txt" || true
if [ -s "$WORK/missing.txt" ]; then
    echo "::error::Files tracked by git are missing from the source release:"
    cat "$WORK/missing.txt"
    FAILED=1
fi

for f in LICENSE.txt NOTICE.txt KEYS RELEASE-NOTES.txt UPGRADE.md pom.xml; do
    if ! grep -qx "$f" "$WORK/tar.txt"; then
        echo "::error::$f is missing from the source release"
        FAILED=1
    fi
done

if [ "$FAILED" -ne 0 ]; then
    exit 1
fi

echo "Source release matches git: $(wc -l < "$WORK/tar.txt" | tr -d ' ') files"
