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
package org.apache.cayenne.configuration.xml;

import java.text.DecimalFormat;
import java.util.Set;

/**
 * Represents Cayenne project XML version.
 * This version is used across all project XML files including domain, data maps, and extensions.
 *
 * @since 5.0
 */
public final class ProjectVersion implements Comparable<ProjectVersion> {

    public static final ProjectVersion V6 = new ProjectVersion(6);
    public static final ProjectVersion V7 = new ProjectVersion(7);
    public static final ProjectVersion V8 = new ProjectVersion(8);
    public static final ProjectVersion V9 = new ProjectVersion(9);
    public static final ProjectVersion V10 = new ProjectVersion(10);
    public static final ProjectVersion V11 = new ProjectVersion(11);

    public static final Set<ProjectVersion> KNOWN_VERSIONS = Set.of(V6, V7, V8, V9, V10, V11);

    private static final DecimalFormat VERSION_FORMAT = new DecimalFormat("#.#################");

    private final double version;

    private ProjectVersion(double version) {
        this.version = version;
    }

    /**
     * Returns the version as a double.
     *
     * @return version number
     */
    public double getAsDouble() {
        return version;
    }

    /**
     * Returns the version as a string.
     *
     * @return version string
     */
    public String getAsString() {
        return VERSION_FORMAT.format(version);
    }

    @Override
    public String toString() {
        return getAsString();
    }

    @Override
    public int compareTo(ProjectVersion other) {
        return Double.compare(this.version, other.version);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ProjectVersion that = (ProjectVersion) obj;
        return Double.compare(that.version, version) == 0;
    }

    @Override
    public int hashCode() {
        return Double.hashCode(version);
    }

    /**
     * Returns the current project version.
     *
     * @return current project version
     */
    public static ProjectVersion getCurrent() {
        return V11;
    }

    /**
     * Parses a version string and returns the corresponding value-object.
     *
     * @param versionString version string to parse
     * @return corresponding ProjectVersion
     */
    public static ProjectVersion fromString(String versionString) {
        double version = decodeVersion(versionString);
        for (ProjectVersion knownVersion : KNOWN_VERSIONS) {
            if (knownVersion.version == version) {
                return knownVersion;
            }
        }
        return new ProjectVersion(version);
    }

    private static double decodeVersion(String version) {
        if (version == null || version.isBlank()) {
            return 0;
        }

        // leave the first dot, and treat remaining as a fraction
        // remove all non digit chars
        StringBuilder buffer = new StringBuilder(version.length());
        boolean dotProcessed = false;
        for (int i = 0; i < version.length(); i++) {
            char nextChar = version.charAt(i);
            if (nextChar == '.' && !dotProcessed) {
                dotProcessed = true;
                buffer.append('.');
            } else if (Character.isDigit(nextChar)) {
                buffer.append(nextChar);
            }
        }
        return Double.parseDouble(buffer.toString());
    }
}
