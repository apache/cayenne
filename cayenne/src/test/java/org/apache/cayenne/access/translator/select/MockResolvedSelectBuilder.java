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

package org.apache.cayenne.access.translator.select;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.property.Property;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.query.QueryCacheStrategy;
import org.apache.cayenne.query.ResultSegments;
import org.apache.cayenne.query.resolved.ResolvedQuery;
import org.apache.cayenne.query.resolved.ResolvedSelect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Builds a {@link ResolvedSelect} straight from its parts, without an EntityResolver, for the translation stage tests.
 */
class MockResolvedSelectBuilder {

    private ObjEntity objEntity;
    private DbEntity dbEntity;
    private int limit;
    private int offset;
    private boolean suppressDistinct;
    private ResultSegments resultSetMapping;
    private boolean distinct;
    private Expression where;
    private Expression having;
    private Collection<Ordering> orderings;
    private Collection<Property<?>> columns;

    MockResolvedSelectBuilder withDbEntity(DbEntity entity) {
        this.dbEntity = entity;
        return this;
    }

    MockResolvedSelectBuilder withObjEntity(ObjEntity entity) {
        this.objEntity = entity;
        return this;
    }

    MockResolvedSelectBuilder withLimitOffset(int limit, int offset) {
        this.limit = limit;
        this.offset = offset;
        return this;
    }

    MockResolvedSelectBuilder withSuppressDistinct() {
        this.suppressDistinct = true;
        return this;
    }

    MockResolvedSelectBuilder withResultSetMapping(ResultSegments resultSetMapping) {
        this.resultSetMapping = resultSetMapping;
        return this;
    }

    MockResolvedSelectBuilder withDistinct(boolean distinct) {
        this.distinct = distinct;
        return this;
    }

    MockResolvedSelectBuilder withWhere(Expression where) {
        this.where = where;
        return this;
    }

    MockResolvedSelectBuilder withOrderings(Collection<Ordering> orderings) {
        this.orderings = orderings;
        return this;
    }

    MockResolvedSelectBuilder withColumns(Collection<Property<?>> columns) {
        this.columns = columns;
        return this;
    }

    MockResolvedSelectBuilder withHaving(Expression having) {
        this.having = having;
        return this;
    }

    ResolvedSelect build() {
        return new ResolvedSelect(null, objEntity, dbEntity, dbEntity != null ? dbEntity.getDataMap() : null, null,
                where, having, orderings != null ? new ArrayList<>(orderings) : List.of(),
                columns != null ? new ArrayList<>(columns) : List.of(), distinct, suppressDistinct, false, limit,
                offset, ResolvedQuery.PAGE_SIZE_DEFAULT, ResolvedQuery.STATEMENT_FETCH_SIZE_DEFAULT,
                ResolvedQuery.QUERY_TIMEOUT_DEFAULT, QueryCacheStrategy.NO_CACHE, null, null, null, Map.of(),
                resultSetMapping, null, List.of(), List.of());
    }
}
