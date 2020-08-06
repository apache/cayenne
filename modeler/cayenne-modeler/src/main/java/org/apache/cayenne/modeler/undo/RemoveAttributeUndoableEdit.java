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

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.EmbeddableAttribute;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.modeler.action.CreateAttributeAction;
import org.apache.cayenne.modeler.action.RemoveAttributeAction;

public class RemoveAttributeUndoableEdit extends BaseRemovePropertyUndoableEdit {

    private DbAttribute[] dbAttributes;
    private ObjAttribute[] objAttributes;
    private EmbeddableAttribute[] embeddableAttrs;

    public RemoveAttributeUndoableEdit(Embeddable embeddable, EmbeddableAttribute[] embeddableAttributes) {
        super();
        this.embeddable = embeddable;
        this.embeddableAttrs = embeddableAttributes;
    }

    public RemoveAttributeUndoableEdit(ObjEntity entity, ObjAttribute[] objAttributes) {
        this.objEntity = entity;
        this.objAttributes = objAttributes;
    }

    public RemoveAttributeUndoableEdit(DbEntity entity, DbAttribute[] dbAttributes) {
        this.dbEntity = entity;
        this.dbAttributes = dbAttributes;
    }

    @Override
    public void redo() throws CannotRedoException {
        RemoveAttributeAction action = actionManager.getAction(RemoveAttributeAction.class);

        if (objEntity != null) {
            action.removeObjAttributes(objEntity, objAttributes);
            focusObjEntity();
        }

        if (dbEntity != null) {
            action.removeDbAttributes(dbEntity.getDataMap(), dbEntity, dbAttributes);
            focusDBEntity();
        }

        if (embeddable != null) {
            action.removeEmbeddableAttributes(embeddable, embeddableAttrs);
            focusEmbeddable();
        }
    }

    @Override
    public void undo() throws CannotUndoException {
        CreateAttributeAction action = actionManager.getAction(CreateAttributeAction.class);

        if (objEntity != null) {
            for (ObjAttribute attr : objAttributes) {
                action.createObjAttribute(objEntity.getDataMap(), objEntity, attr);
            }
            focusObjEntity();
        }

        if (dbEntity != null) {
            for (DbAttribute attr : dbAttributes) {
                action.createDbAttribute(dbEntity.getDataMap(), dbEntity, attr);
            }
            focusDBEntity();
        }

        if (embeddable != null) {
            for (EmbeddableAttribute attr : embeddableAttrs) {
                action.createEmbAttribute(embeddable, attr);
            }
            focusEmbeddable();
        }
    }

    @Override
    public String getPresentationName() {
        if (objEntity != null) {
            return (objAttributes.length > 1) ? "Remove ObjAttributes" : "Remove ObjAttribute";
        }

        if (dbEntity != null) {
            return (dbAttributes.length > 1) ? "Remove DbAttributes" : "Remove DbAttribute";
        }

        if (embeddableAttrs != null) {
            return (embeddableAttrs.length > 1) ? "Remove Embeddable Attributes" : "Remove Embeddable Attribute";
        }

        return super.getPresentationName();
    }
}
