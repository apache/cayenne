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

package org.apache.cayenne.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.configuration.ConfigurationNodeVisitor;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.EntityResolver;

/**
 * A Query decorator for a collection of other queries. Note that QueryChain will always
 * return DataRows (that is if it returns data), as it has no way of knowing how to
 * convert the results to objects.
 * 
 * @since 1.2
 */
public class QueryChain implements Query {

    protected Collection<Query> chain;
    protected String name;

    /**
     * @since 3.1
     */
    protected DataMap dataMap;

    /**
     * Creates an empty QueryChain.
     */
    public QueryChain() {
    }

    /**
     * Creates a new QueryChain out of an array of queries.
     */
    public QueryChain(Query[] queries) {
        if (queries != null && queries.length > 0) {
            this.chain = new ArrayList<Query>(Arrays.asList(queries));
        }
    }

    /**
     * Creates a new QueryChain with a collection of Queries.
     */
    public QueryChain(Collection<? extends Query> queries) {
        if (queries != null && !queries.isEmpty()) {
            this.chain = new ArrayList<Query>(queries);
        }
    }

    /**
     * @since 3.1
     */
    public <T> T acceptVisitor(ConfigurationNodeVisitor<T> visitor) {
        return visitor.visitQuery(this);
    }

    /**
     * Adds a query to the chain.
     */
    public void addQuery(Query query) {
        if (chain == null) {
            chain = new ArrayList<Query>();
        }

        chain.add(query);
    }

    /**
     * Removes a query from the chain, returning true if the query was indeed present in
     * the chain and was removed.
     */
    public boolean removeQuery(Query query) {
        return (chain != null) ? chain.remove(query) : false;
    }

    public boolean isEmpty() {
        return chain == null || chain.isEmpty();
    }

    /**
     * Delegates routing to each individual query in the chain. If there is no queries,
     * this method does nothing.
     */
    public void route(QueryRouter router, EntityResolver resolver, Query substitutedQuery) {
        if (chain != null && !chain.isEmpty()) {
            for (Query q : chain) {
                q.route(router, resolver, substitutedQuery);
            }
        }
    }

    /**
     * Throws an exception as execution should've been delegated to the queries contained
     * in the chain.
     */
    public SQLAction createSQLAction(SQLActionVisitor visitor) {
        throw new CayenneRuntimeException("Chain doesn't support its own execution "
                + "and should've been split into separate queries during routing phase.");
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * @since 3.1
     */
    public DataMap getDataMap() {
        return dataMap;
    }

    /**
     * @since 3.1
     */
    public void setDataMap(DataMap dataMap) {
        this.dataMap = dataMap;
    }

    /**
     * Returns default metadata.
     */
    public QueryMetadata getMetaData(EntityResolver resolver) {
        QueryMetadataWrapper wrapper = new QueryMetadataWrapper(
                DefaultQueryMetadata.defaultMetadata);
        wrapper.override(QueryMetadata.FETCHING_DATA_ROWS_PROPERTY, Boolean.TRUE);
        return wrapper;
    }
}
