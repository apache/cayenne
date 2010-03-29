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

package org.apache.cayenne.modeler.action;

import java.awt.event.ActionEvent;

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.map.event.DomainEvent;
import org.apache.cayenne.map.event.MapEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.event.DomainDisplayEvent;
import org.apache.cayenne.modeler.undo.CreateDomainUndoableEdit;
import org.apache.cayenne.modeler.util.CayenneAction;
import org.apache.cayenne.project.ApplicationProject;
import org.apache.cayenne.project.NamedObjectFactory;
import org.apache.cayenne.project.ProjectPath;

/**
 */
public class CreateDomainAction extends CayenneAction {

    

    public static String getActionName() {
        return "Create DataDomain";
    }

    /**
     * Constructor for CreateDomainAction.
     * 
     * @param name
     */
    public CreateDomainAction(Application application) {
        super(getActionName(), application);
    }

    public String getIconName() {
        return "icon-dom.gif";
    }

    public void performAction(ActionEvent e) {
        ApplicationProject project = (ApplicationProject) getCurrentProject();
        ProjectController mediator = getProjectController();

        DataDomain domain = (DataDomain) NamedObjectFactory.createObject(
                DataDomain.class,
                project.getConfiguration());

        domain.getEntityResolver().setIndexedByClass(false);

        createDomain(domain);

        application.getUndoManager().addEdit(new CreateDomainUndoableEdit(domain));
    }

    public void createDomain(DataDomain domain) {
        ApplicationProject project = (ApplicationProject) getCurrentProject();
        ProjectController mediator = getProjectController();

        project.getConfiguration().addDomain(domain);
        mediator.fireDomainEvent(new DomainEvent(this, domain, MapEvent.ADD));
        mediator.fireDomainDisplayEvent(new DomainDisplayEvent(this, domain));
    }

    /**
     * Returns <code>true</code> if path contains a ApplicationProject object.
     */
    public boolean enableForPath(ProjectPath path) {
        if (path == null) {
            return false;
        }

        return path.firstInstanceOf(ApplicationProject.class) != null;
    }
}
