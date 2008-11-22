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

import java.util.ArrayList;
import java.util.List;

/**
 */
public class TstProjectTraversalHelper implements ProjectTraversalHandler {
    protected List nodes = new ArrayList();
    
    /**
     * Constructor for TstProjectTraversalHelper.
     */
    public TstProjectTraversalHelper() {
        super();
    }

    /**
     * @see org.apache.cayenne.project.ProjectTraversalHandler#projectNode(Object[])
     */
    public void projectNode(ProjectPath nodePath) {
        nodes.add(nodePath.getObject());   
    }

    /**
     * @see org.apache.cayenne.project.ProjectTraversalHandler#shouldReadChildren(Object, Object[])
     */
    public boolean shouldReadChildren(Object node, ProjectPath parentPath) {
        return true;
    }

    /**
     * Returns the nodes.
     * @return List
     */
    public List getNodes() {
        return nodes;
    }
}

