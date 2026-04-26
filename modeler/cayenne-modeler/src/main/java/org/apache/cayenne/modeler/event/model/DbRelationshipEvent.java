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

import org.apache.cayenne.modeler.event.model.DbRelationshipEvent;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;

/**
 * Represents events resulted from DbRelationship changes in CayenneModeler.
 */
public class DbRelationshipEvent extends ModelEvent {

    private final DbRelationship relationship;
    private final DbEntity entity;

    public static DbRelationshipEvent ofAdd(Object src, DbRelationship rel, DbEntity entity) {
        return new DbRelationshipEvent(src, rel, entity, Type.ADD, null);
    }

    public static DbRelationshipEvent ofChange(Object src, DbRelationship rel, DbEntity entity) {
        return new DbRelationshipEvent(src, rel, entity, Type.CHANGE, null);
    }

    public static DbRelationshipEvent ofChange(Object src, DbRelationship rel, DbEntity entity, String oldName) {
        return new DbRelationshipEvent(src, rel, entity, Type.CHANGE, oldName);
    }

    public static DbRelationshipEvent ofRemove(Object src, DbRelationship rel, DbEntity entity) {
        return new DbRelationshipEvent(src, rel, entity, Type.REMOVE, null);
    }

    private DbRelationshipEvent(Object src, DbRelationship rel, DbEntity entity, Type type, String oldName) {
        super(src, type, oldName);
        this.relationship = rel;
        this.entity = entity;
    }

    public DbRelationship getRelationship() {
        return relationship;
    }

    public DbEntity getEntity() {
        return entity;
    }

    @Override
    public String getNewName() {
        return (relationship != null) ? relationship.getName() : null;
    }
}
