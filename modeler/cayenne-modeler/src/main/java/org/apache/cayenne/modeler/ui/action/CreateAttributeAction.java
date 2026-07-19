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
import org.apache.cayenne.modeler.event.model.ObjAttributeEvent;
import org.apache.cayenne.modeler.event.model.DbAttributeEvent;
import org.apache.cayenne.modeler.event.model.EmbeddableAttributeEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.project.ProjectSession;
import org.apache.cayenne.modeler.event.display.DbAttributeDisplayEvent;
import org.apache.cayenne.modeler.event.display.EmbeddableAttributeDisplayEvent;
import org.apache.cayenne.modeler.event.display.ObjAttributeDisplayEvent;
import org.apache.cayenne.modeler.toolkit.AppAction;
import org.apache.cayenne.modeler.undo.CreateDbAttributeUndoableEdit;
import org.apache.cayenne.modeler.undo.CreateObjAttributeUndoableEdit;
import org.apache.cayenne.modeler.undo.CreateEmbAttributeUndoableEdit;

import java.awt.event.ActionEvent;


public class CreateAttributeAction extends AppAction {

    static void fireEmbeddableAttributeEvent(Object src, ProjectSession session, Embeddable embeddable, EmbeddableAttribute attr) {

        session.fireEmbeddableAttributeEvent(EmbeddableAttributeEvent.ofAdd(src, attr, embeddable));

        DataChannelDescriptor domain = (DataChannelDescriptor) session.project().getRootNode();
        EmbeddableAttributeDisplayEvent e = new EmbeddableAttributeDisplayEvent(
                src, domain, session.getSelectedDataMap(), embeddable, attr);

        session.displayEmbeddableAttribute(e);
    }

    /**
     * Fires events when an obj attribute was added
     */
    static void fireObjAttributeEvent(Object src, ProjectSession session, DataMap map, ObjEntity objEntity,
                                      ObjAttribute attr) {

        session.fireObjAttributeEvent(ObjAttributeEvent.ofAdd(src, attr, objEntity));

        DataChannelDescriptor domain = (DataChannelDescriptor) session.project().getRootNode();

        ObjAttributeDisplayEvent ade = new ObjAttributeDisplayEvent(src, domain, map, objEntity, attr);

        session.displayObjAttribute(ade);
    }

    /**
     * Fires events when a db attribute was added
     */
    static void fireDbAttributeEvent(Object src, ProjectSession session, DataMap map, DbEntity dbEntity,
                                     DbAttribute attr) {
        session.fireDbAttributeEvent(DbAttributeEvent.ofAdd(src, attr, dbEntity));

        DataChannelDescriptor domain = (DataChannelDescriptor) session.project().getRootNode();
        DbAttributeDisplayEvent ade = new DbAttributeDisplayEvent(src, domain, map, dbEntity, attr);

        session.displayDbAttribute(ade);
    }

    public CreateAttributeAction(Application application) {
        super(application, "Create Attribute");
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
        ProjectSession session = getProjectSession();

        if (getProjectSession().getSelectedEmbeddable() != null) {
            Embeddable embeddable = session.getSelectedEmbeddable();

            EmbeddableAttribute attr = new EmbeddableAttribute();
            attr.setName(NameBuilder
                    .of(attr).parent(embeddable)
                    .name());

            createEmbAttribute(embeddable, attr);

            app.getUndoManager().addEdit(
                    new CreateEmbAttributeUndoableEdit(getProjectSession(), embeddable, new EmbeddableAttribute[]{attr}));
        }

        if (getProjectSession().getSelectedObjEntity() != null) {

            ObjEntity objEntity = session.getSelectedObjEntity();

            ObjAttribute attr = new ObjAttribute();
            attr.setName(NameBuilder.of(attr).parent(objEntity).name());

            createObjAttribute(session.getSelectedDataMap(), objEntity, attr);

            app.getUndoManager().addEdit(
                    new CreateObjAttributeUndoableEdit(session, (DataChannelDescriptor) session.project().getRootNode(),
                            session.getSelectedDataMap(), objEntity, attr));
        } else if (getProjectSession().getSelectedDbEntity() != null) {
            DbEntity dbEntity = getProjectSession().getSelectedDbEntity();

            DbAttribute attr = new DbAttribute();
            attr.setName(NameBuilder.of(attr).parent(dbEntity).name());
            attr.setType(TypesMapping.NOT_DEFINED);
            attr.setEntity(dbEntity);

            createDbAttribute(session.getSelectedDataMap(), dbEntity, attr);

            app.getUndoManager().addEdit(
                    new CreateDbAttributeUndoableEdit(session, (DataChannelDescriptor) session.project().getRootNode(),
                            session.getSelectedDataMap(), dbEntity, attr));
        }
    }

    public void createEmbAttribute(Embeddable embeddable, EmbeddableAttribute attr) {
        embeddable.addAttribute(attr);
        fireEmbeddableAttributeEvent(this, getProjectSession(), embeddable, attr);
    }

    public void createObjAttribute(DataMap map, ObjEntity objEntity, ObjAttribute attr) {
        objEntity.addAttribute(attr);
        fireObjAttributeEvent(this, getProjectSession(), map, objEntity, attr);
    }

    public void createDbAttribute(DataMap map, DbEntity dbEntity, DbAttribute attr) {
        dbEntity.addAttribute(attr);
        fireDbAttributeEvent(this, getProjectSession(), map, dbEntity, attr);
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
