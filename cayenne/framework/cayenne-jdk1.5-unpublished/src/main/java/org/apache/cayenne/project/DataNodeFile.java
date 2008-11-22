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

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.conf.ConfigSaver;
import org.apache.cayenne.conf.DriverDataSourceFactory;

/**
 * DataNodeFile is a ProjectFile abstraction of the 
 * DataNode file in a Cayenne project. 
 * 
 */
public class DataNodeFile extends ProjectFile {
    public static final String LOCATION_SUFFIX = ".driver.xml";

    protected DataNode nodeObj;

    public DataNodeFile() {}

    /**
     * Constructor for DataNodeFile.
     */
    public DataNodeFile(Project project, DataNode node) {
        super(project, node.getDataSourceLocation());
        this.nodeObj = node;
    }

    /**
     * @see ProjectFile#getObject()
     */
    @Override
    public Object getObject() {
        return nodeObj;
    }

    /**
     * @see ProjectFile#getObjectName()
     */
    @Override
    public String getObjectName() {
        return nodeObj.getName();
    }

    @Override
    public void save(PrintWriter out) throws Exception {
        ProjectDataSource src = (ProjectDataSource) nodeObj.getDataSource();
        new ConfigSaver().storeDataNode(out, getProject(), src.getDataSourceInfo());
    }

    /**
     * @see org.apache.cayenne.project.ProjectFile#canHandle(Object)
     */
    @Override
    public boolean canHandle(Object obj) {
        if (obj instanceof DataNode) {
            DataNode node = (DataNode) obj;

            // only driver datasource factory requires a file
            if (DriverDataSourceFactory
                .class
                .getName()
                .equals(node.getDataSourceFactory())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Updates node location to match the name before save.
     */
    @Override
    public void willSave() {
        super.willSave();

        if (nodeObj != null && canHandle(nodeObj)) {
            nodeObj.setDataSourceLocation(getLocation());
        }
    }

    /**
     * Returns ".driver.xml" that should be used as a file suffix 
     * for DataNode driver files.
     */
    @Override
    public String getLocationSuffix() {
        return LOCATION_SUFFIX;
    }
}
