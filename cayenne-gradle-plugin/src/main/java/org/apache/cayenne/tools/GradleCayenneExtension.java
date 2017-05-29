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

package org.apache.cayenne.tools;

import java.io.IOException;
import java.net.URL;

import org.codehaus.groovy.runtime.ResourceGroovyMethods;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.dsl.DependencyHandler;

/**
 * Cayenne DSL for gradle
 * <p>
 * - dependency management
 * - utility methods
 *
 * @since 4.0
 */
public class GradleCayenneExtension {

    public static final String GROUP = "org.apache.cayenne";
    private static final String VERSION_FILE = "/cayenne.version";

    private String version;
    private final DependencyHandler dependencies;

    /**
     * Shortcut for the cgen task.
     * Can be used in defining additional tasks like:
     * <pre>{@code
     * task customCgen(type: cayenne.cgen) {
     *     //...
     * }
     * }</pre>
     */
    private final Class<CgenTask> cgen = CgenTask.class;

    /**
     * Shortcut for the cdbimport task.
     * Can be used in defining additional tasks like:
     * <pre>{@code
     * task customCdbimport(type: cayenne.cdbimport) {
     *     //...
     * }
     * }</pre>
     */
    private final Class<DbImportTask> cdbimport = DbImportTask.class;

    /**
     * Shortcut for the cdbgen task.
     * Can be used in defining additional tasks like:
     * <pre>{@code
     * task customCdbgen(type: cayenne.cdbgen) {
     *     //...
     * }
     * }</pre>
     */
    private final Class<DbGenerateTask> cdbgen = DbGenerateTask.class;

    /**
     * Default data map that will be used in all tasks.
     * Can be overridden per task.
     */
    private String defaultDataMap;

    public GradleCayenneExtension(Project project) {
        this.dependencies = project.getDependencies();
        try {
            readVersion(project);
        } catch (IOException ex) {
            throw new GradleException("Cayenne version not found", ex);
        }
    }

    private void readVersion(Project project) throws IOException {
        URL versionFileUrl = getClass().getResource(VERSION_FILE);
        if(versionFileUrl == null) {
            this.version = project.getVersion().toString();
        } else {
            this.version = ResourceGroovyMethods.getText(versionFileUrl).trim();
        }
    }

    public Dependency dependency(final String name) {
        return dependencies.create(GROUP + ":cayenne-" + name + ":" + version);
    }

    public String getDefaultDataMap() {
        return defaultDataMap;
    }

    public void setDefaultDataMap(String defaultDataMap) {
        this.defaultDataMap = defaultDataMap;
    }

    public Class<CgenTask> getCgen() {
        return cgen;
    }

    public Class<DbImportTask> getCdbimport() {
        return cdbimport;
    }

    public Class<DbGenerateTask> getCdbgen() {
        return cdbgen;
    }

    public String getVersion() {
        return version;
    }
}