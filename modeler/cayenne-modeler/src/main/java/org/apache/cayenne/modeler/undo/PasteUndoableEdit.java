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
package org.apache.cayenne.modeler.undo;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.ProcedureParameter;
import org.apache.cayenne.modeler.action.PasteAction;
import org.apache.cayenne.modeler.action.RemoveAction;
import org.apache.cayenne.modeler.action.RemoveAttributeAction;
import org.apache.cayenne.modeler.action.RemoveCallbackMethodAction;
import org.apache.cayenne.modeler.action.RemoveProcedureParameterAction;
import org.apache.cayenne.modeler.action.RemoveRelationshipAction;
import org.apache.cayenne.modeler.editor.ObjCallbackMethod;
import org.apache.cayenne.map.QueryDescriptor;

public class PasteUndoableEdit extends CayenneUndoableEdit {

    private DataChannelDescriptor domain;
    private DataMap map;
    private Object where;
    private Object content;

    public PasteUndoableEdit(DataChannelDescriptor domain, DataMap map, Object where,
            Object content) {
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
        PasteAction action = actionManager.getAction(PasteAction.class);

        action.paste(where, content, domain, map);
    }

    @Override
    public void undo() throws CannotUndoException {
        RemoveAttributeAction rAttributeAction = actionManager
                .getAction(RemoveAttributeAction.class);

        RemoveAction rAction = actionManager.getAction(RemoveAction.class);

        RemoveRelationshipAction rRelationShipAction = actionManager
                .getAction(RemoveRelationshipAction.class);

        RemoveCallbackMethodAction rCallbackMethodAction = actionManager
                .getAction(RemoveCallbackMethodAction.class);

        RemoveProcedureParameterAction rProcedureParamAction = actionManager
                .getAction(RemoveProcedureParameterAction.class);

        if (content instanceof DataMap) {
            if (where instanceof DataChannelDescriptor) {
                rAction.removeDataMap((DataMap) content);
            }
            else if (where instanceof DataNodeDescriptor) {
                rAction.removeDataMapFromDataNode(
                        (DataNodeDescriptor) where,
                        (DataMap) content);
            }
        }
        else if (where instanceof DataMap) {
            if (content instanceof DbEntity) {
                rAction.removeDbEntity(map, (DbEntity) content);
            }
            else if (content instanceof ObjEntity) {
                rAction.removeObjEntity(map, (ObjEntity) content);
            }
            else if (content instanceof Embeddable) {
                rAction.removeEmbeddable(map, (Embeddable) content);
            }
            else if (content instanceof QueryDescriptor) {
                rAction.removeQuery(map, (QueryDescriptor) content);
            }
            else if (content instanceof Procedure) {
                rAction.removeProcedure(map, (Procedure) content);
            }
        }
        else if (where instanceof DbEntity) {
            if (content instanceof DbEntity) {
                rAction.removeDbEntity(map, (DbEntity) content);
            }
            else if (content instanceof DbAttribute) {
                rAttributeAction.removeDbAttributes(
                        map,
                        (DbEntity) where,
                        new DbAttribute[] {
                            (DbAttribute) content
                        });
            }
            else if (content instanceof DbRelationship) {
                rRelationShipAction.removeDbRelationships(
                        (DbEntity) where,
                        new DbRelationship[] {
                            (DbRelationship) content
                        });
            }
        }
        else if (where instanceof ObjEntity) {
            if (content instanceof ObjEntity) {
                rAction.removeObjEntity(map, (ObjEntity) content);
            }
            else if (content instanceof ObjAttribute) {
                rAttributeAction.removeObjAttributes(
                        (ObjEntity) where,
                        new ObjAttribute[] {
                            (ObjAttribute) content
                        });
            }
            else if (content instanceof ObjRelationship) {
                rRelationShipAction.removeObjRelationships(
                        (ObjEntity) where,
                        new ObjRelationship[] {
                            (ObjRelationship) content
                        });
            }
            else if (content instanceof ObjCallbackMethod) {
            		ObjCallbackMethod[] methods = new ObjCallbackMethod[] {
                            (ObjCallbackMethod) content };
            		for(ObjCallbackMethod callbackMethod : methods) {
	            		rCallbackMethodAction.removeCallbackMethod(
	                			methods[0].getCallbackType(), 
	                			callbackMethod.getName());
            		}
            }
        }
        else if (where instanceof Procedure) {
            final Procedure procedure = (Procedure) where;
            if (content instanceof ProcedureParameter) {
                rProcedureParamAction.removeProcedureParameters(
                        procedure,
                        new ProcedureParameter[] {
                            (ProcedureParameter) content
                        });
            }
        }
        else if (content instanceof Embeddable) {
            rAction.removeEmbeddable(map, (Embeddable) content);
        }
    }
}
