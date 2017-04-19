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

import java.util.Collection;

import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.map.Attribute;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.EmbeddableAttribute;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.event.AttributeEvent;
import org.apache.cayenne.map.event.EmbeddableAttributeEvent;
import org.apache.cayenne.map.event.MapEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.dialog.ConfirmRemoveDialog;
import org.apache.cayenne.modeler.undo.RemoveAttributeUndoableEdit;
import org.apache.cayenne.modeler.util.ProjectUtil;

/**
 * Removes currently selected attribute from either the DbEntity or ObjEntity.
 * 
 */
public class RemoveAttributeAction extends RemoveAction implements MultipleObjectsAction {

    

    private final static String ACTION_NAME = "Remove Attribute";

    /**
     * Name of action if multiple rels are selected
     */
    private final static String ACTION_NAME_MULTIPLE = "Remove Attributes";

    public static String getActionName() {
        return ACTION_NAME;
    }

    public String getActionName(boolean multiple) {
        return multiple ? ACTION_NAME_MULTIPLE : ACTION_NAME;
    }

    public RemoveAttributeAction(Application application) {
        super(ACTION_NAME, application);
    }

    /**
     * Returns <code>true</code> if last object in the path contains a removable
     * attribute.
     */
    @Override
    public boolean enableForPath(ConfigurationNode object) {
        if (object == null) {
            return false;
        }

        return object instanceof Attribute;
    }

    @Override
    public void performAction(ActionEvent e, boolean allowAsking) {
        ConfirmRemoveDialog dialog = getConfirmDeleteDialog(allowAsking);
        ProjectController mediator = getProjectController();

        EmbeddableAttribute[] embAttrs = getProjectController().getCurrentEmbAttributes();
        ObjAttribute[] objAttrs = getProjectController().getCurrentObjAttributes();
        DbAttribute[] dbAttrs = getProjectController().getCurrentDbAttributes();

        
        if (embAttrs != null && embAttrs.length > 0) {
            if ((embAttrs.length == 1 && dialog.shouldDelete(
                    "Embeddable Attribute",
                    embAttrs[0].getName()))
                    || (embAttrs.length > 1 && dialog
                            .shouldDelete("selected EmbAttributes"))) {

                Embeddable embeddable = mediator.getCurrentEmbeddable();

                application.getUndoManager().addEdit(
                        new RemoveAttributeUndoableEdit(embeddable, embAttrs));

                removeEmbeddableAttributes(embeddable, embAttrs);

            }
        }
        else if (objAttrs != null && objAttrs.length > 0) {
            if ((objAttrs.length == 1 && dialog.shouldDelete("ObjAttribute", objAttrs[0]
                    .getName()))
                    || (objAttrs.length > 1 && dialog.shouldDelete("selected ObjAttributes"))) {

                ObjEntity entity = mediator.getCurrentObjEntity();

                application.getUndoManager().addEdit(
                        new RemoveAttributeUndoableEdit(
                                (DataChannelDescriptor)mediator.getProject().getRootNode(),
                                mediator.getCurrentDataMap(),
                                entity,
                                objAttrs));

                removeObjAttributes(entity, objAttrs);
            }
        }
        else if (dbAttrs != null && dbAttrs.length > 0) {
        	if ((dbAttrs.length == 1 && dialog.shouldDelete("DbAttribute", dbAttrs[0]
        			.getName()))
                    || (dbAttrs.length > 1 && dialog.shouldDelete("selected DbAttributes"))) {

        		DbEntity entity = mediator.getCurrentDbEntity();

                application.getUndoManager().addEdit(
                		new RemoveAttributeUndoableEdit(
                				(DataChannelDescriptor)mediator.getProject().getRootNode(),
                                mediator.getCurrentDataMap(),
                                entity,
                                dbAttrs));

                removeDbAttributes(mediator.getCurrentDataMap(), entity, dbAttrs);
        	}
        }
    }

    public void removeDbAttributes(DataMap dataMap, DbEntity entity, DbAttribute[] attribs) {
        ProjectController mediator = getProjectController();

        for (DbAttribute attrib : attribs) {
            entity.removeAttribute(attrib.getName());

            AttributeEvent e = new AttributeEvent(
                    Application.getFrame(),
                    attrib,
                    entity,
                    MapEvent.REMOVE);

            mediator.fireDbAttributeEvent(e);
        }

        ProjectUtil.cleanObjMappings(dataMap);
    }

    public void removeObjAttributes(ObjEntity entity, ObjAttribute[] attribs) {
        ProjectController mediator = getProjectController();

        for (ObjAttribute attrib : attribs) {
            entity.removeAttribute(attrib.getName());
            AttributeEvent e = new AttributeEvent(
                    Application.getFrame(),
                    attrib,
                    entity,
                    MapEvent.REMOVE);
            mediator.fireObjAttributeEvent(e);

            Collection<ObjEntity> objEntities = ProjectUtil.getCollectionOfChildren((ObjEntity) e.getEntity());
            for (ObjEntity objEntity: objEntities) {
                objEntity.removeAttributeOverride(e.getAttribute().getName());
            }
        }
    }

    public void removeEmbeddableAttributes(
            Embeddable embeddable,
            EmbeddableAttribute[] attrs) {
        ProjectController mediator = getProjectController();

        for (EmbeddableAttribute attrib : attrs) {
            embeddable.removeAttribute(attrib.getName());
            EmbeddableAttributeEvent e = new EmbeddableAttributeEvent(Application
                    .getFrame(), attrib, embeddable, MapEvent.REMOVE);
            mediator.fireEmbeddableAttributeEvent(e);
        }
    }
}
