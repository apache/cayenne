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
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.dbsync.naming.NameBuilder;
import org.apache.cayenne.map.Attribute;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.EmbeddableAttribute;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.event.AttributeEvent;
import org.apache.cayenne.map.event.EmbeddableAttributeEvent;
import org.apache.cayenne.map.event.MapEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.event.AttributeDisplayEvent;
import org.apache.cayenne.modeler.event.EmbeddableAttributeDisplayEvent;
import org.apache.cayenne.modeler.undo.CreateAttributeUndoableEdit;
import org.apache.cayenne.modeler.undo.CreateEmbAttributeUndoableEdit;
import org.apache.cayenne.modeler.util.CayenneAction;

import java.awt.event.ActionEvent;


public class CreateAttributeAction extends CayenneAction {

    static void fireEmbeddableAttributeEvent(Object src, ProjectController controller, Embeddable embeddable, EmbeddableAttribute attr) {

        controller.fireEmbeddableAttributeEvent(new EmbeddableAttributeEvent(src, attr, embeddable, MapEvent.ADD));

        EmbeddableAttributeDisplayEvent e = new EmbeddableAttributeDisplayEvent(src, embeddable, attr,
                controller.getCurrentDataMap(), (DataChannelDescriptor) controller.getProject().getRootNode());

        controller.fireEmbeddableAttributeDisplayEvent(e);
    }

    /**
     * Fires events when an obj attribute was added
     */
    static void fireObjAttributeEvent(Object src, ProjectController controller, DataMap map, ObjEntity objEntity,
                                      ObjAttribute attr) {

        controller.fireObjAttributeEvent(new AttributeEvent(src, attr, objEntity, MapEvent.ADD));

        DataChannelDescriptor domain = (DataChannelDescriptor) controller.getProject().getRootNode();

        AttributeDisplayEvent ade = new AttributeDisplayEvent(src, attr, objEntity, map, domain);

        controller.fireObjAttributeDisplayEvent(ade);
    }

    /**
     * Fires events when a db attribute was added
     */
    static void fireDbAttributeEvent(Object src, ProjectController controller, DataMap map, DbEntity dbEntity,
                                     DbAttribute attr) {
        controller.fireDbAttributeEvent(new AttributeEvent(src, attr, dbEntity, MapEvent.ADD));

        AttributeDisplayEvent ade = new AttributeDisplayEvent(src, attr, dbEntity, map,
                (DataChannelDescriptor) controller.getProject().getRootNode());

        controller.fireDbAttributeDisplayEvent(ade);
    }

    public CreateAttributeAction(Application application) {
        super("Create Attribute", application);
    }

    @Override
    public String getIconName() {
        return "icon-attribute.png";
    }

    /**
     * Creates ObjAttribute, DbAttribute depending on context.
     */
    @Override
    public void performAction(ActionEvent e) {
        ProjectController controller = getProjectController();

        if (getProjectController().getCurrentEmbeddable() != null) {
            Embeddable embeddable = controller.getCurrentEmbeddable();

            EmbeddableAttribute attr = new EmbeddableAttribute();
            attr.setName(NameBuilder
                    .builder(attr, embeddable)
                    .name());

            createEmbAttribute(embeddable, attr);

            application.getUndoManager().addEdit(
                    new CreateEmbAttributeUndoableEdit(embeddable, new EmbeddableAttribute[]{attr}));
        }

        if (getProjectController().getCurrentObjEntity() != null) {

            ObjEntity objEntity = controller.getCurrentObjEntity();

            ObjAttribute attr = new ObjAttribute();
            attr.setName(NameBuilder.builder(attr, objEntity).name());

            createObjAttribute(controller.getCurrentDataMap(), objEntity, attr);

            application.getUndoManager().addEdit(
                    new CreateAttributeUndoableEdit((DataChannelDescriptor) controller.getProject().getRootNode(),
                            controller.getCurrentDataMap(), objEntity, attr));
        } else if (getProjectController().getCurrentDbEntity() != null) {
            DbEntity dbEntity = getProjectController().getCurrentDbEntity();

            DbAttribute attr = new DbAttribute();
            attr.setName(NameBuilder.builder(attr, dbEntity).name());
            attr.setType(TypesMapping.NOT_DEFINED);
            attr.setEntity(dbEntity);

            createDbAttribute(controller.getCurrentDataMap(), dbEntity, attr);

            application.getUndoManager().addEdit(
                    new CreateAttributeUndoableEdit((DataChannelDescriptor) controller.getProject().getRootNode(),
                            controller.getCurrentDataMap(), dbEntity, attr));
        }
    }

    public void createEmbAttribute(Embeddable embeddable, EmbeddableAttribute attr) {
        embeddable.addAttribute(attr);
        fireEmbeddableAttributeEvent(this, getProjectController(), embeddable, attr);
    }

    public void createObjAttribute(DataMap map, ObjEntity objEntity, ObjAttribute attr) {
        objEntity.addAttribute(attr);
        fireObjAttributeEvent(this, getProjectController(), map, objEntity, attr);
    }

    public void createDbAttribute(DataMap map, DbEntity dbEntity, DbAttribute attr) {
        dbEntity.addAttribute(attr);
        fireDbAttributeEvent(this, getProjectController(), map, dbEntity, attr);
    }

    /**
     * Returns <code>true</code> if path contains an Entity object.
     */
    @Override
    public boolean enableForPath(ConfigurationNode object) {
        if (object == null) {
            return false;
        }

        if (object instanceof Attribute) {
            return ((Attribute<?,?,?>) object).getParent() instanceof Entity;
        }

        return false;
    }
}
