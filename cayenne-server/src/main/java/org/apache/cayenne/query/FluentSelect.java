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

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ResultBatchIterator;
import org.apache.cayenne.ResultIterator;
import org.apache.cayenne.ResultIteratorCallback;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.property.Property;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;

/**
 * Base class for {@link ObjectSelect} and {@link ColumnSelect}
 *
 * @since 4.0
 */
public abstract class FluentSelect<T> extends AbstractQuery implements Select<T> {

    // root
    protected Class<?> entityType;
    protected String entityName;
    protected String dbEntityName;

    protected Expression where;
    protected Expression having;
    boolean havingExpressionIsActive = false;

    protected Collection<Ordering> orderings;
    boolean distinct;

    protected FluentSelect() {
    }

    protected Object resolveRoot(EntityResolver resolver) {
        Object root;
        if (entityType != null) {
            root = entityType;
        } else if (entityName != null) {

            ObjEntity entity = resolver.getObjEntity(entityName);
            if (entity == null) {
                throw new CayenneRuntimeException("Unrecognized ObjEntity name: %s", entityName);
            }

            root = entity;
        } else if (dbEntityName != null) {

            DbEntity entity = resolver.getDbEntity(dbEntityName);
            if (entity == null) {
                throw new CayenneRuntimeException("Unrecognized DbEntity name: %s", dbEntityName);
            }

            root = entity;
        } else {
            throw new CayenneRuntimeException("Undefined root entity of the query");
        }
        return root;
    }

    public int getStatementFetchSize() {
        return getBaseMetaData().getStatementFetchSize();
    }

    /**
     * @since 4.2
     */
    public int getQueryTimeout() {
        return getBaseMetaData().getQueryTimeout();
    }

    public int getPageSize() {
        return getBaseMetaData().getPageSize();
    }

    public int getLimit() {
        return getBaseMetaData().getFetchLimit();
    }

    public int getOffset() {
        return getBaseMetaData().getFetchOffset();
    }

    public Class<?> getEntityType() {
        return entityType;
    }

    public String getEntityName() {
        return entityName;
    }

    public String getDbEntityName() {
        return dbEntityName;
    }

    /**
     * Returns a WHERE clause Expression of this query.
     */
    public Expression getWhere() {
        return where;
    }

    /**
     * Returns a HAVING clause Expression of this query.
     */
    public Expression getHaving() {
        return having;
    }

    public Collection<Ordering> getOrderings() {
        return orderings;
    }

    public PrefetchTreeNode getPrefetches() {
        return getBaseMetaData().getPrefetchTree();
    }

    void setActiveExpression(Expression exp) {
        if(havingExpressionIsActive) {
            having = exp;
        } else {
            where = exp;
        }
    }

    Expression getActiveExpression() {
        if(havingExpressionIsActive) {
            return having;
        } else {
            return where;
        }
    }

    @Override
    public List<T> select(ObjectContext context) {
        return context.select(this);
    }

    @Override
    public T selectOne(ObjectContext context) {
        return context.selectOne(this);
    }

    @Override
    public void iterate(ObjectContext context, ResultIteratorCallback<T> callback) {
        context.iterate(this, callback);
    }

    @Override
    public ResultIterator<T> iterator(ObjectContext context) {
        return context.iterator(this);
    }

    @Override
    public ResultBatchIterator<T> batchIterator(ObjectContext context, int size) {
        return context.batchIterator(this, size);
    }

    @Override
    public SQLAction createSQLAction(SQLActionVisitor visitor) {
        return visitor.objectSelectAction(this);
    }

    @Override
    public void route(QueryRouter router, EntityResolver resolver, Query substitutedQuery) {
        super.route(router, resolver, substitutedQuery);

        // suppress prefetches for paginated queries.. instead prefetches will be resolved per row...
        if (getPageSize() <= 0) {
            routePrefetches(router, resolver);
        }
    }

    public boolean isFetchingDataRows() {
        return false;
    }

    protected void routePrefetches(QueryRouter router, EntityResolver resolver) {
        new FluentSelectPrefetchRouterAction().route(this, router, resolver);
    }

    /**
     * @since 4.2
     */
    public Collection<Property<?>> getColumns() {
        return null;
    }

    /**
     * @since 4.2
     */
    public boolean isDistinct() {
        return distinct;
    }

    /**
     * @since 4.2
     */
    public void initWithProperties(Map<String, String> properties) {
        getBaseMetaData().initWithProperties(properties);
    }
}
