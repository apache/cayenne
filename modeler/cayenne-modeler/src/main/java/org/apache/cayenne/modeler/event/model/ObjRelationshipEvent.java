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

package org.apache.cayenne.modeler.event.model;

import org.apache.cayenne.modeler.event.model.ObjRelationshipEvent;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;

/**
 * Represents events resulted from ObjRelationship changes in CayenneModeler.
 */
public class ObjRelationshipEvent extends ModelEvent {

    private final ObjRelationship relationship;
    private final ObjEntity entity;

    public static ObjRelationshipEvent ofAdd(Object src, ObjRelationship rel, ObjEntity entity) {
        return new ObjRelationshipEvent(src, rel, entity, Type.ADD, null);
    }

    public static ObjRelationshipEvent ofChange(Object src, ObjRelationship rel, ObjEntity entity) {
        return new ObjRelationshipEvent(src, rel, entity, Type.CHANGE, null);
    }

    public static ObjRelationshipEvent ofChange(Object src, ObjRelationship rel, ObjEntity entity, String oldName) {
        return new ObjRelationshipEvent(src, rel, entity, Type.CHANGE, oldName);
    }

    public static ObjRelationshipEvent ofRemove(Object src, ObjRelationship rel, ObjEntity entity) {
        return new ObjRelationshipEvent(src, rel, entity, Type.REMOVE, null);
    }

    private ObjRelationshipEvent(Object src, ObjRelationship rel, ObjEntity entity, Type type, String oldName) {
        super(src, type, oldName);
        this.relationship = rel;
        this.entity = entity;
    }

    public ObjRelationship getRelationship() {
        return relationship;
    }

    public ObjEntity getEntity() {
        return entity;
    }

    @Override
    public String getNewName() {
        return (relationship != null) ? relationship.getName() : null;
    }
}
