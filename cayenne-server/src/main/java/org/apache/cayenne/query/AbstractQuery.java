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
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.util.ToStringBuilder;

/**
 * A common superclass of Cayenne queries.
 */
public abstract class AbstractQuery extends CacheableQuery {

    /**
     * The root object this query. May be an entity name, Java class, ObjEntity or
     * DbEntity, depending on the specific query and how it was constructed.
     */
    protected Object root;

    /**
     * Returns default select parameters.
     * 
     * @since 1.2
     */
    public QueryMetadata getMetaData(EntityResolver resolver) {
        BaseQueryMetadata md = new BaseQueryMetadata();
        md.resolve(getRoot(), resolver);
        return md;
    }

    /**
     * Returns the root of this query.
     */
    public Object getRoot() {
        return root;
    }

    /**
     * Sets the root of the query
     * 
     * @param value The new root
     * @throws IllegalArgumentException if value is not a String, ObjEntity, DbEntity,
     *             Procedure, DataMap, Class or null.
     */
    public void setRoot(Object value) {
        if (value == null) {
            this.root = null;
        }

        // sanity check
        if (!((value instanceof String)
                || (value instanceof ObjEntity)
                || (value instanceof DbEntity)
                || (value instanceof Class)
                || (value instanceof Procedure) || (value instanceof DataMap))) {

            String rootClass = (value != null) ? value.getClass().getName() : "null";

            throw new IllegalArgumentException(
                    getClass().getName()
                            + ": \"setRoot(..)\" takes a DataMap, String, ObjEntity, DbEntity, Procedure, "
                            + "or Class. It was passed a "
                            + rootClass);
        }

        this.root = value;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("root", root)
                .toString();
    }

    /**
     * @since 1.2
     */
    public abstract SQLAction createSQLAction(SQLActionVisitor visitor);

    /**
     * Implements default routing mechanism relying on the EntityResolver to find DataMap
     * based on the query root. This mechanism should be sufficient for most queries that
     * "know" their root.
     * 
     * @since 1.2
     */
    public void route(QueryRouter router, EntityResolver resolver, Query substitutedQuery) {
        DataMap map = getMetaData(resolver).getDataMap();

        if (map == null) {
            throw new CayenneRuntimeException("No DataMap found, can't route query %s", this);
        }

        router.route(router.engineForDataMap(map), this, substitutedQuery);
    }
}
