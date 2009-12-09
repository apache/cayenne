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
package org.apache.cayenne.modeler;

import java.io.File;

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.conf.Configuration;
import org.apache.cayenne.modeler.action.ModelerProjectConfiguration;
import org.apache.cayenne.modeler.graph.GraphFile;
import org.apache.cayenne.modeler.graph.GraphMap;
import org.apache.cayenne.project.ApplicationProject;
import org.apache.cayenne.project.ProjectFile;

/**
 * Modeler-specific project. 
 * Used to save domain graphs together with main project files
 */
public class ModelerProject extends ApplicationProject {
    public ModelerProject(File projectFile, Configuration configuration) {
        super(projectFile, configuration);
    }

    /**
     * Returns appropriate ProjectFile or null if object does not require a file of its
     * own. In case of ModelerProject, the nodes that require separate files are: the
     * project itself, each DataMap, each driver DataNode, and each DataDomain that had
     * graphs built for.
     */
    @Override
    public ProjectFile projectFileForObject(Object obj) {
        if (requiresDomainFile(obj)) {
            GraphMap map = ((ModelerProjectConfiguration) getConfiguration()).
                getGraphRegistry().getGraphMap((DataDomain) obj);
            
            if (map.size() > 0) {
                return new GraphFile(this, map);
            }
        }
        return super.projectFileForObject(obj);
    }

    protected boolean requiresDomainFile(Object obj) {
        return obj instanceof DataDomain;
    }
}
