#!/usr/bin/env bash

function get_pom_version {
  awk -F '<[^>]*>' '/<dependencies>/,/<\/dependencies>/{next} /<plugins>/,/<\/plugins>/{next} /<version>/ {$1=$1; gsub(/ /,"") $0; print}' pom.xml
}

VERSION=$(get_pom_version)
echo "pom.xml version: $VERSION"

# export VERSION to the GitHub env
echo "POM_VERSION=$VERSION" >> "$GITHUB_ENV"