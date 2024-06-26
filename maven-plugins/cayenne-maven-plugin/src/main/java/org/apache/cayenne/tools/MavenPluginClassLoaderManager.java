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

package org.apache.cayenne.tools;

import org.apache.cayenne.di.ClassLoaderManager;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Maven class loader manager to update class loader urls with project dependencies.
 *
 * @since 4.1
 */
public class MavenPluginClassLoaderManager implements ClassLoaderManager {

    private MavenProject project;
    private List<URL> urls = new ArrayList<>();

    public MavenPluginClassLoaderManager(final MavenProject project) {
        this.project = project;
    }

    @Override
    public ClassLoader getClassLoader(final String resourceName) {
        return buildClassLoader();
    }

    private void addUrlFromArtifact(final Artifact artifact) {

        if (artifact == null) {
            return;
        }

        addUrlFromFile(artifact.getFile());
    }

    private void addUrlFromFile(final File file) {
        try {
            urls.add(file.toURI().toURL());
        } catch (Exception ignored) {
        }
    }

    private ClassLoader buildClassLoader() {
        @SuppressWarnings("deprecation")
        Set<Artifact> artifacts = project.getDependencyArtifacts();
        if (artifacts != null) {
            for (final Artifact artifact : artifacts) {
                addUrlFromArtifact(artifact);
            }
        }

        addUrlFromArtifact(project.getArtifact());

        return new URLClassLoader(urls.toArray(new URL[0]), getClass().getClassLoader());
    }

}
