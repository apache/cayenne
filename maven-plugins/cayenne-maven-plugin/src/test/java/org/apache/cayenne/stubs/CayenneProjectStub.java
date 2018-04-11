package org.apache.cayenne.stubs;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;

import java.util.HashSet;
import java.util.Set;

public class CayenneProjectStub extends MavenProjectStub{

    public CayenneProjectStub()
    {
        Set artifacts = new HashSet();

        artifacts.add( new ArtifactStub( "assembly", "dependency-artifact1", "1.0", "jar", Artifact.SCOPE_COMPILE ) );
        artifacts.add( new ArtifactStub( "assembly", "dependency-artifact2", "1.0", "jar", Artifact.SCOPE_RUNTIME ) );
        artifacts.add( new ArtifactStub( "assembly", "dependency-artifact3", "1.0", "jar", Artifact.SCOPE_TEST ) );

        setDependencyArtifacts( artifacts );
    }
}
