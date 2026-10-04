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
package org.apache.cayenne.map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.project.ProjectNodeVisitor;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.ql.QLSelectPrinter;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.query.QueryCacheStrategy;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.util.XMLEncoder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @since 4.0
 */
public class SelectQueryDescriptor extends QueryDescriptor {

    protected Expression qualifier;
    protected List<Ordering> orderings = new ArrayList<>();
    protected Map<String, Integer> prefetchesMap = new HashMap<>();
    protected int fetchLimit = QueryMetadata.FETCH_LIMIT_DEFAULT;
    protected int fetchOffset = QueryMetadata.FETCH_OFFSET_DEFAULT;
    protected boolean distinct;

    public boolean isDistinct() {
        return distinct;
    }

    public void setDistinct(boolean distinct) {
        this.distinct = distinct;
    }

    /**
     * Returns the fetch limit of the query. Zero means no limit.
     *
     * @since 5.0
     */
    public int getFetchLimit() {
        return fetchLimit;
    }

    /**
     * @since 5.0
     */
    public void setFetchLimit(int fetchLimit) {
        this.fetchLimit = fetchLimit;
    }

    /**
     * Returns the fetch offset of the query.
     *
     * @since 5.0
     */
    public int getFetchOffset() {
        return fetchOffset;
    }

    /**
     * @since 5.0
     */
    public void setFetchOffset(int fetchOffset) {
        this.fetchOffset = fetchOffset;
    }

    /**
     * Returns qualifier of this query.
     */
    public Expression getQualifier() {
        return qualifier;
    }

    /**
     * Sets qualifier for this query.
     */
    public void setQualifier(Expression qualifier) {
        this.qualifier = qualifier;
    }

    /**
     * Returns list of orderings for this query.
     */
    public List<Ordering> getOrderings() {
        return orderings;
    }

    /**
     * Sets list of orderings for this query.
     */
    public void setOrderings(List<Ordering> orderings) {
        this.orderings = orderings;
    }

    /**
     * Adds single ordering for this query.
     */
    public void addOrdering(Ordering ordering) {
        this.orderings.add(ordering);
    }

    /**
     * Removes single ordering from this query.
     */
    public void removeOrdering(Ordering ordering) {
        this.orderings.remove(ordering);
    }

    /**
     * Returns map of prefetch paths with semantics for this query.
     *
     * @since 4.1
     */
    public Map<String, Integer> getPrefetchesMap() {
        return prefetchesMap;
    }

    /**
     * Sets map of prefetch paths with semantics for this query.
     *
     * @since 4.1
     */
    public void setPrefetchesMap(HashMap<String, Integer> prefetchesMap){
        this.prefetchesMap = prefetchesMap;
    }

    /**
     * Adds prefetch path with semantics to this query.
     *
     * @since 4.1
     */
    public void addPrefetch(String prefetchPath, int semantics){
        this.prefetchesMap.put(prefetchPath, semantics);
    }

    /**
     * Removes single prefetch path from this query.
     */
    public void removePrefetch(String prefetchPath) {
        this.prefetchesMap.remove(prefetchPath);
    }

    @Override
    public ObjectSelect<?> buildQuery() {
        return buildQuery(getQualifier());
    }

    /**
     * @since 5.0
     */
    @Override
    public ObjectSelect<?> buildQuery(Map<String, ?> parameters) {
        Expression qualifier = getQualifier();
        return buildQuery(qualifier != null ? qualifier.params(parameters, true) : null);
    }

    private ObjectSelect<?> buildQuery(Expression qualifier) {
        // resolve root
        Object root = getRoot();
        String rootEntityName;
        if(root instanceof ObjEntity) {
            rootEntityName = ((ObjEntity) root).getName();
        } else if(root instanceof String) {
            rootEntityName = (String)root;
        } else {
            throw new CayenneRuntimeException("Unexpected root for the SelectQueryDescriptor '%s'.", root);
        }

        ObjectSelect<?> query = ObjectSelect.query(Object.class).where(qualifier);
        query.entityName(rootEntityName);

        List<Ordering> orderings = this.getOrderings();
        if (orderings != null && !orderings.isEmpty()) {
            query.orderBy(orderings);
        }

        if (prefetchesMap != null) {
            prefetchesMap.forEach(query::prefetch);
        }

        query.limit(fetchLimit)
                .offset(fetchOffset)
                .pageSize(pageSize)
                .statementFetchSize(statementFetchSize)
                .cacheStrategy(cacheStrategy, cacheGroup);

        if (fetchingDataRows) {
            query.fetchDataRows();
        }

        if (distinct) {
            query.distinct();
        }

        return query;
    }

    /**
     * Returns this query as a String in the syntax of {@link ObjectSelect#parse(String, Object...)}, or null if
     * the query has no root entity, and hence can't be printed.
     *
     * @since 5.0
     */
    public String toQueryString() {
        return rootEntityName() != null ? QLSelectPrinter.print(buildQuery(getQualifier())) : null;
    }

    private String rootEntityName() {
        return switch (root) {
            case ObjEntity entity -> entity.getName();
            case String name -> name;
            case null, default -> null;
        };
    }

    @Override
    public void encodeAsXML(XMLEncoder encoder, ProjectNodeVisitor delegate) {
        // the root, qualifier, orderings, prefetches, limit, offset and distinct are all clauses of the query String,
        // the rest of the properties are stored separately
        encoder.start("objectQuery")
                .attribute("name", getName())
                .attribute("cacheStrategy", cacheStrategy != QueryCacheStrategy.NO_CACHE ? cacheStrategy.name() : null)
                .attribute("dataRows", fetchingDataRows)
                .attribute("pageSize", pageSize)
                .attribute("statementFetchSize", statementFetchSize);

        String select = toQueryString();
        if (select != null) {
            encoder.start("ql").cdata(select, true).end();
        }

        encodeCacheGroup(encoder);

        delegate.visitQuery(this);
        encoder.end();
    }
}
