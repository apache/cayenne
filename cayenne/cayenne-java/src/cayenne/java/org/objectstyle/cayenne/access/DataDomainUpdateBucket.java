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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.query.UpdateBatchQuery;

/**
 * @since 1.2
 * @author Andrus Adamchik
 */
class DataDomainUpdateBucket extends DataDomainSyncBucket {

    DataDomainUpdateBucket(DataDomainFlushAction parent) {
        super(parent);
    }

    void appendQueriesInternal(Collection queries) {

        DataDomainDBDiffBuilder diffBuilder = new DataDomainDBDiffBuilder();
        DataNodeSyncQualifierDescriptor qualifierBuilder = new DataNodeSyncQualifierDescriptor();

        for (Iterator i = dbEntities.iterator(); i.hasNext();) {
            DbEntity dbEntity = (DbEntity) i.next();
            List objEntitiesForDbEntity = (List) objEntitiesByDbEntity.get(dbEntity);
            Map batches = new LinkedHashMap();

            for (Iterator j = objEntitiesForDbEntity.iterator(); j.hasNext();) {
                ObjEntity entity = (ObjEntity) j.next();

                diffBuilder.reset(entity, dbEntity);
                qualifierBuilder.reset(entity, dbEntity);
                boolean isRootDbEntity = entity.getDbEntity() == dbEntity;

                List objects = (List) objectsByEntity.get(entity);

                for (Iterator k = objects.iterator(); k.hasNext();) {
                    DataObject o = (DataObject) k.next();
                    ObjectDiff diff = parent.objectDiff(o.getObjectId());

                    Map snapshot = diffBuilder.buildDBDiff(diff);

                    // check whether MODIFIED object has real db-level modifications
                    if (snapshot == null) {

                        if (isRootDbEntity) {
                            k.remove();
                            o.setPersistenceState(PersistenceState.COMMITTED);
                        }

                        continue;
                    }

                    // after we filtered out "fake" modifications, check if an
                    // attempt is made to modify a read only entity
                    checkReadOnly(entity);

                    Map qualifierSnapshot = qualifierBuilder
                            .createQualifierSnapshot(diff);

                    // organize batches by the updated columns + nulls in qualifier
                    Set snapshotSet = snapshot.keySet();
                    Set nullQualifierNames = new HashSet();
                    Iterator it = qualifierSnapshot.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry entry = (Map.Entry) it.next();
                        if (entry.getValue() == null) {
                            nullQualifierNames.add(entry.getKey());
                        }
                    }

                    List batchKey = Arrays.asList(new Object[] {
                            snapshotSet, nullQualifierNames
                    });

                    UpdateBatchQuery batch = (UpdateBatchQuery) batches.get(batchKey);
                    if (batch == null) {
                        batch = new UpdateBatchQuery(
                                dbEntity,
                                qualifierBuilder.getAttributes(),
                                updatedAttributes(dbEntity, snapshot),
                                nullQualifierNames,
                                10);

                        batch.setUsingOptimisticLocking(qualifierBuilder
                                .isUsingOptimisticLocking());
                        batches.put(batchKey, batch);
                    }

                    batch.add(qualifierSnapshot, snapshot, o.getObjectId());

                    // update replacement id with meaningful PK changes
                    if (isRootDbEntity) {
                        Map replacementId = o.getObjectId().getReplacementIdMap();

                        Iterator pkIt = dbEntity.getPrimaryKey().iterator();
                        while (pkIt.hasNext()) {
                            String name = ((DbAttribute) pkIt.next()).getName();
                            if (snapshot.containsKey(name)
                                    && !replacementId.containsKey(name)) {
                                replacementId.put(name, snapshot.get(name));
                            }
                        }
                    }
                }
            }

            queries.addAll(batches.values());
        }
    }

    /**
     * Creates a list of DbAttributes that are updated in a snapshot
     */
    private List updatedAttributes(DbEntity entity, Map updatedSnapshot) {
        List attributes = new ArrayList(updatedSnapshot.size());
        Map entityAttributes = entity.getAttributeMap();

        Iterator it = updatedSnapshot.keySet().iterator();
        while (it.hasNext()) {
            Object name = it.next();
            attributes.add(entityAttributes.get(name));
        }

        return attributes;
    }
}
