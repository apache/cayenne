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

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.EmbeddableAttribute;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.modeler.action.CreateAttributeAction;
import org.apache.cayenne.modeler.action.RemoveAttributeAction;
import org.apache.cayenne.modeler.event.EmbeddableDisplayEvent;
import org.apache.cayenne.modeler.event.EntityDisplayEvent;

public class RemoveAttributeUndoableEdit extends CayenneUndoableEdit {

    private DataDomain domain;
    private DataMap dataMap;

    private DbAttribute[] dbAttributes;
    private ObjAttribute[] objAttributes;

    private ObjEntity objEntity;
    private DbEntity dbEntity;

    private Embeddable embeddable;
    private EmbeddableAttribute[] embeddableAttrs;

    public RemoveAttributeUndoableEdit(Embeddable embeddable,
            EmbeddableAttribute[] embeddableAttrs) {
        super();
        this.embeddable = embeddable;
        this.embeddableAttrs = embeddableAttrs;
    }

    public RemoveAttributeUndoableEdit(DataDomain domain, DataMap dataMap,
            ObjEntity entity, ObjAttribute[] attribs) {
        this.objEntity = entity;
        this.objAttributes = attribs;
        this.domain = domain;
        this.dataMap = dataMap;
    }

    public RemoveAttributeUndoableEdit(DataDomain domain, DataMap dataMap,
            DbEntity entity, DbAttribute[] attribs) {
        this.dbEntity = entity;
        this.dbAttributes = attribs;
        this.domain = domain;
        this.dataMap = dataMap;
    }

    @Override
    public void redo() throws CannotRedoException {
        RemoveAttributeAction action = (RemoveAttributeAction) actionManager
                .getAction(RemoveAttributeAction.getActionName());

        if (objEntity != null) {
            action.removeObjAttributes(objEntity, objAttributes);
            controller.fireObjEntityDisplayEvent(new EntityDisplayEvent(
                    this,
                    objEntity,
                    dataMap,
                    domain));
        }

        if (dbEntity != null) {
            action.removeDbAttributes(dbEntity.getDataMap(), dbEntity, dbAttributes);
            controller.fireDbEntityDisplayEvent(new EntityDisplayEvent(
                    this,
                    dbEntity,
                    dataMap,
                    domain));
        }

        if (embeddable != null) {
            action.removeEmbeddableAttributes(embeddable, embeddableAttrs);
            controller.fireEmbeddableDisplayEvent(new EmbeddableDisplayEvent(
                    this,
                    embeddable,
                    dataMap,
                    domain));
        }
    }

    @Override
    public void undo() throws CannotUndoException {

        CreateAttributeAction action = (CreateAttributeAction) actionManager
                .getAction(CreateAttributeAction.getActionName());

        if (objEntity != null) {
            for (ObjAttribute attr : objAttributes) {
                action.createObjAttribute(domain, dataMap, objEntity, attr);
            }
        }

        if (dbEntity != null) {
            for (DbAttribute attr : dbAttributes) {
                action.createDbAttribute(domain, dataMap, dbEntity, attr);
            }
        }

        if (embeddable != null) {
            for (EmbeddableAttribute attr : embeddableAttrs) {
                action.createEmbAttribute(embeddable, attr);
            }
        }

    }

    @Override
    public String getPresentationName() {
        if (objEntity != null) {
            return (objAttributes.length > 1)
                    ? "Remove ObjAttributes"
                    : "Remove ObjAttribute";
        }

        if (dbEntity != null) {
            return (dbAttributes.length > 1)
                    ? "Remove DbAttributes"
                    : "Remove DbAttribute";
        }

        if (embeddableAttrs != null) {
            return (embeddableAttrs.length > 1)
                    ? "Remove Embeddable Attributes"
                    : "Remove Embeddable Attribute";
        }

        return super.getPresentationName();
    }
}
