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
import org.apache.cayenne.map.Attribute;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.EmbeddableAttribute;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.event.model.DbAttributeEvent;
import org.apache.cayenne.modeler.event.model.EmbeddableAttributeEvent;
import org.apache.cayenne.modeler.event.model.ObjAttributeEvent;
import org.apache.cayenne.modeler.project.DataMapOps;
import org.apache.cayenne.modeler.project.ObjEntityOps;
import org.apache.cayenne.modeler.ui.confirmremove.ConfirmRemoveDialog;
import org.apache.cayenne.modeler.ui.project.ProjectController;
import org.apache.cayenne.modeler.undo.RemoveAttributeUndoableEdit;

import java.awt.event.ActionEvent;
import java.util.Collection;

/**
 * Removes currently selected attribute from either the DbEntity or ObjEntity.
 */
public class RemoveAttributeAction extends RemoveAction implements MultipleObjectsAction {

    private final static String ACTION_NAME = "Remove Attribute";
    private final static String ACTION_NAME_MULTIPLE = "Remove Attributes";


    public RemoveAttributeAction(Application application) {
        super(ACTION_NAME, application);
    }

    @Override
    public String getActionName(boolean multiple) {
        return multiple ? ACTION_NAME_MULTIPLE : ACTION_NAME;
    }

    /**
     * Returns <code>true</code> if last object in the path contains a removable attribute.
     */
    @Override
    public boolean enableForPath(ConfigurationNode object) {
        return object instanceof Attribute;
    }

    @Override
    public void performAction(ActionEvent e, boolean allowAsking) {
        ConfirmRemoveDialog dialog = getConfirmDeleteDialog(allowAsking);
        ProjectController mediator = getProjectController();

        EmbeddableAttribute[] embAttrs = getProjectController().getSelectedEmbeddableAttributes();
        ObjAttribute[] objAttrs = getProjectController().getSelectedObjAttributes();
        DbAttribute[] dbAttrs = getProjectController().getSelectedDbAttributes();

        
        if (embAttrs != null && embAttrs.length > 0) {
            if ((embAttrs.length == 1 && dialog.shouldDelete(
                    "Embeddable Attribute",
                    embAttrs[0].getName()))
                    || (embAttrs.length > 1 && dialog
                            .shouldDelete("selected EmbAttributes"))) {

                Embeddable embeddable = mediator.getSelectedEmbeddable();

                application.getUndoManager().addEdit(
                        new RemoveAttributeUndoableEdit(mediator,embeddable, embAttrs));

                removeEmbeddableAttributes(embeddable, embAttrs);

            }
        } else if (objAttrs != null && objAttrs.length > 0) {
            if ((objAttrs.length == 1 && dialog.shouldDelete("ObjAttribute", objAttrs[0]
                    .getName()))
                    || (objAttrs.length > 1 && dialog.shouldDelete("selected ObjAttributes"))) {

                ObjEntity entity = mediator.getSelectedObjEntity();

                application.getUndoManager().addEdit(new RemoveAttributeUndoableEdit(mediator,entity, objAttrs));

                removeObjAttributes(entity, objAttrs);
            }
        } else if (dbAttrs != null && dbAttrs.length > 0) {
        	if ((dbAttrs.length == 1 && dialog.shouldDelete("DbAttribute", dbAttrs[0]
        			.getName()))
                    || (dbAttrs.length > 1 && dialog.shouldDelete("selected DbAttributes"))) {

        		DbEntity entity = mediator.getSelectedDbEntity();

                application.getUndoManager().addEdit(new RemoveAttributeUndoableEdit(mediator,entity, dbAttrs));

                removeDbAttributes(mediator.getSelectedDataMap(), entity, dbAttrs);
        	}
        }
    }

    public void removeDbAttributes(DataMap dataMap, DbEntity entity, DbAttribute[] attribs) {
        ProjectController mediator = getProjectController();

        for (DbAttribute attrib : attribs) {
            entity.removeAttribute(attrib.getName());

            DbAttributeEvent e = DbAttributeEvent.ofRemove(
                    application.getFrameController().getView(),
                    attrib,
                    entity);

            mediator.fireDbAttributeEvent(e);
        }

        DataMapOps.removeBrokenObjToDbMappings(dataMap);
    }

    public void removeObjAttributes(ObjEntity entity, ObjAttribute[] attribs) {
        ProjectController mediator = getProjectController();

        for (ObjAttribute attrib : attribs) {
            entity.removeAttribute(attrib.getName());
            ObjAttributeEvent e = ObjAttributeEvent.ofRemove(
                    application.getFrameController().getView(),
                    attrib,
                    entity);
            mediator.fireObjAttributeEvent(e);

            Collection<ObjEntity> objEntities = ObjEntityOps.subentities(e.getEntity());
            for (ObjEntity objEntity: objEntities) {
                objEntity.removeAttributeOverride(e.getAttribute().getName());
            }
        }
    }

    public void removeEmbeddableAttributes(Embeddable embeddable, EmbeddableAttribute[] attrs) {
        ProjectController mediator = getProjectController();

        for (EmbeddableAttribute attrib : attrs) {
            embeddable.removeAttribute(attrib.getName());
            EmbeddableAttributeEvent e = EmbeddableAttributeEvent.ofRemove(application.getFrameController().getView(), attrib, embeddable);
            mediator.fireEmbeddableAttributeEvent(e);
        }
    }
}
