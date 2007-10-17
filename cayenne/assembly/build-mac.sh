#!/bin/bash

#
# Builds Mac OS X DMG assembly 
#

#	Licensed to the Apache Software Foundation (ASF) under one
#	or more contributor license agreements.  See the NOTICE file
#	distributed with this work for additional information
#	regarding copyright ownership.  The ASF licenses this file
#	to you under the Apache License, Version 2.0 (the
#	"License"); you may not use this file except in compliance
#	with the License.  You may obtain a copy of the License at
#
#		http://www.apache.org/licenses/LICENSE-2.0
#
#	Unless required by applicable law or agreed to in writing,
#	software distributed under the License is distributed on an
#	"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
#	KIND, either express or implied.  See the License for the
#	specific language governing permissions and limitations
#	under the License.   


OUT_DIR=`pwd`/target/mac-assembly
TARGET_DIR=`pwd`/target

mvn -o -P mac clean package

# Repackage stuff assembled with Maven in a .dmg that makes sense

ARTIFACT=`ls target/*.tar.gz |perl -n -e 'print $1 if /([^\/]+)\.tar\.gz$/;'`
echo "Cayenne artifact: $ARTIFACT"
mkdir -p $OUT_DIR
(cd $OUT_DIR && tar xzf $TARGET_DIR/$ARTIFACT.tar.gz)
find $TARGET_DIR/modeler-mac -name JavaApplicationStub |xargs chmod 755
mv $TARGET_DIR/modeler-mac/*.app $TARGET_DIR/modeler-mac/*.txt $OUT_DIR/

/usr/bin/hdiutil create -srcfolder $OUT_DIR -format UDZO -volname $ARTIFACT $TARGET_DIR/$ARTIFACT-macosx.dmg
