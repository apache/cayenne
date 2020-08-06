/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/
package org.apache.cayenne.stubs;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;

import java.util.HashSet;
import java.util.Set;

public class CayenneProjectStub extends MavenProjectStub{

    public CayenneProjectStub()
    {
        Set<Artifact> artifacts = new HashSet<>();

        artifacts.add( new ArtifactStub( "assembly", "dependency-artifact1", "1.0", "jar", Artifact.SCOPE_COMPILE ) );
        artifacts.add( new ArtifactStub( "assembly", "dependency-artifact2", "1.0", "jar", Artifact.SCOPE_RUNTIME ) );
        artifacts.add( new ArtifactStub( "assembly", "dependency-artifact3", "1.0", "jar", Artifact.SCOPE_TEST ) );

        setDependencyArtifacts( artifacts );
    }
}
