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

#
# Fallback Rat check, for when a standalone Rat jar is preferred over Maven. The primary check is
# "mvn apache-rat:check". Runs Rat checks on the source code, prints report to STDOUT.
#
# Usage:
#    
#  ./rat.sh /path/to/apache-rat.jar > report.txt
#


DIR=`dirname "$0"`

RAT="$@"
if [[ -z $RAT ]]
then
    echo "*** No rat jar specified" 1>&2
    exit 1
fi

if [[ ! -f $RAT ]] 
then
     echo "*** $RAT is not a file" 1>&2
     exit 1
fi

echo "Deleting 'target' dirs..." 1>&2
( find $DIR -type d -name target | xargs rm -rf )

echo "Deleting 'build' dirs..." 1>&2
( find $DIR -type d -name build | xargs rm -rf )

echo "Running rat, this may take a while..." 1>&2

# Keep this in sync with 'build-tools/rat-excludes'.
java -jar $RAT -d $DIR \
	-e '\..+' \
	-e '\_.+\.java' \
	-e '.+\.md' \
	-e 'cayenne-.+\.xml' \
	-e 'cayenne\.xml' \
	-e '.+\.map\.xml' \
	-e '.+\.driver\.xml' \
	-e '.+\.plist' \
	-e '.+\.eomodeld' \
	-e '.+\.fspec' \
	-e 'DiagramLayout' \
	-e 'org\.slf4j\.spi\.SLF4JServiceProvider' \
	-e 'NOTICE\.txt\.vm' \
	-e 'plain' \
	-e 'KEYS' \
	-e 'RELEASE-NOTES\.txt' \
	-e 'README\.txt' \
	-e 'report.txt'
