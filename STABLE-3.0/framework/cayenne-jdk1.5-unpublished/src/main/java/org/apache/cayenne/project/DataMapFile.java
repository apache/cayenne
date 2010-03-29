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

import java.io.PrintWriter;

import org.apache.cayenne.map.DataMap;

/**
 * DataMapFile is a ProjectFile abstraction of the 
 * DataMap file in a Cayenne project. 
 * 
 */
public class DataMapFile extends ProjectFile {
    public static final String LOCATION_SUFFIX = ".map.xml";
    
    protected DataMap map;
    
    public DataMapFile() {}

    /**
     * Constructor for DataMapFile.
     */
    public DataMapFile(Project project, DataMap map) {
        super(project, map.getLocation());
        this.map = map;
    }

    /**
     * Returns DataMap associated with this project.
     */
    @Override
    public Object getObject() {
        return map;
    }

    /**
     * @see org.apache.cayenne.project.ProjectFile#getObjectName()
     */
    @Override
    public String getObjectName() {
        return map.getName();
    }

    @Override
    public void save(PrintWriter out) throws Exception {
        map.encodeAsXML(out);
    }

    /**
     * @see org.apache.cayenne.project.ProjectFile#canHandle(Object)
     */
    @Override
    public boolean canHandle(Object obj) {
        return obj instanceof DataMap;
    }

    /**
     * Updates map location to match the name before save.
     */
    @Override
    public void willSave() {
        super.willSave();

        if (map != null) {
            map.setLocation(getLocation());
        }
    }

    /**
     * Returns ".map.xml" that should be used as a file suffix for DataMaps.
     */
    @Override
    public String getLocationSuffix() {
        return LOCATION_SUFFIX;
    }
}
