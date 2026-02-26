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
package org.apache.cayenne.project;

import org.apache.cayenne.configuration.xml.ProjectVersion;
import org.apache.cayenne.resource.Resource;

/**
 * Defines API of a project saver.
 * 
 * @since 3.1
 */
public interface ProjectSaver {

    /**
     * Returns a version of the project configuration supported by the current runtime.
     */
    ProjectVersion getSupportedVersion();

    /**
     * Saves project in the location of its current configuration sources. Since resource
     * names are determined using a naming convention based on the project node names, if
     * any of the nodes were renamed, the old locations will be deleted. After saving,
     * resets configuration sources of all project objects to the new Resources.
     */
    void save(Project project);

    /**
     * Saves project in a location defined by the 'baseDirectory' Resource. Does not
     * delete the old resource locations. After saving, resets configuration sources of
     * all project objects to the new Resources.
     */
    void saveAs(Project project, Resource baseDirectory);
}
