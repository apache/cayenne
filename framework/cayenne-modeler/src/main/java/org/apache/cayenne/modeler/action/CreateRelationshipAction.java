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

import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.map.event.MapEvent;
import org.apache.cayenne.map.event.RelationshipEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.event.RelationshipDisplayEvent;
import org.apache.cayenne.modeler.undo.CreateRelationshipUndoableEdit;
import org.apache.cayenne.modeler.util.CayenneAction;
import org.apache.cayenne.project.NamedObjectFactory;
import org.apache.cayenne.project.ProjectPath;
import org.apache.cayenne.util.DeleteRuleUpdater;

/**
 */
public class CreateRelationshipAction extends CayenneAction {

    

    public static String getActionName() {
        return "Create Relationship";
    }

    /**
     * Constructor for CreateRelationshipAction.
     */
    public CreateRelationshipAction(Application application) {
        super(getActionName(), application);
    }

    @Override
    public String getIconName() {
        return "icon-relationship.gif";
    }

    /**
     * @see org.apache.cayenne.modeler.util.CayenneAction#performAction(ActionEvent)
     */
    @Override
    public void performAction(ActionEvent e) {
        ObjEntity objEnt = getProjectController().getCurrentObjEntity();
        if (objEnt != null) {

            ObjRelationship rel = (ObjRelationship) NamedObjectFactory.createObject(
                    ObjRelationship.class,
                    objEnt);

            createObjRelationship(objEnt, rel);

            application.getUndoManager().addEdit(
                    new CreateRelationshipUndoableEdit(objEnt, new ObjRelationship[] {
                        rel
                    }));
        }
        else {
            DbEntity dbEnt = getProjectController().getCurrentDbEntity();
            if (dbEnt != null) {

                DbRelationship rel = (DbRelationship) NamedObjectFactory.createObject(
                        DbRelationship.class,
                        dbEnt);

                createDbRelationship(dbEnt, rel);

                application.getUndoManager().addEdit(
                        new CreateRelationshipUndoableEdit(dbEnt, new DbRelationship[] {
                            rel
                        }));
            }
        }
    }

    public void createObjRelationship(ObjEntity objEntity, ObjRelationship rel) {
        ProjectController mediator = getProjectController();

        rel.setSourceEntity(objEntity);
        DeleteRuleUpdater.updateObjRelationship(rel);

        objEntity.addRelationship(rel);
        fireObjRelationshipEvent(this, mediator, objEntity, rel);
    }

    /**
     * Fires events when a obj rel was added
     */
    static void fireObjRelationshipEvent(
            Object src,
            ProjectController mediator,
            ObjEntity objEntity,
            ObjRelationship rel) {

        mediator.fireObjRelationshipEvent(new RelationshipEvent(
                src,
                rel,
                objEntity,
                MapEvent.ADD));

        RelationshipDisplayEvent rde = new RelationshipDisplayEvent(
                src,
                rel,
                objEntity,
                mediator.getCurrentDataMap(),
                mediator.getCurrentDataDomain());

        mediator.fireObjRelationshipDisplayEvent(rde);
    }

    public void createDbRelationship(DbEntity dbEntity, DbRelationship rel) {
        ProjectController mediator = getProjectController();

        rel.setSourceEntity(dbEntity);
        dbEntity.addRelationship(rel);

        fireDbRelationshipEvent(this, mediator, dbEntity, rel);
    }

    /**
     * Fires events when a db rel was added
     */
    static void fireDbRelationshipEvent(
            Object src,
            ProjectController mediator,
            DbEntity dbEntity,
            DbRelationship rel) {

        mediator.fireDbRelationshipEvent(new RelationshipEvent(
                src,
                rel,
                dbEntity,
                MapEvent.ADD));

        RelationshipDisplayEvent rde = new RelationshipDisplayEvent(
                src,
                rel,
                dbEntity,
                mediator.getCurrentDataMap(),
                mediator.getCurrentDataDomain());

        mediator.fireDbRelationshipDisplayEvent(rde);
    }

    /**
     * Returns <code>true</code> if path contains an Entity object.
     */
    @Override
    public boolean enableForPath(ProjectPath path) {
        if (path == null) {
            return false;
        }

        return path.firstInstanceOf(Entity.class) != null;
    }
}
