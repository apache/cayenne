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

package org.apache.cayenne.modeler.event.model;

import org.apache.cayenne.project.Project;

/**
 * Represents events resulted from DataDomain changes in CayenneModeler.
 */
public class ProjectEvent extends ModelEvent {

    private final Project project;

    public static ProjectEvent ofAdd(Object src, Project project) {
        return new ProjectEvent(src, project, Type.ADD, null);
    }

    public static ProjectEvent ofChange(Object src, Project project) {
        return new ProjectEvent(src, project, Type.CHANGE, null);
    }

    public static ProjectEvent ofChange(Object src, Project project, String oldName) {
        return new ProjectEvent(src, project, Type.CHANGE, oldName);
    }

    public static ProjectEvent ofRemove(Object src, Project project) {
        return new ProjectEvent(src, project, Type.REMOVE, null);
    }

    private ProjectEvent(Object src, Project project, Type type, String oldName) {
        super(src, type, oldName);
        this.project = project;
    }

    public Project getProject() {
        return project;
    }

    @Override
    public String getNewName() {
        return (project != null) ? project.getName() : null;
    }
}
