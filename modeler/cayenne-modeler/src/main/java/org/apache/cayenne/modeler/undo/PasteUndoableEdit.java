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
package org.apache.cayenne.modeler.undo;

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.EmbeddableAttribute;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.ProcedureParameter;
import org.apache.cayenne.map.QueryDescriptor;
import org.apache.cayenne.modeler.project.ProjectSession;
import org.apache.cayenne.modeler.ui.action.PasteAction;
import org.apache.cayenne.modeler.ui.action.RemoveAction;
import org.apache.cayenne.modeler.ui.action.RemoveAttributeAction;
import org.apache.cayenne.modeler.ui.action.RemoveCallbackMethodAction;
import org.apache.cayenne.modeler.ui.action.RemoveProcedureParameterAction;
import org.apache.cayenne.modeler.ui.action.RemoveRelationshipAction;
import org.apache.cayenne.modeler.ui.project.editor.objentity.callbacks.ObjCallbackMethod;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

public class PasteUndoableEdit extends CayenneUndoableEdit {

    private final DataChannelDescriptor domain;
    private final DataMap map;
    private final Object where;
    private final Object content;

    public PasteUndoableEdit(
            ProjectSession session, DataChannelDescriptor domain, DataMap map, Object where, Object content) {
        super(session);
        this.domain = domain;
        this.map = map;
        this.where = where;
        this.content = content;
    }

    @Override
    public String getPresentationName() {

        String className = this.content.getClass().getName();
        int pos = className.lastIndexOf(".");
        String contentName = className.substring(pos + 1);

        return "Paste " + contentName;
    }

    @Override
    public void redo() throws CannotRedoException {
        PasteAction action = globalActions.getAction(PasteAction.class);

        action.paste(where, content, domain);
    }

    @Override
    public void undo() throws CannotUndoException {
        RemoveAttributeAction rAttributeAction = globalActions
                .getAction(RemoveAttributeAction.class);

        RemoveAction rAction = globalActions.getAction(RemoveAction.class);

        RemoveRelationshipAction rRelationShipAction = globalActions
                .getAction(RemoveRelationshipAction.class);

        RemoveCallbackMethodAction rCallbackMethodAction = globalActions
                .getAction(RemoveCallbackMethodAction.class);

        RemoveProcedureParameterAction rProcedureParamAction = globalActions
                .getAction(RemoveProcedureParameterAction.class);

        if (content instanceof DataMap dataMap) {
            if (where instanceof DataChannelDescriptor) {
                rAction.removeDataMap(dataMap);
            } else if (where instanceof DataNodeDescriptor whereNode) {
                rAction.removeDataMapFromDataNode(whereNode, dataMap);
            }
        } else if (where instanceof DataMap) {
            if (content instanceof DbEntity dbEntity) {
                rAction.removeDbEntity(map, dbEntity);
            } else if (content instanceof ObjEntity objEntity) {
                rAction.removeObjEntity(map, objEntity);
            } else if (content instanceof Embeddable embeddable) {
                rAction.removeEmbeddable(map, embeddable);
            } else if (content instanceof QueryDescriptor queryDescriptor) {
                rAction.removeQuery(map, queryDescriptor);
            } else if (content instanceof Procedure proc) {
                rAction.removeProcedure(map, proc);
            }
        } else if (where instanceof DbEntity dbEntityWhere) {
            if (content instanceof DbEntity dbEntity) {
                rAction.removeDbEntity(map, dbEntity);
            } else if (content instanceof DbAttribute dbAttribute) {
                rAttributeAction.removeDbAttributes(
                        map,
                        dbEntityWhere,
                        new DbAttribute[] { dbAttribute });
            } else if (content instanceof DbRelationship dbRelationship) {
                rRelationShipAction.removeDbRelationships(
                        dbEntityWhere,
                        new DbRelationship[] { dbRelationship });
            }
        } else if (where instanceof ObjEntity objEntityWhere) {
            if (content instanceof ObjEntity objEntity) {
                rAction.removeObjEntity(map, objEntity);
            } else if (content instanceof ObjAttribute objAttribute) {
                rAttributeAction.removeObjAttributes(
                        objEntityWhere,
                        new ObjAttribute[] { objAttribute });
            } else if (content instanceof ObjRelationship objRelationship) {
                rRelationShipAction.removeObjRelationships(
                        objEntityWhere,
                        new ObjRelationship[] { objRelationship });
            } else if (content instanceof ObjCallbackMethod callbackMethod) {
                rCallbackMethodAction.removeCallbackMethod(
                        callbackMethod.getCallbackType(),
                        callbackMethod.getName());
            }
        } else if (where instanceof Procedure whereProcedure) {
            if (content instanceof ProcedureParameter param) {
                rProcedureParamAction.removeProcedureParameters(
                        whereProcedure,
                        new ProcedureParameter[] { param });
            }
        } else if (where instanceof Embeddable embeddableWhere) {
            if (content instanceof Embeddable embeddable) {
                rAction.removeEmbeddable(map, embeddable);
            } else if (content instanceof EmbeddableAttribute embeddableAttribute) {
                rAttributeAction.removeEmbeddableAttributes(
                        embeddableWhere,
                        new EmbeddableAttribute[]{ embeddableAttribute });
            }
        }
    }
}
