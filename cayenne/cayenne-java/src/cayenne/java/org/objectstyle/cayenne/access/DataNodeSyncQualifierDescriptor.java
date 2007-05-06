/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.access;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.Transformer;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DbJoin;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.ObjAttribute;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;

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
            Iterator pkIt = entity.getDbEntity().getPrimaryKey().iterator();
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
