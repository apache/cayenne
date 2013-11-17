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
package org.apache.cayenne.access;

import java.util.ArrayList;
import java.util.List;

import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.util.NamedObjectFactory;

/**
 * Class represent ObjEntity that may be optimized using flattened relationships
 * as many to many table
 */
class ManyToManyCandidateEntity {
    private ObjEntity entity;

    public ManyToManyCandidateEntity(ObjEntity entityValue) {
        entity = entityValue;
    }

    public ObjEntity getEntity() {
        return entity;
    }

    private boolean isTargetEntitiesDifferent() {
        return !getTargetEntity1().equals(getTargetEntity2());
    }

    private boolean isRelationshipsHasDependentPK() {
        boolean isRelationship1HasDepPK = getDbRelationship1().getReverseRelationship().isToDependentPK();
        boolean isRelationship2HasDepPK = getDbRelationship2().getReverseRelationship().isToDependentPK();

        return isRelationship1HasDepPK && isRelationship2HasDepPK;
    }

    private ObjRelationship getRelationship1() {
        List<ObjRelationship> relationships = new ArrayList<ObjRelationship>(entity.getRelationships());
        return relationships.get(0);
    }

    private ObjRelationship getRelationship2() {
        List<ObjRelationship> relationships = new ArrayList<ObjRelationship>(entity.getRelationships());
        return relationships.get(1);
    }

    private ObjEntity getTargetEntity1() {
        return (ObjEntity) getRelationship1().getTargetEntity();
    }

    private ObjEntity getTargetEntity2() {
        return (ObjEntity) getRelationship2().getTargetEntity();
    }

    private DbRelationship getDbRelationship1() {
        return getRelationship1().getDbRelationships().get(0);
    }

    private DbRelationship getDbRelationship2() {
        return getRelationship2().getDbRelationships().get(0);
    }

    /**
     * Method check - if current entity represent many to many temporary table
     * @return true if current entity is represent many to many table; otherwise returns false
     */
    public boolean isRepresentManyToManyTable() {
        boolean hasTwoRelationships = entity.getRelationships().size() == 2;
        boolean isNotHaveAttributes = entity.getAttributes().size() == 0;

        return hasTwoRelationships && isNotHaveAttributes && isRelationshipsHasDependentPK()
                && isTargetEntitiesDifferent();
    }

    private void removeRelationshipsFromTargetEntities() {
        getTargetEntity1().removeRelationship(getRelationship1().getReverseRelationship().getName());
        getTargetEntity2().removeRelationship(getRelationship2().getReverseRelationship().getName());
    }

    private void addFlattenedRelationship(ObjEntity srcEntity, ObjEntity dstEntity,
                                          DbRelationship... relationshipPath) {
        ObjRelationship newRelationship = (ObjRelationship) NamedObjectFactory.createRelationship(srcEntity, dstEntity,
                true);

        newRelationship.setSourceEntity(srcEntity);
        newRelationship.setTargetEntity(dstEntity);

        for (DbRelationship curRelationship : relationshipPath) {
            newRelationship.addDbRelationship(curRelationship);
        }

        srcEntity.addRelationship(newRelationship);
    }

    /**
     * Method make direct relationships between 2 entities and remove relationships to
     * many to many entity
     */
    public void optimizeRelationships() {
        removeRelationshipsFromTargetEntities();

        DbRelationship dbRelationship1 = getRelationship1().getDbRelationships().get(0);
        DbRelationship dbRelationship2 = getRelationship2().getDbRelationships().get(0);

        addFlattenedRelationship(getTargetEntity1(), getTargetEntity2(), dbRelationship1.getReverseRelationship(),
                dbRelationship2);

        addFlattenedRelationship(getTargetEntity2(), getTargetEntity1(), dbRelationship2.getReverseRelationship(),
                dbRelationship1);
    }

}
