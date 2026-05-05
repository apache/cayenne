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
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.map.Relationship;
import org.apache.cayenne.modeler.event.model.ObjRelationshipEvent;
import org.apache.cayenne.modeler.event.model.DbRelationshipEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.project.ProjectSession;
import org.apache.cayenne.modeler.ui.dbrelationship.DbRelationshipDialog;
import org.apache.cayenne.modeler.ui.project.editor.objentity.relinfo.ObjRelationshipInfoDialog;
import org.apache.cayenne.modeler.event.display.DbRelationshipDisplayEvent;
import org.apache.cayenne.modeler.event.display.ObjRelationshipDisplayEvent;
import org.apache.cayenne.util.DeleteRuleUpdater;

import java.awt.event.ActionEvent;

public class CreateRelationshipAction extends ModelerAbstractAction {

    /**
     * Fires events when a obj rel was added
     */
    static void fireObjRelationshipEvent(Object src, ProjectSession session, ObjEntity objEntity,
                                         ObjRelationship rel) {

        session.fireObjRelationshipEvent(ObjRelationshipEvent.ofAdd(src, rel, objEntity));

        DataChannelDescriptor domain = (DataChannelDescriptor) session.project().getRootNode();
        ObjRelationshipDisplayEvent rde = new ObjRelationshipDisplayEvent(src, domain, session.getSelectedDataMap(), objEntity, rel);

        session.displayObjRelationship(rde);
    }

    /**
     * Fires events when a db rel was added
     */
    static void fireDbRelationshipEvent(Object src, ProjectSession session, DbEntity dbEntity, DbRelationship rel) {

        session.fireDbRelationshipEvent(DbRelationshipEvent.ofAdd(src, rel, dbEntity));

        DataChannelDescriptor domain = (DataChannelDescriptor) session.project().getRootNode();
        DbRelationshipDisplayEvent rde = new DbRelationshipDisplayEvent(src, domain, session.getSelectedDataMap(), dbEntity, rel);

        session.displayDbRelationship(rde);
    }

    public CreateRelationshipAction(Application application) {
        super("Create Relationship", application);
    }

    @Override
    public String getIconName() {
        return "icon-relationship.png";
    }

    /**
     * @see ModelerAbstractAction#performAction(ActionEvent)
     */
    @Override
    public void performAction(ActionEvent e) {
        ObjEntity objEnt = getProjectSession().getSelectedObjEntity();
        if (objEnt != null) {

            new ObjRelationshipInfoDialog(getProjectSession(), app.getFrame())
                    .createRelationship(objEnt)
                    .open();

        } else {
            DbEntity dbEnt = getProjectSession().getSelectedDbEntity();
            if (dbEnt != null) {

                new DbRelationshipDialog(getProjectSession(), app.getFrame())
                        .createNewRelationship(dbEnt)
                        .open();
            }
        }
    }

    public void createObjRelationship(ObjEntity objEntity, ObjRelationship rel) {
        ProjectSession session = getProjectSession();

        rel.setSourceEntity(objEntity);
        DeleteRuleUpdater.updateObjRelationship(rel);

        objEntity.addRelationship(rel);
        fireObjRelationshipEvent(this, session, objEntity, rel);
    }

    public void createDbRelationship(DbEntity dbEntity, DbRelationship rel) {
        ProjectSession session = getProjectSession();
        dbEntity.addRelationship(rel);

        fireDbRelationshipEvent(this, session, dbEntity, rel);
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
            return ((Relationship<?,?,?>) object).getParent() instanceof Entity;
        }

        return false;
    }
}
