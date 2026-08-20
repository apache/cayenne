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

public class CgenOps {

    public static CgenConfiguration createDefaultCgenConfiguration(DataMap map, ProjectSession session) {
        Path basePath = map.getLocation() != null ? CgenOps.baseDir(session) : null;
        CgenConfiguration configuration = CgenConfiguration.createDefault(map, basePath);
        configuration.setForce(true);

        Preferences preferences = session.app().getPrefsLocator().appNode(GeneralPrefs.NODE);
        configuration.setEncoding(new GeneralPrefs(preferences).getEncoding());
        return configuration;
    }

    public static Path baseDir(ProjectSession session) {
        Path projectRoot = projectRoot(session);
        if (projectRoot == null) {
            return Paths.get(".");
        }

        return Utils.getMavenSrcPathForPath(projectRoot).map(Paths::get).orElse(projectRoot);
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
