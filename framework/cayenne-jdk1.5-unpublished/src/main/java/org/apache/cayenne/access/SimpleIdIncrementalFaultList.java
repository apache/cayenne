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

import java.util.Collection;
import java.util.Map;

import org.apache.cayenne.Persistent;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.QueryMetadata;

/**
 * A paginated list that implements a strategy for retrieval of entities with a single PK
 * column. It is much more memory-efficient compared to the superclass.
 * 
 * @since 3.0
 */
class SimpleIdIncrementalFaultList<E> extends IncrementalFaultList<E> {

    protected DbAttribute pk;

    SimpleIdIncrementalFaultList(DataContext dataContext, Query query, int maxFetchSize) {
        super(dataContext, query, maxFetchSize);

        Collection<DbAttribute> pks = rootEntity.getDbEntity().getPrimaryKeys();
        if (pks.size() != 1) {
            throw new IllegalArgumentException(
                    "Expected a single column primary key, instead got "
                            + pks.size()
                            + ". ObjEntity: "
                            + rootEntity.getName());
        }

        pk = pks.iterator().next();
    }

    @Override
    IncrementalFaultList<E>.IncrementalListHelper createHelper(QueryMetadata metadata) {
        if (metadata.isFetchingDataRows()) {
            return new SingleIdDataRowListHelper();
        }
        else {
            return new SingleIdPersistentListHelper();
        }
    }

    @Override
    protected Expression buildIdQualifier(Object id) {
        return ExpressionFactory.matchDbExp(pk.getName(), id);
    }

    class SingleIdPersistentListHelper extends
            IncrementalFaultList<E>.PersistentListHelper {

        @Override
        boolean objectsAreEqual(Object object, Object objectInTheList) {

            if (objectInTheList instanceof Persistent) {
                // due to object uniquing this should be sufficient
                return object == objectInTheList;
            }
            else {
                Persistent persistent = (Persistent) object;
                Map<String, Object> idSnapshot = persistent.getObjectId().getIdSnapshot();

                return idSnapshot.size() == 1
                        && objectInTheList.equals(idSnapshot.get(pk.getName()));
            }
        }

        @Override
        boolean replacesObject(Object object, Object objectInTheList) {
            if (objectInTheList instanceof Persistent) {
                return false;
            }

            Persistent persistent = (Persistent) object;
            Map<String, Object> idSnapshot = persistent.getObjectId().getIdSnapshot();

            return idSnapshot.size() == 1
                    && objectInTheList.equals(idSnapshot.get(pk.getName()));
        }
    }

    class SingleIdDataRowListHelper extends IncrementalFaultList<E>.DataRowListHelper {

        @Override
        boolean objectsAreEqual(Object object, Object objectInTheList) {

            if (objectInTheList instanceof Map) {
                return super.objectsAreEqual(object, objectInTheList);
            }

            if (object == null && objectInTheList == null) {
                return true;
            }

            if (object != null && objectInTheList != null) {

                Map<?, ?> map = (Map<?, ?>) object;

                return objectInTheList.equals(map.get(pk.getName()));
            }

            return false;
        }

        @Override
        boolean replacesObject(Object object, Object objectInTheList) {

            if (objectInTheList instanceof Map) {
                return false;
            }

            Map<?, ?> map = (Map<?, ?>) object;
            return objectInTheList.equals(map.get(pk.getName()));
        }
    }
}
