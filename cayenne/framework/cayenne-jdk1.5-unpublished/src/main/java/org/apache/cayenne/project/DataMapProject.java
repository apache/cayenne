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

package org.apache.cayenne.project;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.cayenne.conf.ConfigStatus;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.MapLoader;
import org.xml.sax.InputSource;

/**
 * Cayenne project that consists of a single DataMap.
 * 
 */
public class DataMapProject extends Project {

    protected DataMap map;

    /**
     * Constructor for MapProject.
     * 
     * @param projectFile
     */
    public DataMapProject(File projectFile) {
        super(projectFile);
    }

    /**
     * @since 1.1
     */
    @Override
    public void upgrade() throws ProjectException {
        // upgrades not supported in this type of project
        throw new ProjectException("'DataMapProject' does not support upgrades.");
    }

    /**
     * Does nothing.
     */
    @Override
    public void checkForUpgrades() {
        // do nothing
    }

    /**
     * Initializes internal <code>map</code> object and then calls super.
     */
    @Override
    protected void postInitialize(File projectFile) {
        if (projectFile != null) {
            try {
                InputStream in = new FileInputStream(projectFile.getCanonicalFile());
                map = new MapLoader().loadDataMap(new InputSource(in));

                String fileName = resolveSymbolicName(projectFile);
                String mapName = (fileName != null && fileName
                        .endsWith(DataMapFile.LOCATION_SUFFIX))
                        ? fileName.substring(0, fileName.length()
                                - DataMapFile.LOCATION_SUFFIX.length())
                        : "UntitledMap";

                map.setName(mapName);
            }
            catch (Exception dme) {
                throw new ProjectException(
                        "Error creating " + this.getClass().getName(),
                        dme);
            }
        }
        else {
            map = (DataMap) NamedObjectFactory.createObject(DataMap.class, null);
        }

        super.postInitialize(projectFile);
    }

    /**
     * Returns a list that contains project DataMap as a single object.
     */
    @Override
    public List getChildren() {
        List<DataMap> entities = new ArrayList<DataMap>();
        entities.add(map);
        return entities;
    }

    /**
     * Returns appropriate ProjectFile or null if object does not require a file of its
     * own. In case of DataMapProject, the only object that requires a file is the project
     * itself.
     */
    @Override
    public ProjectFile projectFileForObject(Object obj) {
        if (obj == this) {
            return new DataMapFile(this, map);
        }

        return null;
    }

    /**
     * Always returns empty status. Map projects do not support status tracking yet.
     */
    @Override
    public ConfigStatus getLoadStatus() {
        return new ConfigStatus();
    }
}
