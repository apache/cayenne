package org.apache.cayenne.project;

import org.apache.cayenne.CayenneRuntimeException;

/**
 * Encapsulates compatible schema versions.
 */
public enum ProjectVersion {

    VERSION_9(null,"9"),
    VERSION_10("10","10"),
    VERSION_11("11","10");

    public final String domainSchemaVersion;
    public final String modelMapSchemaVersion;

    ProjectVersion(String domainSchemaVersion, String modelMapSchemaVersion) {
        this.domainSchemaVersion = domainSchemaVersion;
        this.modelMapSchemaVersion = modelMapSchemaVersion;
    }

    public ProjectVersion getProjectVersion(String version) {
        switch (version) {
            case "9":
                return VERSION_9;
            case "10":
                return VERSION_10;
            case "11":
                return VERSION_11;
            default:
                throw new CayenneRuntimeException("Invalid project version: " + version);
        }
    }
}
