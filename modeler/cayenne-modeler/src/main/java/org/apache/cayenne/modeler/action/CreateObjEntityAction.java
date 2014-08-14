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

import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.event.EntityEvent;
import org.apache.cayenne.map.event.MapEvent;
import org.apache.cayenne.map.naming.DefaultUniqueNameGenerator;
import org.apache.cayenne.map.naming.NameCheckers;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.event.EntityDisplayEvent;
import org.apache.cayenne.modeler.undo.CreateObjEntityUndoableEdit;
import org.apache.cayenne.modeler.util.CayenneAction;
import org.apache.cayenne.util.DeleteRuleUpdater;
import org.apache.cayenne.util.EntityMergeSupport;
import org.apache.cayenne.map.naming.NameConverter;

/**
 */
public class CreateObjEntityAction extends CayenneAction {

    public static String getActionName() {
        return "Create ObjEntity";
    }

    /**
     * Constructor for CreateObjEntityAction.
     */
    public CreateObjEntityAction(Application application) {
        super(getActionName(), application);
    }

    @Override
    public String getIconName() {
        return "icon-new_objentity.gif";
    }

    /**
     * @see org.apache.cayenne.modeler.util.CayenneAction#performAction(ActionEvent)
     */
    @Override
    public void performAction(ActionEvent e) {
        createObjEntity();
    }

    protected void createObjEntity() {
        ProjectController mediator = getProjectController();

        DataMap dataMap = mediator.getCurrentDataMap();
        ObjEntity entity = new ObjEntity(DefaultUniqueNameGenerator.generate(NameCheckers.ObjEntity, dataMap));

        // init defaults
        entity.setSuperClassName(dataMap.getDefaultSuperclass());
        entity.setDeclaredLockType(dataMap.getDefaultLockType());

        DbEntity dbEntity = mediator.getCurrentDbEntity();
        if (dbEntity != null) {
            entity.setDbEntity(dbEntity);
            String baseName = NameConverter.underscoredToJava(dbEntity.getName(), true);
            entity.setName(DefaultUniqueNameGenerator.generate(NameCheckers.ObjEntity, dbEntity.getDataMap(), baseName));
        }

        String pkg = dataMap.getDefaultPackage();
        if (pkg != null) {
            if (!pkg.endsWith(".")) {
                pkg = pkg + ".";
            }

            entity.setClassName(pkg + entity.getName());
        }

        if (dataMap.isClientSupported()) {
            String clientPkg = dataMap.getDefaultClientPackage();
            if (clientPkg != null) {
                if (!clientPkg.endsWith(".")) {
                    clientPkg = clientPkg + ".";
                }

                entity.setClientClassName(clientPkg + entity.getName());
            }

            entity.setClientSuperClassName(dataMap.getDefaultClientSuperclass());
        }

        dataMap.addObjEntity(entity);

        // perform the merge
        EntityMergeSupport merger = new EntityMergeSupport(dataMap);
        merger.addEntityMergeListener(DeleteRuleUpdater.getEntityMergeListener());
        merger.synchronizeWithDbEntity(entity);

        fireObjEntityEvent(this, mediator, dataMap, entity);

        application.getUndoManager().addEdit(
                new CreateObjEntityUndoableEdit(dataMap, entity));
    }

    public void createObjEntity(DataMap dataMap, ObjEntity entity) {
        ProjectController mediator = getProjectController();
        dataMap.addObjEntity(entity);
        fireObjEntityEvent(this, mediator, dataMap, entity);
    }

    /**
     * Fires events when a obj entity was added
     */
    static void fireObjEntityEvent(
            Object src,
            ProjectController mediator,
            DataMap dataMap,
            ObjEntity entity) {
        mediator.fireObjEntityEvent(new EntityEvent(src, entity, MapEvent.ADD));
        EntityDisplayEvent displayEvent = new EntityDisplayEvent(
                src,
                entity,
                dataMap,
                mediator.getCurrentDataNode(),
                (DataChannelDescriptor) mediator.getProject().getRootNode());
        displayEvent.setMainTabFocus(true);
        mediator.fireObjEntityDisplayEvent(displayEvent);
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
