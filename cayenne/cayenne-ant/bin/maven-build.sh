#!/bin/sh

JAVA_HOME=/opt/java-1.5
CAYENNE_BASE=/home/andrus/work/cayenne
CAYENNE_ANT=$CAYENNE_BASE/cayenne-ant

cd $CAYENNE_BASE
svn up

cd $CAYENNE_ANT
ant clean

cd $CAYENNE_ANT/maven
ant install-all -Dm2.repo=/var/sites/objectstyle/html/maven2 -Dproject.version=1.2-SNAPSHOT


