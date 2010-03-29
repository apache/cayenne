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
import java.util.Iterator;
import java.util.List;

import org.apache.cayenne.conf.ConfigStatus;

/**
 * Concrete subclass of Project used for testing purposes.
 * 
 */
public class TstProject extends Project {

    /**
     * Constructor for TstProject.
     * @param name
     * @param projectFile
     */
    public TstProject(File projectFile) {
        super(projectFile);   
    }

    /**
     * @see org.apache.cayenne.project.Project#checkForUpgrades()
     */
    @Override
    public void checkForUpgrades() {}

    /**
     * @see org.apache.cayenne.project.Project#treeNodes()
     */
    @Override
    public Iterator treeNodes() {
        return new ArrayList().iterator();
    }
    
    /**
     * @see org.apache.cayenne.project.Project#getChildren()
     */
    @Override
    public List getChildren() {
        return new ArrayList();
    }

    /**
     * @see org.apache.cayenne.project.Project#projectFileForObject(Object)
     */
    @Override
    public ProjectFile projectFileForObject(Object obj) {
        return null;
    }
    
    /**
     * @see org.apache.cayenne.project.Project#projectLoadStatus()
     */
    @Override
    public ConfigStatus getLoadStatus() {
        return null;
    }
    
    @Override
    public void upgrade() throws ProjectException {

    }
}

