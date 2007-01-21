package org.apache.cayenne.maven.plugin.aggregator;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * A goal to build aggregated jar artifacts from multiple other artifacts.
 * 
 * @author Andrus Adamchik
 * @goal aggregate-bin
 * @phase package
 * @requiresProject
 */
public class BinAggregatorMojo extends AbstractAggregatorMojo {

    /**
     * Default location used for mojo unless overridden in ArtifactItem
     * 
     * @parameter expression="${unpackDirectory}"
     *            default-value="${project.build.directory}/aggregate/unpack-bin"
     * @required
     */
    private File unpackDirectory;

    public void execute() throws MojoExecutionException {
        unpackArtifacts(unpackDirectory, null);
        packAggregatedArtifact(unpackDirectory, null);
    }
}