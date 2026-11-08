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
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.compiled.CompiledAttribute;
import org.apache.cayenne.map.compiled.CompiledEntity;
import org.apache.cayenne.query.EntityResultSegment;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.resolved.ResolvedQuery;
import org.apache.cayenne.query.ResultSegment;
import java.util.Objects;

/**
 * FaultList that is used for paginated column queries, see the column methods of {@link ObjectSelect}.
 * It expects data as Object[] where ids are stored instead of Persistent objects (as raw value for single PK
 * or Map for compound PKs).
 * Scalar values that were fetched by the column query are not processed in any way,
 * if there is no Persistent objects in the result Collection it will be iterated as is, without faulting anything.
 *
 * @see org.apache.cayenne.query.resolved.ResolvedQuery#pageSize()
 *
 * @since 4.0
 */
class MixedResultIncrementalFaultList<E> extends IncrementalFaultList<E> {

    /**
     * Cached positions for entity results in elements array
     */
    private Map<Integer, CompiledEntity> indexToEntity;

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
    MixedResultIncrementalFaultList(DataContext dataContext, ResolvedQuery resolved, int maxFetchSize, List<?> data) {
        super(dataContext, resolved, maxFetchSize, data);
    }

    @Override
    IncrementalListHelper createHelper(ResolvedQuery resolved) {
        // first compile some meta data about results
        indexToEntity = new HashMap<>();
        scalarResult = true;
        List<ResultSegment> segments = resolved.resultSegments().segments();
        for (int i = 0; i < segments.size(); i++) {
            if (segments.get(i) instanceof EntityResultSegment resultSegment) {
                CompiledEntity entity = resultSegment.entity();
                // a row is an Object[] with one element per segment, so the entity id sits at the segment position
                indexToEntity.put(i, entity);
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

            for(Map.Entry<Integer, CompiledEntity> entry : indexToEntity.entrySet()) {
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

    ObjectSelect<Persistent> createSelectQuery(CompiledEntity entity, List<Expression> expressions) {
        ObjectSelect<Persistent> query = ObjectSelect.query(Persistent.class)
                .entityName(entity.name())
                .where(ExpressionFactory.or(expressions));
        if (entity.equals(rootEntity) && resolved.prefetchTree() != null) {
            query.prefetch(resolved.prefetchTree());
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
            for (CompiledAttribute attribute : indexToEntity.get(index).primaryKeyAttributes()) {
                map.put(attribute.columnName(), data[index + i++]);
            }
        }
        return ExpressionFactory.matchAllDbExp(map);
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
                    if (!Objects.deepEquals(entry.getValue(), map.get(entry.getKey()))) {
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
