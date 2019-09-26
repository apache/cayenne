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

import java.awt.event.ActionEvent;

import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.map.Relationship;
import org.apache.cayenne.map.event.MapEvent;
import org.apache.cayenne.map.event.RelationshipEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.dialog.DbRelationshipDialog;
import org.apache.cayenne.modeler.dialog.objentity.ObjRelationshipInfo;
import org.apache.cayenne.modeler.event.RelationshipDisplayEvent;
import org.apache.cayenne.modeler.util.CayenneAction;
import org.apache.cayenne.util.DeleteRuleUpdater;

public class CreateRelationshipAction extends CayenneAction {

    /**
     * Constructor for CreateRelationshipAction.
     */
    public CreateRelationshipAction(Application application) {
        super(getActionName(), application);
    }

    public static String getActionName() {
        return "Create Relationship";
    }

    /**
     * Fires events when a obj rel was added
     */
    static void fireObjRelationshipEvent(Object src, ProjectController mediator, ObjEntity objEntity,
                                         ObjRelationship rel) {

        mediator.fireObjRelationshipEvent(new RelationshipEvent(src, rel, objEntity, MapEvent.ADD));

        RelationshipDisplayEvent rde = new RelationshipDisplayEvent(src, rel, objEntity, mediator.getCurrentDataMap(),
                (DataChannelDescriptor) mediator.getProject().getRootNode());

        mediator.fireObjRelationshipDisplayEvent(rde);
    }

    /**
     * Fires events when a db rel was added
     */
    static void fireDbRelationshipEvent(Object src, ProjectController mediator, DbEntity dbEntity, DbRelationship rel) {

        mediator.fireDbRelationshipEvent(new RelationshipEvent(src, rel, dbEntity, MapEvent.ADD));

        RelationshipDisplayEvent rde = new RelationshipDisplayEvent(src, rel, dbEntity, mediator.getCurrentDataMap(),
                (DataChannelDescriptor) mediator.getProject().getRootNode());

        mediator.fireDbRelationshipDisplayEvent(rde);
    }

    @Override
    public String getIconName() {
        return "icon-relationship.png";
    }

    /**
     * @see org.apache.cayenne.modeler.util.CayenneAction#performAction(ActionEvent)
     */
    @Override
    public void performAction(ActionEvent e) {
        ObjEntity objEnt = getProjectController().getCurrentObjEntity();
        if (objEnt != null) {

            new ObjRelationshipInfo(getProjectController())
                    .createRelationship(objEnt)
                    .startupAction();

        } else {
            DbEntity dbEnt = getProjectController().getCurrentDbEntity();
            if (dbEnt != null) {

                new DbRelationshipDialog(getProjectController())
                        .createNewRelationship(dbEnt)
                        .startUp();
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

    public void createDbRelationship(DbEntity dbEntity, DbRelationship rel) {
        ProjectController mediator = getProjectController();
        dbEntity.addRelationship(rel);

        fireDbRelationshipEvent(this, mediator, dbEntity, rel);
    }

    /**
     * Returns <code>true</code> if path contains an Entity object.
     */
    @Override
    public boolean enableForPath(ConfigurationNode object) {
        if (object == null) {
            return false;
        }

        if (object instanceof Relationship) {
            return ((Relationship) object).getParent() != null && ((Relationship) object).getParent() instanceof Entity;
        }

        return false;
    }
}
