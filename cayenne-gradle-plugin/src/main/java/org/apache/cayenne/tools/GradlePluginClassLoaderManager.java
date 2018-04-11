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

import org.apache.cayenne.di.ClassLoaderManager;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.DependencySet;

/**
 * Gradle class loader manager to update class loader urls with project dependencies.
 *
 * @since 4.1
 */
public class GradlePluginClassLoaderManager implements ClassLoaderManager {

    private Project project;
    private List<URL> urls = new ArrayList<>();

    public GradlePluginClassLoaderManager(final Project project) {
        this.project = project;
    }

    @Override
    public ClassLoader getClassLoader(final String resourceName) {
        return buildClassLoader();
    }

    private void addUrlFromDependency(final Dependency dependency, final Configuration configuration) {

        if(dependency == null) {
            return;
        }

        configuration.files(dependency).forEach(this::addUrlFromFile);
    }

    private void addUrlFromFile(final File file) {
        try {
            urls.add(file.toURI().toURL());
        } catch (Exception ignored) {
        }
    }

    private ClassLoader buildClassLoader() {
        ClassLoader classLoader = getClass().getClassLoader();
        ConfigurationContainer configurations = project.getConfigurations();

        if (configurations == null || configurations.isEmpty()) {
            return classLoader;
        }

        Configuration configuration = configurations.getByName("compile");
        DependencySet dependencies = configuration.getDependencies();
        if(dependencies == null || dependencies.isEmpty()) {
            return classLoader;
        }

        dependencies.forEach(dependency -> addUrlFromDependency(dependency, configuration));

        return new URLClassLoader(urls.toArray(new URL[0]), getClass().getClassLoader());
    }

}
