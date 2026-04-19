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

package org.apache.cayenne.modeler.action;

import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.dbsync.naming.NameBuilder;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.event.EntityEvent;
import org.apache.cayenne.map.event.MapEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.event.display.EntityDisplayEvent;
import org.apache.cayenne.modeler.undo.CreateDbEntityUndoableEdit;
import org.apache.cayenne.modeler.util.CayenneAction;

import java.awt.event.ActionEvent;

public class CreateDbEntityAction extends CayenneAction {

    static void onDbEntityCreated(Object src, ProjectController controller, DbEntity entity) {
        controller.fireDbEntityEvent(new EntityEvent(src, entity, MapEvent.ADD));
        EntityDisplayEvent displayEvent = new EntityDisplayEvent(src, entity, controller.getSelectedDataMap(),
                controller.getSelectedDataNode(), (DataChannelDescriptor) controller.getProject().getRootNode());
        displayEvent.setMainTabFocus(true);
        controller.displayDbEntity(displayEvent);
    }

    public CreateDbEntityAction(Application application) {
        super("Create DbEntity", application);
    }

    @Override
    public String getIconName() {
        return "icon-dbentity.png";
    }

    /**
     * Creates new DbEntity, adds it to the current DataMap, fires DbEntityEvent and DbEntityDisplayEvent.
     */
    public void performAction(ActionEvent e) {
        DataMap map = getProjectController().getSelectedDataMap();
        DbEntity entity = new DbEntity();
        entity.setName(NameBuilder.builder(entity, map).name());
        createEntity(map, entity);

        application.getUndoManager().addEdit(new CreateDbEntityUndoableEdit(map, entity));
    }

    /**
     * Constructs and returns a new DbEntity. Entity returned is added to the
     * DataMap.
     */
    public void createEntity(DataMap map, DbEntity entity) {
        entity.setCatalog(map.getDefaultCatalog());
        entity.setSchema(map.getDefaultSchema());
        map.addDbEntity(entity);
        onDbEntityCreated(this, getProjectController(), entity);
    }

    /**
     * Returns <code>true</code> if path contains a DataMap object.
     */
    @Override
    public boolean enableForPath(ConfigurationNode object) {
        if (object == null) {
            return false;
        }

        return ((Entity<?, ?, ?>) object).getDataMap() != null;
    }
}
