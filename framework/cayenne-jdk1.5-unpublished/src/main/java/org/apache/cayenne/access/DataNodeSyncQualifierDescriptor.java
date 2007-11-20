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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.Transformer;
import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;

/**
 * Builds update qualifier snapshots, including optimistic locking.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
class DataNodeSyncQualifierDescriptor {

    private List attributes;
    private List valueTransformers;
    private boolean usingOptimisticLocking;

    public boolean isUsingOptimisticLocking() {
        return usingOptimisticLocking;
    }

    List getAttributes() {
        return attributes;
    }

    Map createQualifierSnapshot(ObjectDiff diff) {
        int len = attributes.size();

        Map map = new HashMap(len * 2);
        for (int i = 0; i < len; i++) {
            DbAttribute attribute = (DbAttribute) attributes.get(i);
            if (!map.containsKey(attribute.getName())) {

                Object value = ((Transformer) valueTransformers.get(i)).transform(diff);
                map.put(attribute.getName(), value);
            }
        }

        return map;
    }

    void reset(ObjEntity entity, DbEntity dbEntity) {
        attributes = new ArrayList(3);
        valueTransformers = new ArrayList(3);
        usingOptimisticLocking = entity.getLockType() == ObjEntity.LOCK_TYPE_OPTIMISTIC;

        // master PK columns
        if (entity.getDbEntity() == dbEntity) {
            Iterator pkIt = entity.getDbEntity().getPrimaryKeys().iterator();
            while (pkIt.hasNext()) {
                final DbAttribute attribute = (DbAttribute) pkIt.next();
                attributes.add(attribute);
                valueTransformers.add(new Transformer() {

                    public Object transform(Object input) {
                        ObjectId id = (ObjectId) ((ObjectDiff) input).getNodeId();
                        return id.getIdSnapshot().get(attribute.getName());
                    }
                });
            }
        }
        else {
            // detail table PK columns
            DbRelationship masterDependentDbRel = findMasterToDependentDbRelationship(
                    entity.getDbEntity(),
                    dbEntity);

            if (masterDependentDbRel != null) {

                Iterator joinsIterator = masterDependentDbRel.getJoins().iterator();
                while (joinsIterator.hasNext()) {
                    final DbJoin dbAttrPair = (DbJoin) joinsIterator.next();
                    DbAttribute dbAttribute = dbAttrPair.getTarget();
                    if (!attributes.contains(dbAttribute)) {

                        attributes.add(dbAttribute);
                        valueTransformers.add(new Transformer() {

                            public Object transform(Object input) {
                                ObjectId id = (ObjectId) ((ObjectDiff) input).getNodeId();
                                return id.getIdSnapshot().get(dbAttrPair.getSourceName());
                            }
                        });
                    }
                }
            }
        }

        if (usingOptimisticLocking) {

            Iterator attributeIt = entity.getAttributes().iterator();
            while (attributeIt.hasNext()) {
                final ObjAttribute attribute = (ObjAttribute) attributeIt.next();

                if (attribute.isUsedForLocking()) {
                    // only care about first step in a flattened attribute
                    DbAttribute dbAttribute = (DbAttribute) attribute
                            .getDbPathIterator()
                            .next();

                    if (!attributes.contains(dbAttribute)) {
                        attributes.add(dbAttribute);

                        valueTransformers.add(new Transformer() {

                            public Object transform(Object input) {
                                return ((ObjectDiff) input).getSnapshotValue(attribute
                                        .getName());
                            }
                        });
                    }
                }
            }

            Iterator relationshipIt = entity.getRelationships().iterator();
            while (relationshipIt.hasNext()) {
                final ObjRelationship relationship = (ObjRelationship) relationshipIt
                        .next();

                if (relationship.isUsedForLocking()) {
                    // only care about the first DbRelationship
                    DbRelationship dbRelationship = (DbRelationship) relationship
                            .getDbRelationships()
                            .get(0);

                    Iterator joinsIterator = dbRelationship.getJoins().iterator();
                    while (joinsIterator.hasNext()) {
                        final DbJoin dbAttrPair = (DbJoin) joinsIterator.next();
                        DbAttribute dbAttribute = dbAttrPair.getSource();

                        // relationship transformers override attribute transformers for
                        // meaningful FK's... why meanigful FKs can go out of sync is
                        // another story (CAY-595)
                        int index = attributes.indexOf(dbAttribute);
                        if (index >= 0 && !dbAttribute.isForeignKey()) {
                            continue;
                        }

                        Object transformer = new Transformer() {

                            public Object transform(Object input) {
                                ObjectId targetId = ((ObjectDiff) input)
                                        .getArcSnapshotValue(relationship.getName());
                                return targetId != null ? targetId.getIdSnapshot().get(
                                        dbAttrPair.getTargetName()) : null;
                            }
                        };

                        if (index < 0) {
                            attributes.add(dbAttribute);
                            valueTransformers.add(transformer);
                        }
                        else {
                            valueTransformers.set(index, transformer);
                        }
                    }
                }
            }
        }
    }

    private DbRelationship findMasterToDependentDbRelationship(
            DbEntity masterDbEntity,
            DbEntity dependentDbEntity) {

        Iterator it = masterDbEntity.getRelationshipMap().values().iterator();
        while (it.hasNext()) {

            DbRelationship relationship = (DbRelationship) it.next();
            if (dependentDbEntity.equals(relationship.getTargetEntity())
                    && relationship.isToDependentPK()) {

                if (relationship.isToMany()) {
                    throw new CayenneRuntimeException(
                            "Only 'to one' master-detail relationships can be processed.");
                }

                return relationship;
            }
        }

        return null;
    }
}
