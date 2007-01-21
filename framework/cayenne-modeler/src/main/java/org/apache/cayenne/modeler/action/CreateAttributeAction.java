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

import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DerivedDbAttribute;
import org.apache.cayenne.map.DerivedDbEntity;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.event.AttributeEvent;
import org.apache.cayenne.map.event.MapEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.event.AttributeDisplayEvent;
import org.apache.cayenne.modeler.util.CayenneAction;
import org.apache.cayenne.project.NamedObjectFactory;
import org.apache.cayenne.project.ProjectPath;

/**
 * @author Andrus Adamchik
 */
public class CreateAttributeAction extends CayenneAction {

    public static String getActionName() {
    	return "Create Attribute";
    }

    /**
     * Constructor for CreateAttributeAction.
     * @param name
     */
    public CreateAttributeAction(Application application) {
        super(getActionName(), application);
    }

    public String getIconName() {
        return "icon-attribute.gif";
    }

    /**
     * Creates ObjAttribute, DbAttribute depending on context.
     */
    public void performAction(ActionEvent e) {
        if (getProjectController().getCurrentObjEntity() != null) {
            createObjAttribute();
        }
        else if (getProjectController().getCurrentDbEntity() != null) {
            createDbAttribute();
        }
    }

    public void createObjAttribute() {
        ProjectController mediator = getProjectController();
        ObjEntity objEntity = mediator.getCurrentObjEntity();

        ObjAttribute attr = (ObjAttribute) NamedObjectFactory.createObject(
                ObjAttribute.class,
                objEntity);
        objEntity.addAttribute(attr);

        mediator.fireObjAttributeEvent(new AttributeEvent(
                this,
                attr,
                objEntity,
                MapEvent.ADD));

        AttributeDisplayEvent ade = new AttributeDisplayEvent(
                this,
                attr,
                objEntity,
                mediator.getCurrentDataMap(),
                mediator.getCurrentDataDomain());

        mediator.fireObjAttributeDisplayEvent(ade);
    }

    public void createDbAttribute() {
        Class attrClass = null;

        DbEntity dbEntity = getProjectController().getCurrentDbEntity();
        if (dbEntity instanceof DerivedDbEntity) {
            if (((DerivedDbEntity) dbEntity).getParentEntity() == null) {
                return;
            }
            attrClass = DerivedDbAttribute.class;
        }
        else {
            attrClass = DbAttribute.class;
        }

        DbAttribute attr =
            (DbAttribute) NamedObjectFactory.createObject(attrClass, dbEntity);
        dbEntity.addAttribute(attr);

        ProjectController mediator = getProjectController();
        mediator.fireDbAttributeEvent(
            new AttributeEvent(this, attr, dbEntity, MapEvent.ADD));

        AttributeDisplayEvent ade = new AttributeDisplayEvent(
                this,
                attr,
                dbEntity,
                mediator.getCurrentDataMap(),
                mediator.getCurrentDataDomain());

        mediator.fireDbAttributeDisplayEvent(ade);
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
