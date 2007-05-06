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
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.event.EntityEvent;
import org.apache.cayenne.map.event.MapEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.event.EntityDisplayEvent;
import org.apache.cayenne.modeler.util.CayenneAction;
import org.apache.cayenne.project.NamedObjectFactory;
import org.apache.cayenne.project.ProjectPath;
import org.apache.cayenne.util.EntityMergeSupport;
import org.apache.cayenne.util.NameConverter;

/**
 * @author Andrei Adamchik
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

    public String getIconName() {
        return "icon-new_objentity.gif";
    }

    /**
     * @see org.apache.cayenne.modeler.util.CayenneAction#performAction(ActionEvent)
     */
    public void performAction(ActionEvent e) {
        createObjEntity();
    }

    protected void createObjEntity() {
        ProjectController mediator = getProjectController();

        DataMap dataMap = mediator.getCurrentDataMap();
        ObjEntity entity = (ObjEntity) NamedObjectFactory.createObject(
                ObjEntity.class,
                mediator.getCurrentDataMap());

        // init defaults
        entity.setSuperClassName(dataMap.getDefaultSuperclass());
        entity.setDeclaredLockType(dataMap.getDefaultLockType());

        DbEntity dbEntity = mediator.getCurrentDbEntity();
        if (dbEntity != null) {
            entity.setDbEntity(dbEntity);
            String baseName = NameConverter.underscoredToJava(dbEntity.getName(), true);
            String entityName = NamedObjectFactory.createName(ObjEntity.class, dbEntity
                    .getDataMap(), baseName);
            entity.setName(entityName);
        }

        String pkg = dataMap.getDefaultPackage();
        if (pkg != null) {
            if (!pkg.endsWith(".")) {
                pkg = pkg + ".";
            }

            entity.setClassName(pkg + entity.getName());
        }

        dataMap.addObjEntity(entity);

        // perform the merge
        EntityMergeSupport merger = new EntityMergeSupport(dataMap);
        merger.synchronizeWithDbEntity(entity);

        mediator.fireObjEntityEvent(new EntityEvent(this, entity, MapEvent.ADD));
        EntityDisplayEvent displayEvent = new EntityDisplayEvent(
                this,
                entity,
                dataMap,
                mediator.getCurrentDataNode(),
                mediator.getCurrentDataDomain());
        mediator.fireObjEntityDisplayEvent(displayEvent);
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
