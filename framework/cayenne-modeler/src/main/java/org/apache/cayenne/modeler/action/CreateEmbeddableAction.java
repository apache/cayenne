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
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.event.EmbeddableEvent;
import org.apache.cayenne.map.event.MapEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.event.EmbeddableDisplayEvent;
import org.apache.cayenne.modeler.undo.CreateEmbeddableUndoableEdit;
import org.apache.cayenne.modeler.util.CayenneAction;
import org.apache.cayenne.project.NamedObjectFactory;
import org.apache.cayenne.project.ProjectPath;

public class CreateEmbeddableAction extends CayenneAction {

    public static String getActionName() {
        return "Create Embeddable";
    }

    public CreateEmbeddableAction(Application application) {
        super(getActionName(), application);
    }

    @Override
    public String getIconName() {
        return "icon-new_embeddable.gif";
    }

    @Override
    public void performAction(ActionEvent e) {
        ProjectController mediator = getProjectController();

        DataMap dataMap = mediator.getCurrentDataMap();

        Embeddable embeddable = (Embeddable) NamedObjectFactory.createObject(
                Embeddable.class,
                mediator.getCurrentDataMap());

        createEmbeddable(dataMap, embeddable);

        application.getUndoManager().addEdit(
                new CreateEmbeddableUndoableEdit(dataMap, embeddable));
    }

    public void createEmbeddable(DataMap dataMap, Embeddable embeddable) {
        dataMap.addEmbeddable(embeddable);
        fireEmbeddableEvent(this, getProjectController(), dataMap, embeddable);
    }

    static void fireEmbeddableEvent(
            Object src,
            ProjectController mediator,
            DataMap dataMap,
            Embeddable embeddable) {

        mediator.fireEmbeddableEvent(
                new EmbeddableEvent(src, embeddable, MapEvent.ADD),
                dataMap);
        EmbeddableDisplayEvent displayEvent = new EmbeddableDisplayEvent(
                src,
                embeddable,
                dataMap,
                mediator.getCurrentDataDomain());
        displayEvent.setMainTabFocus(true);
        mediator.fireEmbeddableDisplayEvent(displayEvent);

    }

    /**
     * Returns <code>true</code> if path contains a DataMap object.
     */
    @Override
    public boolean enableForPath(ProjectPath path) {
        if (path == null) {
            return false;
        }

        return path.firstInstanceOf(DataMap.class) != null;
    }
}
