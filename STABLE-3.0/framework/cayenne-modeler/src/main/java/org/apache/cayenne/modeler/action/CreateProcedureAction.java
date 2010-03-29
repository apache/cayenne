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

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.event.MapEvent;
import org.apache.cayenne.map.event.ProcedureEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.event.ProcedureDisplayEvent;
import org.apache.cayenne.modeler.undo.CreateProcedureUndoableEdit;
import org.apache.cayenne.modeler.util.CayenneAction;
import org.apache.cayenne.project.NamedObjectFactory;
import org.apache.cayenne.project.ProjectPath;

/**
 * Action class to create a new stored procedure mapping.
 * 
 */
public class CreateProcedureAction extends CayenneAction {

    

    public static String getActionName() {
        return "Create Stored Procedure";
    }

    public CreateProcedureAction(Application application) {
        super(getActionName(), application);
    }

    public void performAction(ActionEvent e) {
        ProjectController mediator = getProjectController();
        DataMap map = mediator.getCurrentDataMap();

        Procedure procedure = (Procedure) NamedObjectFactory.createObject(
                Procedure.class,
                map);

        createProcedure(map, procedure);

        application.getUndoManager().addEdit(
                new CreateProcedureUndoableEdit(map, procedure));
    }

    /**
     * Fires events when a procedure was added
     */
    static void fireProcedureEvent(
            Object src,
            ProjectController mediator,
            DataMap dataMap,
            Procedure procedure) {
        mediator.fireProcedureEvent(new ProcedureEvent(src, procedure, MapEvent.ADD));
        mediator.fireProcedureDisplayEvent(new ProcedureDisplayEvent(
                src,
                procedure,
                mediator.getCurrentDataMap(),
                mediator.getCurrentDataDomain()));
    }

    public void createProcedure(DataMap map, Procedure procedure) {
        ProjectController mediator = getProjectController();
        procedure.setSchema(map.getDefaultSchema());
        map.addProcedure(procedure);
        fireProcedureEvent(this, mediator, map, procedure);
    }

    /**
     * Returns <code>true</code> if path contains a DataMap object.
     */
    public boolean enableForPath(ProjectPath path) {
        if (path == null) {
            return false;
        }

        return path.firstInstanceOf(DataMap.class) != null;
    }

    public String getIconName() {
        return "icon-stored-procedure.gif";
    }
}
