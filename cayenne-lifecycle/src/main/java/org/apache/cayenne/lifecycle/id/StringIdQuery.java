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
package org.apache.cayenne.lifecycle.id;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.configuration.ConfigurationNodeVisitor;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.QueryCacheStrategy;
import org.apache.cayenne.query.QueryChain;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.query.QueryRouter;
import org.apache.cayenne.query.SQLAction;
import org.apache.cayenne.query.SQLActionVisitor;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.reflect.ClassDescriptor;

/**
 * A query that allows to fetch objects based on one or more String IDs. The returned
 * objects do not have to be of the same type, be related via inheritance, or come from
 * the same DB table. Note that if you expect multiple types of objects, use
 * {@link ObjectContext#performGenericQuery(Query)}. The returned QueryResponse will
 * contain separate lists of DataRows for each type in no particular order.
 * <p>
 * As of this writing, a limitation of this query is that it returns DataRows that need to
 * be manually converted to objects if needed. In that it is similar to {@link QueryChain}.
 * 
 * @since 3.1
 */
public class StringIdQuery implements Query {

    private static Collection<String> toCollection(String... stringIds) {

        if (stringIds == null) {
            throw new NullPointerException("Null stringIds");
        }

        return Arrays.asList(stringIds);
    }

    protected String name;
    protected DataMap dataMap;
    protected Collection<String> stringIds;

    protected transient Map<String, SelectQuery> idQueriesByEntity;

    public StringIdQuery(String... stringIds) {
        this(toCollection(stringIds));
    }

    public StringIdQuery(Collection<String> stringIds) {
        // using a Set to ensure that duplicates do not result in a longer invariant
        // qualifier
        this.stringIds = new HashSet<String>(stringIds);
    }

    public Collection<String> getStringIds() {
        return stringIds;
    }

    public void addStringIds(String... ids) {
        if (ids == null) {
            throw new NullPointerException("Null ids");
        }

        boolean changed = false;
        for (String id : ids) {
            if (stringIds.add(id)) {
                changed = true;
            }
        }

        if (changed) {
            idQueriesByEntity = null;
        }
    }

    protected Map<String, SelectQuery> getIdQueriesByEntity(EntityResolver resolver) {
        if (this.idQueriesByEntity == null) {

            Map<String, SelectQuery> idQueriesByEntity = new HashMap<String, SelectQuery>();
            Map<String, EntityIdCoder> codersByEntity = new HashMap<String, EntityIdCoder>();

            for (String id : stringIds) {
                String entityName = EntityIdCoder.getEntityName(id);
                EntityIdCoder coder = codersByEntity.get(entityName);
                SelectQuery query;

                if (coder == null) {
                    coder = new EntityIdCoder(resolver.getObjEntity(entityName));

                    query = new SelectQuery(entityName);

                    codersByEntity.put(entityName, coder);
                    idQueriesByEntity.put(entityName, query);
                }
                else {
                    query = idQueriesByEntity.get(entityName);
                }

                Expression idExp = ExpressionFactory.matchAllDbExp(coder
                        .toObjectId(id)
                        .getIdSnapshot(), Expression.EQUAL_TO);
                query.orQualifier(idExp);
            }

            this.idQueriesByEntity = idQueriesByEntity;
        }

        return this.idQueriesByEntity;
    }

    public <T> T acceptVisitor(ConfigurationNodeVisitor<T> visitor) {
        return visitor.visitQuery(this);
    }

    public QueryMetadata getMetaData(EntityResolver resolver) {

        // Cayenne doesn't know how to handle multiple root entities, so this
        // QueryMetadata, just like QueryChain's metadata is not very precise and won't
        // result in correct PersistentObjects...
        return new QueryMetadata() {

            public DataMap getDataMap() {
                return null;
            }

            public List<Object> getResultSetMapping() {
                return null;
            }

            public Query getOrginatingQuery() {
                return null;
            }

            public QueryCacheStrategy getCacheStrategy() {
                return QueryCacheStrategy.getDefaultStrategy();
            }

            public DbEntity getDbEntity() {
                return null;
            }

            public ObjEntity getObjEntity() {
                return null;
            }

            public ClassDescriptor getClassDescriptor() {
                return null;
            }

            public Procedure getProcedure() {
                return null;
            }

            public String getCacheKey() {
                return null;
            }

            public String[] getCacheGroups() {
                return null;
            }

            public boolean isFetchingDataRows() {
                // overriding this... Can't fetch objects until DataDomainQueryAction
                // starts converting multiple ResultSets to object... Same as QueryChain
                // essentially.
                return true;
            }

            public boolean isRefreshingObjects() {
                return true;
            }

            public int getPageSize() {
                return QueryMetadata.PAGE_SIZE_DEFAULT;
            }

            public int getFetchOffset() {
                return -1;
            }

            public int getFetchLimit() {
                return QueryMetadata.FETCH_LIMIT_DEFAULT;
            }

            public PrefetchTreeNode getPrefetchTree() {
                return null;
            }

            public Map<String, String> getPathSplitAliases() {
                return Collections.emptyMap();
            }

            public int getStatementFetchSize() {
                return QueryMetadata.STATEMENT_FETCH_SIZE_DEFAULT;
            }
        };
    }

    public void route(QueryRouter router, EntityResolver resolver, Query substitutedQuery) {

        Map<String, SelectQuery> queries = getIdQueriesByEntity(resolver);
        for (SelectQuery query : queries.values()) {
            query.route(router, resolver, this);
        }
    }

    public SQLAction createSQLAction(SQLActionVisitor visitor) {
        throw new UnsupportedOperationException(
                "This query was supposed to be replace with a set of SelectQueries during the route phase");
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DataMap getDataMap() {
        return dataMap;
    }

    public void setDataMap(DataMap dataMap) {
        this.dataMap = dataMap;
    }
}
