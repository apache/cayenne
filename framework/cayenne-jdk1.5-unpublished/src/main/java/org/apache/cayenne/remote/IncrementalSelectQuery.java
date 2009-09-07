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
package org.apache.cayenne.remote;

import java.util.List;
import java.util.Map;

import org.apache.cayenne.access.IncrementalFaultList;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.QueryCacheStrategy;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.query.QueryRouter;
import org.apache.cayenne.query.SQLAction;
import org.apache.cayenne.query.SQLActionVisitor;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.util.XMLEncoder;

/**
 * A SelectQuery decorator that overrides the metadata to ensure that query result is
 * cached on the server, so that subranges could be retrieved at a later time. Note that a
 * special decorator that is a subclass of SelectQuery is needed so that
 * {@link IncrementalFaultList} on the server-side could apply SelectQuery-specific
 * optimizations.
 * 
 * @since 3.0
 */
class IncrementalSelectQuery extends SelectQuery {

    private SelectQuery query;
    private String cacheKey;

    IncrementalSelectQuery(SelectQuery delegate, String cacheKey) {
        this.query = delegate;
        this.cacheKey = cacheKey;
    }

    @Override
    public QueryMetadata getMetaData(EntityResolver resolver) {
        final QueryMetadata metadata = query.getMetaData(resolver);

        // the way paginated queries work on the server is that they are never cached
        // (IncrementalFaultList interception happens before cache interception). So
        // overriding caching settings in the metadata will only affect
        // ClientServerChannel behavior

        return new QueryMetadata() {

            public Query getOrginatingQuery() {
                return null;
            }

            public String getCacheKey() {
                return cacheKey;
            }

            public List<Object> getResultSetMapping() {
                return metadata.getResultSetMapping();
            }

            public String[] getCacheGroups() {
                return metadata.getCacheGroups();
            }

            /**
             * @deprecated since 3.0 in favor of 'getCacheStrategy'.
             */
            @Deprecated
            public String getCachePolicy() {
                return metadata.getCachePolicy();
            }

            public QueryCacheStrategy getCacheStrategy() {
                return metadata.getCacheStrategy();
            }

            public DataMap getDataMap() {
                return metadata.getDataMap();
            }

            public DbEntity getDbEntity() {
                return metadata.getDbEntity();
            }

            public int getFetchLimit() {
                return metadata.getFetchLimit();
            }

            public int getFetchOffset() {
                return metadata.getFetchOffset();
            }

            /**
             * @deprecated since 3.0
             */
            @Deprecated
            public int getFetchStartIndex() {
                return metadata.getFetchStartIndex();
            }

            public ObjEntity getObjEntity() {
                return metadata.getObjEntity();
            }

            public ClassDescriptor getClassDescriptor() {
                return metadata.getClassDescriptor();
            }

            public int getPageSize() {
                return metadata.getPageSize();
            }

            public PrefetchTreeNode getPrefetchTree() {
                return metadata.getPrefetchTree();
            }

            public Procedure getProcedure() {
                return metadata.getProcedure();
            }

            public Map<String, String> getPathSplitAliases() {
                return metadata.getPathSplitAliases();
            }

            public boolean isFetchingDataRows() {
                return metadata.isFetchingDataRows();
            }

            public boolean isRefreshingObjects() {
                return metadata.isRefreshingObjects();
            }

            public boolean isResolvingInherited() {
                return metadata.isResolvingInherited();
            }

            public int getStatementFetchSize() {
                return metadata.getStatementFetchSize();
            }
        };
    }

    @Override
    public void addOrdering(Ordering ordering) {
        query.addOrdering(ordering);
    }

    @Override
    public void addOrdering(String sortPathSpec, boolean isAscending, boolean ignoreCase) {
        query.addOrdering(sortPathSpec, isAscending, ignoreCase);
    }

    @Override
    public void addOrdering(String sortPathSpec, boolean isAscending) {
        query.addOrdering(sortPathSpec, isAscending);
    }

    @Override
    public void addOrderings(List orderings) {
        query.addOrderings(orderings);
    }

    @Override
    public PrefetchTreeNode addPrefetch(String prefetchPath) {
        return query.addPrefetch(prefetchPath);
    }

    @Override
    public void andQualifier(Expression e) {
        query.andQualifier(e);
    }

    @Override
    public void clearOrderings() {
        query.clearOrderings();
    }

    @Override
    public void clearPrefetches() {
        query.clearPrefetches();
    }

    @Override
    public Query createQuery(Map parameters) {
        return query.createQuery(parameters);
    }

    @Override
    public SQLAction createSQLAction(SQLActionVisitor visitor) {
        return query.createSQLAction(visitor);
    }

    @Override
    public void encodeAsXML(XMLEncoder encoder) {
        query.encodeAsXML(encoder);
    }

    @Override
    public boolean equals(Object obj) {
        return query.equals(obj);
    }

    @Override
    public String[] getCacheGroups() {
        return query.getCacheGroups();
    }

    @Override
    public String getCachePolicy() {
        return query.getCachePolicy();
    }

    @Override
    public int getFetchLimit() {
        return query.getFetchLimit();
    }

    @Override
    public String getName() {
        return query.getName();
    }

    @Override
    public List<Ordering> getOrderings() {
        return query.getOrderings();
    }

    @Override
    public int getPageSize() {
        return query.getPageSize();
    }

    @Override
    public PrefetchTreeNode getPrefetchTree() {
        return query.getPrefetchTree();
    }

    @Override
    public Expression getQualifier() {
        return query.getQualifier();
    }

    @Override
    public Object getRoot() {
        return query.getRoot();
    }

    @Override
    public int hashCode() {
        return query.hashCode();
    }

    @Override
    public void initWithProperties(Map properties) {
        query.initWithProperties(properties);
    }

    @Override
    public boolean isDistinct() {
        return query.isDistinct();
    }

    @Override
    public boolean isFetchingDataRows() {
        return query.isFetchingDataRows();
    }

    /**
     * @deprecated since 3.0
     */
    @Deprecated
    @Override
    public boolean isRefreshingObjects() {
        return query.isRefreshingObjects();
    }

    /**
     * @deprecated since 3.0. Inheritance resolving is not optional anymore.
     */
    @Deprecated
    @Override
    public boolean isResolvingInherited() {
        return query.isResolvingInherited();
    }

    @Override
    public void orQualifier(Expression e) {
        query.orQualifier(e);
    }

    @Override
    public SelectQuery queryWithParameters(Map parameters, boolean pruneMissing) {
        return query.queryWithParameters(parameters, pruneMissing);
    }

    @Override
    public SelectQuery queryWithParameters(Map parameters) {
        return query.queryWithParameters(parameters);
    }

    @Override
    public void removeOrdering(Ordering ordering) {
        query.removeOrdering(ordering);
    }

    @Override
    public void removePrefetch(String prefetchPath) {
        query.removePrefetch(prefetchPath);
    }

    @Override
    public void route(QueryRouter router, EntityResolver resolver, Query substitutedQuery) {
        query.route(router, resolver, substitutedQuery);
    }

    @Override
    public void setCacheGroups(String[] cachGroups) {
        query.setCacheGroups(cachGroups);
    }

    @Override
    public void setCachePolicy(String policy) {
        query.setCachePolicy(policy);
    }

    @Override
    public void setDistinct(boolean distinct) {
        query.setDistinct(distinct);
    }

    @Override
    public void setFetchingDataRows(boolean flag) {
        query.setFetchingDataRows(flag);
    }

    @Override
    public void setFetchLimit(int fetchLimit) {
        query.setFetchLimit(fetchLimit);
    }

    @Override
    public void setName(String name) {
        query.setName(name);
    }

    @Override
    public void setPageSize(int pageSize) {
        query.setPageSize(pageSize);
    }

    @Override
    public void setPrefetchTree(PrefetchTreeNode prefetchTree) {
        query.setPrefetchTree(prefetchTree);
    }

    @Override
    public void setQualifier(Expression qualifier) {
        query.setQualifier(qualifier);
    }

    /**
     * @deprecated since 3.0 as wrapped method is also deprecated.
     */
    @Deprecated
    @Override
    public void setRefreshingObjects(boolean flag) {
        query.setRefreshingObjects(flag);
    }

    /**
     * @deprecated since 3.0 as wrapped method is also deprecated.
     */
    @Deprecated
    @Override
    public void setResolvingInherited(boolean b) {
        query.setResolvingInherited(b);
    }

    @Override
    public void setRoot(Object value) {
        query.setRoot(value);
    }

    @Override
    public String toString() {
        return query.toString();
    }
}
