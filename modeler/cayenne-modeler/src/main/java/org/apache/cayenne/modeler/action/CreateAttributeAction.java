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

/**
 */
public class CreateAttributeAction extends CayenneAction {

    /**
     * Constructor for CreateAttributeAction.
     */
    public CreateAttributeAction(Application application) {
        super(getActionName(), application);
    }

    public static String getActionName() {
        return "Create Attribute";
    }

    static void fireEmbeddableAttributeEvent(Object src, ProjectController mediator, Embeddable embeddable,
                                             EmbeddableAttribute attr) {

        mediator.fireEmbeddableAttributeEvent(new EmbeddableAttributeEvent(src, attr, embeddable, MapEvent.ADD));

        EmbeddableAttributeDisplayEvent e = new EmbeddableAttributeDisplayEvent(src, embeddable, attr,
                mediator.getCurrentDataMap(), (DataChannelDescriptor) mediator.getProject().getRootNode());

        mediator.fireEmbeddableAttributeDisplayEvent(e);
    }

    /**
     * Fires events when an obj attribute was added
     */
    static void fireObjAttributeEvent(Object src, ProjectController mediator, DataMap map, ObjEntity objEntity,
                                      ObjAttribute attr) {

        mediator.fireObjAttributeEvent(new AttributeEvent(src, attr, objEntity, MapEvent.ADD));

        DataChannelDescriptor domain = (DataChannelDescriptor) mediator.getProject().getRootNode();

        AttributeDisplayEvent ade = new AttributeDisplayEvent(src, attr, objEntity, map, domain);

        mediator.fireObjAttributeDisplayEvent(ade);
    }

    /**
     * Fires events when a db attribute was added
     */
    static void fireDbAttributeEvent(Object src, ProjectController mediator, DataMap map, DbEntity dbEntity,
                                     DbAttribute attr) {
        mediator.fireDbAttributeEvent(new AttributeEvent(src, attr, dbEntity, MapEvent.ADD));

        AttributeDisplayEvent ade = new AttributeDisplayEvent(src, attr, dbEntity, map,
                (DataChannelDescriptor) mediator.getProject().getRootNode());

        mediator.fireDbAttributeDisplayEvent(ade);
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
        ProjectController mediator = getProjectController();

        if (getProjectController().getCurrentEmbeddable() != null) {
            Embeddable embeddable = mediator.getCurrentEmbeddable();

            EmbeddableAttribute attr = new EmbeddableAttribute();
            attr.setName(NameBuilder
                    .builder(attr, embeddable)
                    .name());

            createEmbAttribute(embeddable, attr);

            application.getUndoManager().addEdit(
                    new CreateEmbAttributeUndoableEdit(embeddable, new EmbeddableAttribute[]{attr}));
        }

        if (getProjectController().getCurrentObjEntity() != null) {

            ObjEntity objEntity = mediator.getCurrentObjEntity();

            ObjAttribute attr = new ObjAttribute();
            attr.setName(NameBuilder.builder(attr, objEntity).name());

            createObjAttribute(mediator.getCurrentDataMap(), objEntity, attr);

            application.getUndoManager().addEdit(
                    new CreateAttributeUndoableEdit((DataChannelDescriptor) mediator.getProject().getRootNode(),
                            mediator.getCurrentDataMap(), objEntity, attr));
        } else if (getProjectController().getCurrentDbEntity() != null) {
            DbEntity dbEntity = getProjectController().getCurrentDbEntity();

            DbAttribute attr = new DbAttribute();
            attr.setName(NameBuilder.builder(attr, dbEntity).name());
            attr.setType(TypesMapping.NOT_DEFINED);
            attr.setEntity(dbEntity);

            createDbAttribute(mediator.getCurrentDataMap(), dbEntity, attr);

            application.getUndoManager().addEdit(
                    new CreateAttributeUndoableEdit((DataChannelDescriptor) mediator.getProject().getRootNode(),
                            mediator.getCurrentDataMap(), dbEntity, attr));
        }
    }

    public void createEmbAttribute(Embeddable embeddable, EmbeddableAttribute attr) {
        ProjectController mediator = getProjectController();
        embeddable.addAttribute(attr);
        fireEmbeddableAttributeEvent(this, mediator, embeddable, attr);
    }

    public void createObjAttribute(DataMap map, ObjEntity objEntity, ObjAttribute attr) {

        ProjectController mediator = getProjectController();
        objEntity.addAttribute(attr);
        fireObjAttributeEvent(this, mediator, map, objEntity, attr);
    }

    public void createDbAttribute(DataMap map, DbEntity dbEntity, DbAttribute attr) {
        dbEntity.addAttribute(attr);
        ProjectController mediator = getProjectController();
        fireDbAttributeEvent(this, mediator, map, dbEntity, attr);
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
            return ((Attribute) object).getParent() != null && ((Attribute) object).getParent() instanceof Entity;
        }

        return false;
    }
}
