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
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.event.EntityEvent;
import org.apache.cayenne.map.event.MapEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.event.EntityDisplayEvent;
import org.apache.cayenne.modeler.undo.CreateDbEntityUndoableEdit;
import org.apache.cayenne.modeler.util.CayenneAction;
import org.apache.cayenne.project.NamedObjectFactory;
import org.apache.cayenne.project.ProjectPath;

/**
 */
public class CreateDbEntityAction extends CayenneAction {

    

    public static String getActionName() {
        return "Create DbEntity";
    }

    /**
     * Constructor for CreateDbEntityAction.
     */
    public CreateDbEntityAction(Application application) {
        super(getActionName(), application);
    }

    public String getIconName() {
        return "icon-dbentity.gif";
    }

    /**
     * Creates new DbEntity, adds it to the current DataMap, fires DbEntityEvent and
     * DbEntityDisplayEvent.
     * 
     * @see org.apache.cayenne.modeler.util.CayenneAction#performAction(ActionEvent)
     */
    public void performAction(ActionEvent e) {
        ProjectController mediator = getProjectController();

        DataMap map = mediator.getCurrentDataMap();
        DbEntity entity = (DbEntity) NamedObjectFactory.createObject(DbEntity.class, map);

        createEntity(map, entity);

        application.getUndoManager().addEdit(new CreateDbEntityUndoableEdit(map, entity));
    }

    /**
     * Fires events when a db entity was added
     */
    static void fireDbEntityEvent(Object src, ProjectController mediator, DbEntity entity) {
        mediator.fireDbEntityEvent(new EntityEvent(src, entity, MapEvent.ADD));
        EntityDisplayEvent displayEvent = new EntityDisplayEvent(src, entity, mediator
                .getCurrentDataMap(), mediator.getCurrentDataNode(), mediator
                .getCurrentDataDomain());
        displayEvent.setMainTabFocus(true);
        mediator.fireDbEntityDisplayEvent(displayEvent);
    }

    /**
     * Constructs and returns a new DbEntity. Entity returned is added to the DataMap.
     */
    public void createEntity(DataMap map, DbEntity entity) {
        ProjectController mediator = getProjectController();
        entity.setSchema(map.getDefaultSchema());
        map.addDbEntity(entity);
        fireDbEntityEvent(this, mediator, entity);
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
}
