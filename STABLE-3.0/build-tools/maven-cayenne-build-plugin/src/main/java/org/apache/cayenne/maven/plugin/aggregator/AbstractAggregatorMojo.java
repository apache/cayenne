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
package org.apache.cayenne.maven.plugin.aggregator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.cayenne.maven.plugin.util.PatternGroup;
import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.util.FileUtils;

/**
 * A superclass of aggregator mojos.
 * 
 */
public abstract class AbstractAggregatorMojo extends AbstractMojo {

    // by default exclude maven entries from other jars and overlapping manifests.
    // note that excludes for the top package Java files and jj artifacts is done as a
    // hack to remove generated JJTree parser classes placed in the wrong location
    static final String[] DEFAULT_EXCLUDES = new String[] {
            "META-INF/maven/**", "META-INF/MANIFEST.MF", "**/.svn/**", "*.java", "*.jj"
    };

    /**
     * Used to look up Artifacts in the remote repository.
     * 
     * @parameter expression="${component.org.apache.maven.artifact.factory.ArtifactFactory}"
     * @required
     * @readonly
     */
    private ArtifactFactory factory;

    /**
     * Used to look up Artifacts in the remote repository.
     * 
     * @parameter expression="${component.org.apache.maven.artifact.resolver.ArtifactResolver}"
     * @required
     * @readonly
     */
    private ArtifactResolver resolver;

    /**
     * Location of the local repository.
     * 
     * @parameter expression="${localRepository}"
     * @readonly
     * @required
     */
    private ArtifactRepository local;

    /**
     * List of Remote Repositories used by the resolver
     * 
     * @parameter expression="${project.remoteArtifactRepositories}"
     * @readonly
     * @required
     */
    private List remoteRepos;

    /**
     * To look up Archiver/UnArchiver implementations
     * 
     * @parameter expression="${component.org.codehaus.plexus.archiver.manager.ArchiverManager}"
     * @required
     * @readonly
     */
    protected ArchiverManager archiverManager;

    /**
     * Collection of ArtifactItems to work on. (ArtifactItem contains groupId, artifactId,
     * version, type, location, destFile, markerFile and overwrite.) See "How To Use" and
     * "Javadoc" for details.
     * 
     * @parameter
     * @required
     */
    private ArrayList artifactItems;

    /**
     * POM
     * 
     * @parameter expression="${project}"
     * @readonly
     * @required
     */
    protected MavenProject project;

    /**
     * @component
     */
    private MavenProjectHelper projectHelper;

    /**
     * Name of the generated JAR.
     * 
     * @parameter expression="${project.build.finalName}"
     * @required
     */
    private String finalName;

    /**
     * A file that contains excludes patterns.
     * 
     * @parameter
     */
    private File excludesFile;

    /**
     * A file that contains includes patterns.
     * 
     * @parameter
     */
    private File includesFile;

    /**
     * Preprocesses the list of ArtifactItems and unpacks them to the provided directory.
     */
    protected void unpackArtifacts(File unpackDirectory, String classifier)
            throws MojoExecutionException {

        Iterator it = artifactItems.iterator();
        while (it.hasNext()) {
            ArtifactItem artifactItem = (ArtifactItem) it.next();
            artifactItem.setClassifier(classifier);

            getLog().debug("Configured artifact: " + artifactItem.toString());

            if (artifactItem.getOutputDirectory() == null) {
                artifactItem.setOutputDirectory(unpackDirectory);
            }

            artifactItem.getOutputDirectory().mkdirs();
            unpackArtifact(artifactItem);
        }
    }

    /**
     * Creates a filtered aggregated jar file from unpacked artifacts.
     */
    protected void packAggregatedArtifact(File unpackDirectory, String classifier)
            throws MojoExecutionException {

        if (classifier != null) {
            finalName += "-" + classifier;
        }

        File outputDirectory = new File(project.getBuild().getDirectory());
        File destinationFile = new File(outputDirectory, finalName + ".jar");

        JarArchiver jarArchiver;
        try {
            jarArchiver = (JarArchiver) archiverManager.getArchiver("jar");
        }
        catch (NoSuchArchiverException e) {
            throw new MojoExecutionException("Unknown archiver type", e);
        }

        PatternGroup excludes = new PatternGroup(excludesFile);
        excludes.addPatterns(DEFAULT_EXCLUDES);

        PatternGroup includes = new PatternGroup(includesFile);
        if (includes.size() == 0) {
            includes.addPatterns(new String[] {
                "**/**"
            });
        }

        // MavenArchiver adds Maven stuff into META-INF
        MavenArchiver archiver = new MavenArchiver();
        archiver.setArchiver(jarArchiver);
        archiver.setOutputFile(destinationFile);

        MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

        try {
            if (!unpackDirectory.exists()) {
                getLog().warn("Jar will be empty, no unpack directory.");
            }
            else {
                archiver.getArchiver().addDirectory(
                        unpackDirectory,
                        includes.getPatterns(),
                        excludes.getPatterns());
            }

            archiver.createArchive(project, archive);
        }
        catch (Exception e) {
            throw new MojoExecutionException("Error assembling JAR", e);
        }

        if (classifier == null) {
            project.getArtifact().setFile(destinationFile);
        }
        else {
            projectHelper.attachArtifact(project, "jar", classifier, destinationFile);
        }
    }

    /**
     * Resolves the Artifact from the remote repository if nessessary. If no version is
     * specified, it will be retrieved from the DependencyManagement section of the pom.
     */
    private Artifact getArtifact(ArtifactItem artifactItem) throws MojoExecutionException {
        Artifact artifact;

        if (artifactItem.getVersion() == null) {
            fillArtifactVersionFromDependencyManagement(artifactItem);

            if (artifactItem.getVersion() == null) {
                throw new MojoExecutionException("Unable to find artifact version of "
                        + artifactItem.getGroupId()
                        + ":"
                        + artifactItem.getArtifactId()
                        + " in project's dependency management.");
            }

        }

        // use classifer if set.
        String classifier = artifactItem.getClassifier();

        if (classifier == null || classifier.equals("")) {
            artifact = factory.createArtifact(
                    artifactItem.getGroupId(),
                    artifactItem.getArtifactId(),
                    artifactItem.getVersion(),
                    Artifact.SCOPE_PROVIDED,
                    artifactItem.getType());
        }
        else {
            artifact = factory.createArtifactWithClassifier(
                    artifactItem.getGroupId(),
                    artifactItem.getArtifactId(),
                    artifactItem.getVersion(),
                    artifactItem.getType(),
                    artifactItem.getClassifier());
        }

        try {
            resolver.resolve(artifact, remoteRepos, local);
        }
        catch (ArtifactResolutionException e) {
            throw new MojoExecutionException("Unable to resolve artifact.", e);
        }
        catch (ArtifactNotFoundException e) {
            throw new MojoExecutionException("Unable to find artifact.", e);
        }

        return artifact;
    }

    /**
     * Tries to find missing version from dependancy management. If found, the artifact is
     * updated with the correct version.
     * 
     * @param artifact representing configured file.
     */
    private void fillArtifactVersionFromDependencyManagement(ArtifactItem artifact) {
        this.getLog().debug(
                "Attempting to find missing version from dependency management.");

        List list = this.project.getDependencyManagement().getDependencies();

        for (int i = 0; i < list.size(); ++i) {
            Dependency dependency = (Dependency) list.get(i);

            if (dependency.getGroupId().equals(artifact.getGroupId())
                    && dependency.getArtifactId().equals(artifact.getArtifactId())
                    && dependency.getType().equals(artifact.getType())) {
                this.getLog().debug("Found missing version: " + dependency.getVersion());

                artifact.setVersion(dependency.getVersion());
            }
        }
    }

    /**
     * Unpacks an artifact item.
     */
    private void unpackArtifact(ArtifactItem artifactItem) throws MojoExecutionException {
        Artifact artifact = getArtifact(artifactItem);

        File location = artifactItem.getOutputDirectory();
        File file = artifact.getFile();

        String archiveExt = FileUtils.getExtension(file.getAbsolutePath()).toLowerCase();

        try {
            UnArchiver unArchiver = archiverManager.getUnArchiver(archiveExt);
            unArchiver.setSourceFile(file);
            unArchiver.setDestDirectory(location);
            unArchiver.extract();
        }
        catch (NoSuchArchiverException e) {
            throw new MojoExecutionException("Unknown archiver type", e);
        }
        catch (IOException e) {
            throw new MojoExecutionException("Error unpacking file: "
                    + file
                    + "to: "
                    + location, e);
        }
        catch (ArchiverException e) {
            throw new MojoExecutionException("Error unpacking file: "
                    + file
                    + "to: "
                    + location, e);
        }
    }
}
