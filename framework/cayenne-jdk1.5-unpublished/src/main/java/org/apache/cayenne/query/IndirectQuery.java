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

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.configuration.ConfigurationNodeVisitor;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.EntityResolver;

/**
 * A convenience superclass of the queries that resolve into some other queries during the
 * routing phase. Provides caching of a replacement query.
 * 
 * @since 1.2
 */
public abstract class IndirectQuery implements Query {

    protected String name;

    /**
     * @since 3.1
     */
    protected DataMap dataMap;

    protected transient Query replacementQuery;

    protected transient EntityResolver lastResolver;

    /**
     * @since 3.1
     */
    public <T> T acceptVisitor(ConfigurationNodeVisitor<T> visitor) {
        return visitor.visitQuery(this);
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
     * Returns the metadata obtained from the replacement query.
     */
    public QueryMetadata getMetaData(EntityResolver resolver) {
        return getReplacementQuery(resolver).getMetaData(resolver);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Delegates routing to a replacement query.
     */
    public void route(QueryRouter router, EntityResolver resolver, Query substitutedQuery) {
        getReplacementQuery(resolver).route(
                router,
                resolver,
                substitutedQuery != null ? substitutedQuery : this);
    }

    /**
     * Creates a substitute query. An implementor is free to provide an arbitrary
     * replacement query.
     */
    protected abstract Query createReplacementQuery(EntityResolver resolver);

    /**
     * Returns a replacement query, creating it on demand and caching it for reuse.
     */
    protected Query getReplacementQuery(EntityResolver resolver) {
        if (replacementQuery == null || lastResolver != resolver) {
            this.replacementQuery = createReplacementQuery(resolver);
            this.lastResolver = resolver;
        }

        return replacementQuery;
    }

    /**
     * Throws an exception as indirect query should not be executed directly.
     */
    public SQLAction createSQLAction(SQLActionVisitor visitor) {
        throw new CayenneRuntimeException(this.getClass().getName()
                + " is an indirect query and doesn't support its own sql actions. "
                + "It should've been delegated to another "
                + "query during resolution or routing phase.");
    }
}
