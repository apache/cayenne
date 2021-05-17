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

import java.io.File;

import org.gradle.api.DefaultTask;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.tasks.Internal;

/**
 * @since 4.0
 */
public class BaseCayenneTask extends DefaultTask {

    @Internal
    private File map;

    @Internal
    private String mapFileName;

    public File getMap() {
        return map;
    }

    public void setMap(File mapFile) {
        map = mapFile;
    }

    public void setMap(String mapFileName) {
        this.mapFileName = mapFileName;
    }

    public void map(String mapFileName) {
        setMap(mapFileName);
    }

    public void map(File mapFile) {
        setMap(mapFile);
    }

    public String getMapFileName() {
        return mapFileName;
    }

    @Internal
    public File getDataMapFile() {
        if (map != null) {
            return map;
        }

        if (mapFileName == null) {
            mapFileName = getProject().getExtensions().getByType(GradleCayenneExtension.class).getDefaultDataMap();
        }

        if (mapFileName != null) {
            return getProject().file(mapFileName);
        }

        throw new InvalidUserDataException("No datamap configured in task or in cayenne.defaultDataMap.");
    }
}