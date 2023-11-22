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

package org.apache.cayenne.query;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.util.Util;

/**
 * A query that matches zero or one object or data row corresponding to the ObjectId. Used
 * internally by Cayenne to lookup objects by id. Notice that cache policies of
 * ObjectIdQuery are different from generic {@link QueryMetadata} cache policies.
 * ObjectIdQuery is special - it is the only query that can be done against Cayenne main
 * cache, thus cache handling is significantly different from all other of the queries.
 * 
 * @since 1.2
 */
public class ObjectIdQuery extends IndirectQuery {

    // TODO: Andrus, 2/18/2006 - reconcile this with QueryMetadata cache policies
    public static final int CACHE = 1;
    public static final int CACHE_REFRESH = 2;
    public static final int CACHE_NOREFRESH = 3;

    protected ObjectId objectId;
    protected int cachePolicy;
    protected boolean fetchingDataRows;

    protected transient EntityResolver metadataResolver;
    protected transient QueryMetadata metadata;

    // needed for hessian serialization
    @SuppressWarnings("unused")
    private ObjectIdQuery() {
        this.cachePolicy = CACHE_REFRESH;
    }

    /**
     * Creates a refreshing ObjectIdQuery.
     */
    public ObjectIdQuery(ObjectId objectID) {
        this(objectID, false, CACHE_REFRESH);
    }

    /**
     * Creates a new ObjectIdQuery.
     */
    public ObjectIdQuery(ObjectId objectId, boolean fetchingDataRows, int cachePolicy) {
        if (objectId == null) {
            throw new NullPointerException("Null objectID");
        }

        this.objectId = objectId;
        this.cachePolicy = cachePolicy;
        this.fetchingDataRows = fetchingDataRows;
    }

    /**
     * Returns query metadata object.
     */
    // return metadata without creating replacement, as it is not always possible to
    // create replacement (e.g. temp ObjectId).
    @Override
    public QueryMetadata getMetaData(final EntityResolver resolver) {
        // caching metadata as it may be accessed multiple times (at a DC and DD level)
        if (metadata == null || metadataResolver != resolver) {
            this.metadata = new DefaultQueryMetadata() {

                @Override
                public ClassDescriptor getClassDescriptor() {
                    return resolver.getClassDescriptor(objectId.getEntityName());
                }
                
                @Override
                public ObjEntity getObjEntity() {
                    return getClassDescriptor().getEntity();
                }

                @Override
                public boolean isFetchingDataRows() {
                    return fetchingDataRows;
                }
            };

            this.metadataResolver = resolver;
        }

        return metadata;
    }

    public ObjectId getObjectId() {
        return objectId;
    }

    @Override
    protected Query createReplacementQuery(EntityResolver resolver) {
        if (objectId == null) {
            throw new CayenneRuntimeException("Can't resolve query - objectId is null.");
        }

        if (objectId.isTemporary() && !objectId.isReplacementIdAttached()) {
            throw new CayenneRuntimeException("Can't build a query for temporary id: %s", objectId);
        }

        ObjectSelect<Persistent> query = ObjectSelect.query(Persistent.class)
                .entityName(objectId.getEntityName())
                .where(ExpressionFactory.matchAllDbExp(objectId.getIdSnapshot(), Expression.EQUAL_TO));
        // if we got to the point of fetch, always force refresh....
        if(fetchingDataRows) {
            query.fetchDataRows();
        }
        return query;
    }

    public int getCachePolicy() {
        return cachePolicy;
    }

    public boolean isFetchMandatory() {
        return cachePolicy == CACHE_REFRESH;
    }

    public boolean isFetchAllowed() {
        return cachePolicy != CACHE_NOREFRESH;
    }

    public boolean isFetchingDataRows() {
        return fetchingDataRows;
    }

    /**
     * Overrides toString() outputting a short string with query class and ObjectId.
     */
    @Override
    public String toString() {
        return Util.stripPackageName(getClass().getName()) + ":" + objectId;
    }

    /**
     * An object is considered equal to this query if it is also a ObjectIdQuery with
     * an equal ObjectId.
     */
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof ObjectIdQuery)) {
            return false;
        }

        ObjectIdQuery query = (ObjectIdQuery) object;

        return Util.nullSafeEquals(objectId, query.getObjectId());
    }

    /**
     * Implements a standard hashCode contract considering custom 'equals' implementation.
     */
    @Override
    public int hashCode() {
        return (objectId != null) ? objectId.hashCode() : 11;
    }
}
