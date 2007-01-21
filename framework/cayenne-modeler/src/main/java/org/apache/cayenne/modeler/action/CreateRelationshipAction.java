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
import org.apache.cayenne.modeler.util.CayenneAction;
import org.apache.cayenne.project.NamedObjectFactory;
import org.apache.cayenne.project.ProjectPath;

/**
 * @author Andrus Adamchik
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

    public String getIconName() {
        return "icon-relationship.gif";
    }

    /**
     * @see org.apache.cayenne.modeler.util.CayenneAction#performAction(ActionEvent)
     */
    public void performAction(ActionEvent e) {
        ObjEntity objEnt = getProjectController().getCurrentObjEntity();
        if (objEnt != null) {
            createObjRelationship(objEnt);
        } else {
            DbEntity dbEnt = getProjectController().getCurrentDbEntity();
            if (dbEnt != null) {
                createDbRelationship(dbEnt);
            }
        }
    }

    public void createObjRelationship(ObjEntity objEnt) {
        ProjectController mediator = getProjectController();

        ObjRelationship rel =
            (ObjRelationship) NamedObjectFactory.createObject(
                ObjRelationship.class,
                objEnt);
        rel.setSourceEntity(objEnt);
        objEnt.addRelationship(rel);

        mediator.fireObjRelationshipEvent(
            new RelationshipEvent(this, rel, objEnt, MapEvent.ADD));
        
        RelationshipDisplayEvent rde = new RelationshipDisplayEvent(
                this,
                rel,
                objEnt,
                mediator.getCurrentDataMap(),
                mediator.getCurrentDataDomain());
        
        mediator.fireObjRelationshipDisplayEvent(rde);
    }

    public void createDbRelationship(DbEntity dbEnt) {
        ProjectController mediator = getProjectController();

        DbRelationship rel =
            (DbRelationship) NamedObjectFactory.createObject(DbRelationship.class, dbEnt);

        rel.setSourceEntity(dbEnt);
        dbEnt.addRelationship(rel);

        mediator.fireDbRelationshipEvent(
            new RelationshipEvent(this, rel, dbEnt, MapEvent.ADD));

        RelationshipDisplayEvent rde = new RelationshipDisplayEvent(
                this,
                rel,
                dbEnt,
                mediator.getCurrentDataMap(),
                mediator.getCurrentDataDomain());
        
        mediator.fireDbRelationshipDisplayEvent(rde);
    }

    /**
    * Returns <code>true</code> if path contains an Entity object.
    */
    public boolean enableForPath(ProjectPath path) {
        if (path == null) {
            return false;
        }

        return path.firstInstanceOf(Entity.class) != null;
    }
}
