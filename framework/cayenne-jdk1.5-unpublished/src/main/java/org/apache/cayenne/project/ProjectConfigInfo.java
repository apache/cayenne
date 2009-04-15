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
import java.util.ArrayList;
import java.util.List;

/**
 * Stores project information necessary to reconfigure existing projects.
 * 
 * @deprecated since 3.0. {@link ProjectConfigurator} approach turned out to be not
 *             usable, and is in fact rarely used (if ever). It will be removed in
 *             subsequent releases.
 */
public class ProjectConfigInfo {

    protected File sourceJar;
    protected File destJar;
    protected File altProjectFile;
    protected List<DataNodeConfigInfo> nodes = new ArrayList<DataNodeConfigInfo>();

    public void addToNodes(DataNodeConfigInfo nodeInfo) {
        nodes.add(nodeInfo);
    }

    /**
     * Returns the altProjectFile.
     * 
     * @return File
     */
    public File getAltProjectFile() {
        return altProjectFile;
    }

    /**
     * Returns the destJar.
     * 
     * @return File
     */
    public File getDestJar() {
        return destJar;
    }

    /**
     * Returns the nodes.
     * 
     * @return List
     */
    public List<DataNodeConfigInfo> getNodes() {
        return nodes;
    }

    /**
     * Returns the sourceJar.
     * 
     * @return File
     */
    public File getSourceJar() {
        return sourceJar;
    }

    /**
     * Sets the altProjectFile.
     * 
     * @param altProjectFile The altProjectFile to set
     */
    public void setAltProjectFile(File altProjectFile) {
        this.altProjectFile = altProjectFile;
    }

    /**
     * Sets the destJar.
     * 
     * @param destJar The destJar to set
     */
    public void setDestJar(File destJar) {
        this.destJar = destJar;
    }

    /**
     * Sets the nodes.
     * 
     * @param nodes The nodes to set
     */
    public void setNodes(List<DataNodeConfigInfo> nodes) {
        this.nodes = nodes;
    }

    /**
     * Sets the sourceJar.
     * 
     * @param sourceJar The sourceJar to set
     */
    public void setSourceJar(File sourceJar) {
        this.sourceJar = sourceJar;
    }
}
