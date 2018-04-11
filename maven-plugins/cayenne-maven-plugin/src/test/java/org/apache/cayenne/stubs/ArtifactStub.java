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