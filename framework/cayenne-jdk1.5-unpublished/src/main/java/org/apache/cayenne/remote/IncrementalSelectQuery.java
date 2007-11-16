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
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.query.QueryRouter;
import org.apache.cayenne.query.SQLAction;
import org.apache.cayenne.query.SQLActionVisitor;
import org.apache.cayenne.query.SQLResultSetMapping;
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
 * @author Andrus Adamchik
 */
class IncrementalSelectQuery extends SelectQuery {

    private SelectQuery query;
    private String cacheKey;

    IncrementalSelectQuery(SelectQuery delegate, String cacheKey) {
        this.query = delegate;
        this.cacheKey = cacheKey;
    }

    public QueryMetadata getMetaData(EntityResolver resolver) {
        final QueryMetadata metadata = query.getMetaData(resolver);

        // the way paginated queries work on the server is that they are never cached
        // (IncrementalFaultList interception happens before cache interception). So
        // overriding caching settings in the metadata will only affect
        // ClientServerChannel behavior

        return new QueryMetadata() {

            public String getCacheKey() {
                return cacheKey;
            }

            public SQLResultSetMapping getResultSetMapping() {
                return metadata.getResultSetMapping();
            }

            public String[] getCacheGroups() {
                return metadata.getCacheGroups();
            }

            public String getCachePolicy() {
                return metadata.getCachePolicy();
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

            public boolean isFetchingDataRows() {
                return metadata.isFetchingDataRows();
            }

            public boolean isRefreshingObjects() {
                return metadata.isRefreshingObjects();
            }

            public boolean isResolvingInherited() {
                return metadata.isResolvingInherited();
            }
        };
    }

    public void addCustomDbAttribute(String attributePath) {
        query.addCustomDbAttribute(attributePath);
    }

    public void addCustomDbAttributes(List attrPaths) {
        query.addCustomDbAttributes(attrPaths);
    }

    public void addOrdering(Ordering ordering) {
        query.addOrdering(ordering);
    }

    public void addOrdering(String sortPathSpec, boolean isAscending, boolean ignoreCase) {
        query.addOrdering(sortPathSpec, isAscending, ignoreCase);
    }

    public void addOrdering(String sortPathSpec, boolean isAscending) {
        query.addOrdering(sortPathSpec, isAscending);
    }

    public void addOrderings(List orderings) {
        query.addOrderings(orderings);
    }

    public PrefetchTreeNode addPrefetch(String prefetchPath) {
        return query.addPrefetch(prefetchPath);
    }

    public void andQualifier(Expression e) {
        query.andQualifier(e);
    }

    public void clearOrderings() {
        query.clearOrderings();
    }

    public void clearPrefetches() {
        query.clearPrefetches();
    }

    public Query createQuery(Map parameters) {
        return query.createQuery(parameters);
    }

    public SQLAction createSQLAction(SQLActionVisitor visitor) {
        return query.createSQLAction(visitor);
    }

    public void encodeAsXML(XMLEncoder encoder) {
        query.encodeAsXML(encoder);
    }

    public boolean equals(Object obj) {
        return query.equals(obj);
    }

    public String[] getCacheGroups() {
        return query.getCacheGroups();
    }

    public String getCachePolicy() {
        return query.getCachePolicy();
    }

    public List getCustomDbAttributes() {
        return query.getCustomDbAttributes();
    }

    public int getFetchLimit() {
        return query.getFetchLimit();
    }

    public String getName() {
        return query.getName();
    }

    public List getOrderings() {
        return query.getOrderings();
    }

    public int getPageSize() {
        return query.getPageSize();
    }

    public PrefetchTreeNode getPrefetchTree() {
        return query.getPrefetchTree();
    }

    public Expression getQualifier() {
        return query.getQualifier();
    }

    public Object getRoot() {
        return query.getRoot();
    }

    public int hashCode() {
        return query.hashCode();
    }

    public void initWithProperties(Map properties) {
        query.initWithProperties(properties);
    }

    public boolean isDistinct() {
        return query.isDistinct();
    }

    public boolean isFetchingCustomAttributes() {
        return query.isFetchingCustomAttributes();
    }

    public boolean isFetchingDataRows() {
        return query.isFetchingDataRows();
    }

    public boolean isRefreshingObjects() {
        return query.isRefreshingObjects();
    }

    public boolean isResolvingInherited() {
        return query.isResolvingInherited();
    }

    public void orQualifier(Expression e) {
        query.orQualifier(e);
    }

    public SelectQuery queryWithParameters(Map parameters, boolean pruneMissing) {
        return query.queryWithParameters(parameters, pruneMissing);
    }

    public SelectQuery queryWithParameters(Map parameters) {
        return query.queryWithParameters(parameters);
    }

    public void removeOrdering(Ordering ordering) {
        query.removeOrdering(ordering);
    }

    public void removePrefetch(String prefetchPath) {
        query.removePrefetch(prefetchPath);
    }

    public void route(QueryRouter router, EntityResolver resolver, Query substitutedQuery) {
        query.route(router, resolver, substitutedQuery);
    }

    public void setCacheGroups(String[] cachGroups) {
        query.setCacheGroups(cachGroups);
    }

    public void setCachePolicy(String policy) {
        query.setCachePolicy(policy);
    }

    public void setDistinct(boolean distinct) {
        query.setDistinct(distinct);
    }

    public void setFetchingDataRows(boolean flag) {
        query.setFetchingDataRows(flag);
    }

    public void setFetchLimit(int fetchLimit) {
        query.setFetchLimit(fetchLimit);
    }

    public void setName(String name) {
        query.setName(name);
    }

    public void setPageSize(int pageSize) {
        query.setPageSize(pageSize);
    }

    public void setPrefetchTree(PrefetchTreeNode prefetchTree) {
        query.setPrefetchTree(prefetchTree);
    }

    public void setQualifier(Expression qualifier) {
        query.setQualifier(qualifier);
    }

    public void setRefreshingObjects(boolean flag) {
        query.setRefreshingObjects(flag);
    }

    public void setResolvingInherited(boolean b) {
        query.setResolvingInherited(b);
    }

    public void setRoot(Object value) {
        query.setRoot(value);
    }

    public String toString() {
        return query.toString();
    }
}
