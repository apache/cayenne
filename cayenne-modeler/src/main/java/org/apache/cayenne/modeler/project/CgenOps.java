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

package org.apache.cayenne.modeler.project;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.gen.CgenConfigList;
import org.apache.cayenne.gen.CgenConfiguration;
import org.apache.cayenne.gen.internal.Utils;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.pref.adapters.GeneralPrefs;
import org.apache.cayenne.project.Project;
import org.apache.cayenne.resource.Resource;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.prefs.Preferences;
import java.util.Optional;

public class CgenOps {

    static final int MAX_NAME_ATTEMPTS = 1000;

    /**
     * Returns a name for a new configuration that doesn't clash with the existing ones, following the
     * same "Default", "Default1", "Default2" pattern used for newly created ObjEntities. Gives up with
     * an exception after {@link #MAX_NAME_ATTEMPTS} suffixes.
     */
    public static String createUniqueConfigName(CgenConfigList configurations) {
        String name = CgenConfigList.DEFAULT_CONFIG_NAME;
        for (int i = 1; configurations.isExist(name); i++) {
            if (i > MAX_NAME_ATTEMPTS) {
                throw new CayenneRuntimeException("Can't create a unique cgen configuration name after %d attempts",
                        MAX_NAME_ATTEMPTS);
            }
            name = CgenConfigList.DEFAULT_CONFIG_NAME + i;
        }
        return name;
    }

    public static CgenConfiguration createDefaultCgenConfiguration(DataMap map, ProjectSession session) {
        CgenConfiguration configuration = CgenConfiguration.createDefault(map, baseDir(session).orElse(null));

        Preferences preferences = session.app().getPrefsLocator().appNode(GeneralPrefs.NODE);
        configuration.setEncoding(new GeneralPrefs(preferences).getEncoding());
        return configuration;
    }

    /**
     * Default cgen output directory: the project directory, mapped through the standard Maven layout when it applies.
     * Empty for a project that has not been saved yet.
     */
    public static Optional<Path> baseDir(ProjectSession session) {
        Path projectRoot = projectRoot(session);
        if (projectRoot == null) {
            return Optional.empty();
        }

        return Optional.of(Utils.getMavenSrcPathForPath(projectRoot)
                .map(Paths::get)
                .orElse(projectRoot));
    }

    private static Path projectRoot(ProjectSession session) {
        Project project = session.project();
        if (project == null) {
            return null;
        }
        Resource resource = project.getConfigurationResource();
        if (resource == null) {
            return null;
        }
        try {
            Path path = Path.of(resource.getURL().toURI());
            return Files.isRegularFile(path) ? path.getParent() : path;
        } catch (URISyntaxException e) {
            return null;
        }
    }
}
