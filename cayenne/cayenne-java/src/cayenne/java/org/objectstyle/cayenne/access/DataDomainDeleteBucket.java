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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.Persistent;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.EntitySorter;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.query.DeleteBatchQuery;

/**
 * @since 1.2
 * @author Andrus Adamchik
 */
class DataDomainDeleteBucket extends DataDomainSyncBucket {

    DataDomainDeleteBucket(DataDomainFlushAction parent) {
        super(parent);
    }

    void postprocess() {

        if (!objectsByEntity.isEmpty()) {

            Collection deletedIds = parent.getResultDeletedIds();

            Iterator it = objectsByEntity.values().iterator();
            while (it.hasNext()) {
                Iterator objects = ((Collection) it.next()).iterator();
                while (objects.hasNext()) {
                    Persistent object = (Persistent) objects.next();
                    deletedIds.add(object.getObjectId());
                }
            }
        }
    }

    void appendQueriesInternal(Collection queries) {

        DataNodeSyncQualifierDescriptor qualifierBuilder = new DataNodeSyncQualifierDescriptor();

        EntitySorter sorter = parent.getDomain().getEntitySorter();
        sorter.sortDbEntities(dbEntities, true);

        for (Iterator i = dbEntities.iterator(); i.hasNext();) {
            DbEntity dbEntity = (DbEntity) i.next();
            List objEntitiesForDbEntity = (List) objEntitiesByDbEntity.get(dbEntity);
            Map batches = new LinkedHashMap();

            for (Iterator j = objEntitiesForDbEntity.iterator(); j.hasNext();) {
                ObjEntity entity = (ObjEntity) j.next();

                qualifierBuilder.reset(entity, dbEntity);

                boolean isRootDbEntity = (entity.getDbEntity() == dbEntity);

                // remove object set for dependent entity, so that it does not show up
                // on post processing
                List objects = (List) objectsByEntity.get(entity);

                if (objects.isEmpty()) {
                    continue;
                }

                checkReadOnly(entity);

                if (isRootDbEntity) {
                    sorter.sortObjectsForEntity(entity, objects, true);
                }

                for (Iterator k = objects.iterator(); k.hasNext();) {
                    DataObject o = (DataObject) k.next();
                    ObjectDiff diff = parent.objectDiff(o.getObjectId());

                    Map qualifierSnapshot = qualifierBuilder
                            .createQualifierSnapshot(diff);

                    // organize batches by the nulls in qualifier
                    Set nullQualifierNames = new HashSet();
                    Iterator it = qualifierSnapshot.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry entry = (Map.Entry) it.next();
                        if (entry.getValue() == null) {
                            nullQualifierNames.add(entry.getKey());
                        }
                    }

                    List batchKey = Arrays.asList(new Object[] {
                        nullQualifierNames
                    });

                    DeleteBatchQuery batch = (DeleteBatchQuery) batches.get(batchKey);
                    if (batch == null) {
                        batch = new DeleteBatchQuery(dbEntity, qualifierBuilder
                                .getAttributes(), nullQualifierNames, 27);
                        batch.setUsingOptimisticLocking(qualifierBuilder
                                .isUsingOptimisticLocking());
                        batches.put(batchKey, batch);
                    }

                    batch.add(qualifierSnapshot);
                }
            }

            queries.addAll(batches.values());
        }
    }
}
