/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/
package org.apache.cayenne.stubs;

import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.versioning.VersionRange;
import org.codehaus.plexus.PlexusTestCase;

import java.io.File;

public class ArtifactStub
        extends DefaultArtifact
{
    public ArtifactStub( String groupId, String artifactId, String version, String packaging, String scope )
    {
        this( groupId, artifactId, version, packaging, null, scope );
    }

    public ArtifactStub( String groupId, String artifactId, String version, String packaging, String classifier, String scope )
    {
        super( groupId, artifactId, VersionRange.createFromVersion( version ), scope, packaging,
                classifier, new DefaultArtifactHandler( packaging ), false );
    }

    public File getFile()
    {
        return new File( PlexusTestCase.getBasedir() + "/target/local-repo", getArtifactId() + "-" + getVersion() + "." + getType() )
        {
            public long lastModified()
            {
                return System.currentTimeMillis();
            }
        };
    }
}