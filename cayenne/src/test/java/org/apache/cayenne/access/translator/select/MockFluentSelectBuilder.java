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

import java.util.Collection;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.property.Property;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.FluentSelect;
import org.apache.cayenne.query.MockQueryMetadata;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.query.QueryMetadata;

/**
 * Builds a {@link FluentSelect} whose metadata is supplied directly, so that translation stages can be unit-tested
 * without a real {@link EntityResolver} behind the query.
 */
class MockFluentSelectBuilder {

    private boolean distinct;

    private QueryMetadata queryMetadata;

    private Expression where;

    private Collection<Ordering> orderings;

    private Collection<Property<?>> queryColumns;

    private Expression having;

    MockFluentSelectBuilder withDistinct(boolean distinct) {
        this.distinct = distinct;
        return this;
    }

    MockFluentSelectBuilder withMetaData(QueryMetadata metaData) {
        this.queryMetadata = metaData;
        return this;
    }

    MockFluentSelectBuilder withWhere(Expression where) {
        this.where = where;
        return this;
    }

    MockFluentSelectBuilder withOrderings(Collection<Ordering> orderings) {
        this.orderings = orderings;
        return this;
    }

    MockFluentSelectBuilder withColumns(Collection<Property<?>> columns) {
        this.queryColumns = columns;
        return this;
    }

    MockFluentSelectBuilder withHaving(Expression having) {
        this.having = having;
        return this;
    }

    FluentSelect<?, ?> build() {
        return new ObjectSelect<>() {
            {
                this.distinct = MockFluentSelectBuilder.this.distinct;
                this.where = MockFluentSelectBuilder.this.where;
                this.having = MockFluentSelectBuilder.this.having;
                this.orderings = MockFluentSelectBuilder.this.orderings;
            }

            @Override
            public QueryMetadata getMetaData(EntityResolver resolver) {
                return queryMetadata != null ? queryMetadata : new MockQueryMetadata();
            }

            @Override
            public Collection<Property<?>> getColumns() {
                return queryColumns;
            }
        };
    }
}
