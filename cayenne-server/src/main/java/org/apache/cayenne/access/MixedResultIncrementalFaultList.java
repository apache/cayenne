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

package org.apache.cayenne.access;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.Persistent;
import org.apache.cayenne.ResultIterator;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.ColumnSelect;
import org.apache.cayenne.query.EntityResultSegment;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.util.Util;

/**
 * FaultList that is used for paginated {@link ColumnSelect} queries.
 * It expects data as Object[] where ids are stored instead of Persistent objects (as raw value for single PK
 * or Map for compound PKs).
 * Scalar values that were fetched from ColumnSelect not processed in any way,
 * if there is no Persistent objects in the result Collection it will be iterated as is, without faulting anything.
 *
 * @see QueryMetadata#getPageSize()
 * @see org.apache.cayenne.query.QueryMetadata
 *
 * @since 4.0
 */
class MixedResultIncrementalFaultList<E> extends IncrementalFaultList<E> {

    /**
     * Cached positions for entity results in elements array
     */
    private Map<Integer, ObjEntity> indexToEntity;

    /**
     * Whether result contains only scalars
     */
    private boolean scalarResult;

    /**
     * Creates a new IncrementalFaultList using a given DataContext and query.
     *
     * @param dataContext  DataContext used by IncrementalFaultList to fill itself with
     *                     objects.
     * @param query        Main query used to retrieve data. Must have "pageSize"
     *                     property set to a value greater than zero.
     */
    MixedResultIncrementalFaultList(DataContext dataContext, Query query, int maxFetchSize, List<?> data) {
        super(dataContext, query, maxFetchSize, data);
    }

    @Override
    IncrementalListHelper createHelper(QueryMetadata metadata) {
        // first compile some meta data about results
        indexToEntity = new HashMap<>();
        scalarResult = true;
        for(Object next : metadata.getResultSetMapping()) {
            if(next instanceof EntityResultSegment) {
                EntityResultSegment resultSegment = (EntityResultSegment)next;
                ObjEntity entity = resultSegment.getClassDescriptor().getEntity();
                // store entity's PK position in result
                indexToEntity.put(resultSegment.getColumnOffset(), entity);
                scalarResult = false;
            }
        }

        // if there is no entities in these results,
        // then all data is already there, and we don't need to resolve any objects
        if(indexToEntity.isEmpty()) {
            return new ScalarArrayListHelper();
        } else {
            return new MixedArrayListHelper();
        }
    }

    @Override
    protected void resolveInterval(int fromIndex, int toIndex) {
        if (fromIndex >= toIndex || scalarResult) {
            return;
        }

        synchronized (elements) {
            if (elements.size() == 0) {
                return;
            }

            // perform bound checking
            if (fromIndex < 0) {
                fromIndex = 0;
            }

            if (toIndex > elements.size()) {
                toIndex = elements.size();
            }

            for(Map.Entry<Integer, ObjEntity> entry : indexToEntity.entrySet()) {
                List<Expression> quals = new ArrayList<>(pageSize);
                int dataIdx = entry.getKey();
                for (int i = fromIndex; i < toIndex; i++) {
                    Object[] object = (Object[])elements.get(i);
                    if (getHelper().unresolvedSuspect(object[dataIdx])) {
                        Expression nextQualifier = buildIdQualifier(dataIdx, object);
                        if(nextQualifier != null) {
                            quals.add(nextQualifier);
                        }
                    }
                }

                int qualsSize = quals.size();
                if (qualsSize == 0) {
                    continue;
                }

                // fetch the range of objects in fetchSize chunks
                List<Persistent> objects = new ArrayList<>(qualsSize);

                int fetchSize = maxFetchSize > 0 ? maxFetchSize : Integer.MAX_VALUE;
                int fetchEnd = Math.min(qualsSize, fetchSize);
                int fetchBegin = 0;
                while (fetchBegin < qualsSize) {
                    ObjectSelect<Persistent> query = createSelectQuery(entry.getValue(), quals.subList(fetchBegin, fetchEnd));
                    objects.addAll(query.select(dataContext));
                    fetchBegin = fetchEnd;
                    fetchEnd += Math.min(fetchSize, qualsSize - fetchEnd);
                }

                // replace ids in the list with objects
                updatePageWithResults(objects, dataIdx);
            }
        }
    }

    void updatePageWithResults(List<Persistent> objects, int dataIndex) {
        MixedArrayListHelper helper = (MixedArrayListHelper)getHelper();
        for (Persistent object : objects) {
            helper.updateWithResolvedObject(object, dataIndex);
        }
    }

    ObjectSelect<Persistent> createSelectQuery(ObjEntity entity, List<Expression> expressions) {
        ObjectSelect<Persistent> query = ObjectSelect.query(Persistent.class)
                .entityName(entity.getName())
                .where(ExpressionFactory.joinExp(Expression.OR, expressions));
        if (entity.equals(rootEntity) && metadata.getPrefetchTree() != null) {
            query.prefetch(metadata.getPrefetchTree());
        }
        return query;
    }

    Expression buildIdQualifier(int index, Object[] data) {
        Map<String, Object> map;
        if(data[index] instanceof Map) {
            map = (Map<String, Object>) data[index];
        } else if(data[index] == null) {
            return null;
        } else {
            map = new HashMap<>();
            int i = 0;
            for (ObjAttribute attribute : indexToEntity.get(index).getPrimaryKeys()) {
                map.put(attribute.getDbAttributeName(), data[index + i++]);
            }
        }
        return ExpressionFactory.matchAllDbExp(map, Expression.EQUAL_TO);
    }

    /**
     * Helper that operates on Object[] and checks for Persistent objects' presence in it.
     */
    class MixedArrayListHelper extends IncrementalListHelper {
        @Override
        boolean unresolvedSuspect(Object object) {
            return !(object instanceof Persistent);
        }

        @Override
        boolean objectsAreEqual(Object object, Object objectInTheList) {
            if(!(object instanceof Object[])){
                return false;
            }
            return Arrays.equals((Object[])object, (Object[])objectInTheList);
        }

        @Override
        boolean replacesObject(Object object, Object objectInTheList) {
            throw new UnsupportedOperationException();
        }

        boolean replacesObject(Persistent object, Object[] dataInTheList, int dataIdx) {
            Map<?, ?> map = object.getObjectId().getIdSnapshot();

            if(dataInTheList[dataIdx] instanceof Map) {
                Map<?, ?> id = (Map<?, ?>) dataInTheList[dataIdx];
                if (id.size() != map.size()) {
                    return false;
                }

                for (Map.Entry<?, ?> entry : id.entrySet()) {
                    if (!Util.nullSafeEquals(entry.getValue(), map.get(entry.getKey()))) {
                        return false;
                    }
                }
            } else if(dataInTheList[dataIdx] == null) {
                return false;
            } else {
                for(Object id : map.values()) {
                    if (!dataInTheList[dataIdx++].equals(id)) {
                        return false;
                    }
                }
            }
            return true;
        }

        void updateWithResolvedObject(Persistent object, int dataIdx) {
            synchronized (elements) {
                for (Object element : elements) {
                    Object[] data = (Object[]) element;
                    if (replacesObject(object, data, dataIdx)) {
                        data[dataIdx] = object;
                    }
                }
            }
        }
    }

    /**
     * Helper that actually does nothing
     */
    class ScalarArrayListHelper extends IncrementalListHelper {
        @Override
        boolean unresolvedSuspect(Object object) {
            return false;
        }

        @Override
        boolean objectsAreEqual(Object object, Object objectInTheList) {
            return objectInTheList.equals(object);
        }

        @Override
        boolean replacesObject(Object object, Object objectInTheList) {
            return false;
        }
    }
}
