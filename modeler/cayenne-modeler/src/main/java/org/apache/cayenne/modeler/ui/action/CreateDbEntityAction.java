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

package org.apache.cayenne.modeler.ui.action;

import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.dbsync.naming.NameBuilder;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.event.display.DbEntityDisplayEvent;
import org.apache.cayenne.modeler.event.model.DbEntityEvent;
import org.apache.cayenne.modeler.project.ProjectSession;
import org.apache.cayenne.modeler.toolkit.AppAction;
import org.apache.cayenne.modeler.undo.CreateDbEntityUndoableEdit;

import java.awt.event.ActionEvent;

public class CreateDbEntityAction extends AppAction {

    static void onDbEntityCreated(Object src, ProjectSession session, DbEntity entity) {
        session.fireDbEntityEvent(DbEntityEvent.ofAdd(src, entity));
        DbEntityDisplayEvent displayEvent = new DbEntityDisplayEvent(
                src,
                (DataChannelDescriptor) session.project().getRootNode(),
                session.getSelectedDataMap(),
                entity,
                true,
                false);
        session.displayDbEntity(displayEvent);
    }

    public CreateDbEntityAction(Application application) {
        super(application, "Create DbEntity");
    }

    @Override
    public String getIconName() {
        return "icon-dbentity.png";
    }

    /**
     * Creates new DbEntity, adds it to the current DataMap, fires DbEntityEvent and DbEntityDisplayEvent.
     */
    public void performAction(ActionEvent e) {
        DataMap map = getProjectSession().getSelectedDataMap();
        DbEntity entity = new DbEntity();
        entity.setName(NameBuilder.of(entity, map).name());
        createEntity(map, entity);

        app.getUndoManager().addEdit(new CreateDbEntityUndoableEdit(getProjectSession(), map, entity));
    }

    /**
     * Constructs and returns a new DbEntity. Entity returned is added to the
     * DataMap.
     */
    public void createEntity(DataMap map, DbEntity entity) {
        entity.setCatalog(map.getDefaultCatalog());
        entity.setSchema(map.getDefaultSchema());
        map.addDbEntity(entity);
        onDbEntityCreated(this, getProjectSession(), entity);
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
