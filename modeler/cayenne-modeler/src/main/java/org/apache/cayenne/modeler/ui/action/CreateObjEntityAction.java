/*
 *    Licensed to the Apache Software Foundation (ASF) under one
 *    or more contributor license agreements.  See the NOTICE file
 *    distributed with this work for additional information
 *    regarding copyright ownership.  The ASF licenses this file
 *    to you under the Apache License, Version 2.0 (the
 *    "License"); you may not use this file except in compliance
 *    with the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing,
 *    software distributed under the License is distributed on an
 *    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *    KIND, either express or implied.  See the License for the
 *    specific language governing permissions and limitations
 *    under the License.
 */
package org.apache.cayenne.modeler.ui.action;

import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.dbsync.filter.NamePatternMatcher;
import org.apache.cayenne.dbsync.merge.context.EntityMergeSupport;
import org.apache.cayenne.dbsync.naming.NameBuilder;
import org.apache.cayenne.dbsync.naming.DefaultObjectNameGenerator;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.event.display.ObjEntityDisplayEvent;
import org.apache.cayenne.modeler.event.model.ObjEntityEvent;
import org.apache.cayenne.modeler.project.ProjectSession;
import org.apache.cayenne.modeler.toolkit.AppAction;
import org.apache.cayenne.modeler.undo.CreateObjEntityUndoableEdit;
import org.apache.cayenne.util.DeleteRuleUpdater;

import java.awt.event.ActionEvent;

public class CreateObjEntityAction extends AppAction {

    static void onObjEntityCreated(
            Object src,
            ProjectSession session,
            DataMap dataMap,
            ObjEntity entity) {
        session.fireObjEntityEvent(ObjEntityEvent.ofAdd(src, entity));
        ObjEntityDisplayEvent displayEvent = new ObjEntityDisplayEvent(
                src,
                (DataChannelDescriptor) session.project().getRootNode(),
                dataMap,
                entity,
                true,
                false);
        session.displayObjEntity(displayEvent);
    }

    public CreateObjEntityAction(Application application) {
        super(application, "Create ObjEntity");
    }


    @Override
    public String getIconName() {
        return "icon-objentity.png";
    }

    @Override
    public void performAction(ActionEvent e) {
        createObjEntity();
    }

    protected void createObjEntity() {
        ProjectSession session = getProjectSession();

        DataMap dataMap = session.getSelectedDataMap();
        ObjEntity entity = new ObjEntity();
        entity.setName(NameBuilder.of(entity, dataMap).build());

        // init defaults
        entity.setSuperClassName(dataMap.getDefaultSuperclass());
        entity.setDeclaredLockType(dataMap.getDefaultLockType());

        DbEntity dbEntity = session.getSelectedDbEntity();
        if (dbEntity != null) {
            entity.setDbEntity(dbEntity);

            // TODO: use injectable name generator
            String baseName = new DefaultObjectNameGenerator().objEntityName(dbEntity);
            entity.setName(NameBuilder
                    .of(entity, dbEntity.getDataMap())
                    .preferredName(baseName)
                    .build());
        }

        entity.setClassName(dataMap.getNameWithDefaultPackage(entity.getName()));

        dataMap.addObjEntity(entity);

        EntityMergeSupport merger = new EntityMergeSupport(
                new DefaultObjectNameGenerator(),
                NamePatternMatcher.EXCLUDE_ALL,
                true,
                false);

        merger.addEntityMergeListener(DeleteRuleUpdater.getEntityMergeListener());
        merger.synchronizeWithDbEntity(entity);

        onObjEntityCreated(this, session, dataMap, entity);

        app.getUndoManager().addEdit(new CreateObjEntityUndoableEdit(getProjectSession(), dataMap, entity));
    }

    public void createObjEntity(DataMap dataMap, ObjEntity entity) {
        dataMap.addObjEntity(entity);
        onObjEntityCreated(this, getProjectSession(), dataMap, entity);
    }

    /**
     * Returns <code>true</code> if path contains a DataMap object.
     */
    @Override
    public boolean enableForPath(ConfigurationNode object) {
        if (object == null) {
            return false;
        }

        if (object instanceof ObjEntity) {
            return ((ObjEntity) object).getParent() != null
                    && ((ObjEntity) object).getParent() instanceof DataMap;
        }

        return false;
    }
}
