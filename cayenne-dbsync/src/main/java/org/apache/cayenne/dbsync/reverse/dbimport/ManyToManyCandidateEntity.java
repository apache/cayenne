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

package org.apache.cayenne.dbsync.reverse.dbimport;

import org.apache.cayenne.dbsync.naming.NameBuilder;
import org.apache.cayenne.dbsync.naming.ObjectNameGenerator;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * An ObjEntity that may be removed as a result of flattenning relationships.
 */
class ManyToManyCandidateEntity {

    private static final Logger LOG = LoggerFactory.getLogger(ManyToManyCandidateEntity.class);

    private final ObjEntity joinEntity;

    private final DbRelationship dbRel1;
    private final DbRelationship dbRel2;

    private final ObjEntity entity1;
    private final ObjEntity entity2;

    private final DbRelationship reverseRelationship1;
    private final DbRelationship reverseRelationship2;

    private ManyToManyCandidateEntity(ObjEntity entityValue, List<ObjRelationship> relationships) {
        joinEntity = entityValue;

        ObjRelationship rel1 = relationships.get(0);
        ObjRelationship rel2 = relationships.get(1);

        dbRel1 = rel1.getDbRelationships().get(0);
        dbRel2 = rel2.getDbRelationships().get(0);

        reverseRelationship1 = dbRel1.getReverseRelationship();
        reverseRelationship2 = dbRel2.getReverseRelationship();

        entity1 = rel1.getTargetEntity();
        entity2 = rel2.getTargetEntity();
    }

    /**
     * Method check - if current entity represent many to many temporary table
     *
     * @return true if current entity is represent many to many table; otherwise returns false
     */
    public static ManyToManyCandidateEntity build(ObjEntity joinEntity) {
        ArrayList<ObjRelationship> relationships = new ArrayList<>(joinEntity.getRelationships());
        if (relationships.size() != 2 || (relationships.get(0).getDbRelationships().isEmpty() || relationships.get(1).getDbRelationships().isEmpty())) {
            return null;
        }

        ManyToManyCandidateEntity candidateEntity = new ManyToManyCandidateEntity(joinEntity, relationships);
        if (candidateEntity.isManyToMany()) {
            return candidateEntity;
        }

        return null;
    }

    private boolean isManyToMany() {
        boolean isNotHaveAttributes = joinEntity.getAttributes().isEmpty();

        return isNotHaveAttributes
                && reverseRelationship1 != null && !reverseRelationship1.isFK()
                && reverseRelationship2 != null && !reverseRelationship2.isFK()
                && entity1 != null && entity2 != null;
    }

    private void addFlattenedRelationship(ObjectNameGenerator nameGenerator, ObjEntity srcEntity, ObjEntity dstEntity,
                                          DbRelationship rel1, DbRelationship rel2) {

        if (rel1.getSourceAttributes().isEmpty() && rel2.getTargetAttributes().isEmpty()) {
            LOG.warn("Wrong call ManyToManyCandidateEntity.addFlattenedRelationship(... , " + srcEntity.getName()
                    + ", " + dstEntity.getName() + ", ...)");

            return;
        }

        ObjRelationship newRelationship = new ObjRelationship();
        newRelationship.setName(NameBuilder
                .builder(newRelationship, srcEntity)
                .baseName(nameGenerator.relationshipName(rel1, rel2))
                .name());

        newRelationship.setSourceEntity(srcEntity);
        newRelationship.setTargetEntityName(dstEntity);

        newRelationship.addDbRelationship(rel1);
        newRelationship.addDbRelationship(rel2);

        srcEntity.addRelationship(newRelationship);
    }

    /**
     * Method make direct relationships between 2 entities and remove relationships to
     * many to many entity
     *
     * @param nameGenerator
     */
    public void optimizeRelationships(ObjectNameGenerator nameGenerator) {
        entity1.removeRelationship(reverseRelationship1.getName());
        entity2.removeRelationship(reverseRelationship2.getName());

        addFlattenedRelationship(nameGenerator, entity1, entity2, reverseRelationship1, dbRel2);
        addFlattenedRelationship(nameGenerator, entity2, entity1, reverseRelationship2, dbRel1);
    }

}
