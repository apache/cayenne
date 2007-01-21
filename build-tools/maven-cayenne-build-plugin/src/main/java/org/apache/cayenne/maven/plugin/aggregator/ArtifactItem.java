package org.apache.cayenne.maven.plugin.aggregator;

import java.io.File;

/**
 * ArtifactItem represents information specified in the plugin configuration section for
 * each artifact.
 */
public class ArtifactItem {

    /**
     * Group Id of Artifact
     * 
     * @parameter
     * @required
     */
    private String groupId;

    /**
     * Name of Artifact
     * 
     * @parameter
     * @required
     */
    private String artifactId;

    /**
     * Version of Artifact
     * 
     * @parameter
     */
    private String version = null;

    /**
     * Type of Artifact (War,Jar,etc)
     * 
     * @parameter
     * @required
     */
    private String type = "jar";

    /**
     * Classifier for Artifact (tests,sources,etc)
     * 
     * @parameter
     */
    private String classifier;

    /**
     * Location to use for this Artifact. Overrides default location.
     * 
     * @parameter
     */
    private File outputDirectory;

    /**
     * Provides ability to change destination file name
     * 
     * @parameter
     */
    private String destFileName;

    /**
     * @return Returns the artifactId.
     */
    public String getArtifactId() {
        return artifactId;
    }

    /**
     * @param artifactId The artifactId to set.
     */
    public void setArtifactId(String artifact) {
        this.artifactId = artifact;
    }

    /**
     * @return Returns the groupId.
     */
    public String getGroupId() {
        return groupId;
    }

    /**
     * @param groupId The groupId to set.
     */
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    /**
     * @return Returns the type.
     */
    public String getType() {
        return type;
    }

    /**
     * @param type The type to set.
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return Returns the version.
     */
    public String getVersion() {
        return version;
    }

    /**
     * @param version The version to set.
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * @return Classifier.
     */
    public String getClassifier() {
        return classifier;
    }

    /**
     * @param classifier Classifier.
     */
    public void setClassifier(String classifier) {
        this.classifier = classifier;
    }

    public String toString() {
        return groupId + ":" + artifactId + ":" + classifier + ":" + version + ":" + type;
    }

    /**
     * @return Returns the location.
     */
    public File getOutputDirectory() {
        return outputDirectory;
    }

    /**
     * @param location The location to set.
     */
    public void setOutputDirectory(File outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    /**
     * @return Returns the location.
     */
    public String getDestFileName() {
        return destFileName;
    }

    /**
     * @param destFileName The destFileName to set.
     */
    public void setDestFileName(String destFileName) {
        this.destFileName = destFileName;
    }

}
