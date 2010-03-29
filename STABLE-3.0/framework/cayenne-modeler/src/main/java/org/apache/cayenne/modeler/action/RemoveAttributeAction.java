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
import org.apache.cayenne.project.ProjectPath;

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
    public boolean enableForPath(ProjectPath path) {
        if (path == null) {
            return false;
        }

        return path.getObject() instanceof Attribute;
    }

    @Override
    public void performAction(ActionEvent e, boolean allowAsking) {
        ConfirmRemoveDialog dialog = getConfirmDeleteDialog(allowAsking);
        ProjectController mediator = getProjectController();

        EmbeddableAttribute[] embAttrs = getProjectController().getCurrentEmbAttrs();

        ObjAttribute[] attrs = getProjectController().getCurrentObjAttributes();

        if (embAttrs != null && embAttrs.length > 0) {
            if ((embAttrs.length == 1 && dialog.shouldDelete(
                    "Embeddable Attribute",
                    embAttrs[0].getName()))
                    || (embAttrs.length > 1 && dialog
                            .shouldDelete("selected EmbAttributes"))) {

                Embeddable embeddable = mediator.getCurrentEmbeddable();

                EmbeddableAttribute[] eAttrs = getProjectController()
                        .getCurrentEmbAttrs();

                application.getUndoManager().addEdit(
                        new RemoveAttributeUndoableEdit(embeddable, eAttrs));

                removeEmbeddableAttributes(embeddable, eAttrs);

            }
        }
        else if (attrs != null && attrs.length > 0) {
            if ((attrs.length == 1 && dialog.shouldDelete("ObjAttribute", attrs[0]
                    .getName()))
                    || (attrs.length > 1 && dialog.shouldDelete("selected ObjAttributes"))) {

                ObjEntity entity = mediator.getCurrentObjEntity();
                ObjAttribute[] attribs = mediator.getCurrentObjAttributes();

                application.getUndoManager().addEdit(
                        new RemoveAttributeUndoableEdit(
                                mediator.getCurrentDataDomain(),
                                mediator.getCurrentDataMap(),
                                entity,
                                attribs));

                removeObjAttributes(entity, attribs);
            }
        }
        else {
            DbAttribute[] dbAttrs = getProjectController().getCurrentDbAttributes();
            if (dbAttrs != null && dbAttrs.length > 0) {
                if ((dbAttrs.length == 1 && dialog.shouldDelete("DbAttribute", dbAttrs[0]
                        .getName()))
                        || (dbAttrs.length > 1 && dialog
                                .shouldDelete("selected DbAttributes"))) {

                    DbEntity entity = mediator.getCurrentDbEntity();
                    DbAttribute[] attribs = mediator.getCurrentDbAttributes();

                    ProjectPath[] paths = getProjectController().getCurrentPaths();

                    application.getUndoManager().addEdit(
                            new RemoveAttributeUndoableEdit(
                                    mediator.getCurrentDataDomain(),
                                    mediator.getCurrentDataMap(),
                                    entity,
                                    attribs));

                    removeDbAttributes(mediator.getCurrentDataMap(), entity, attribs);
                }
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
