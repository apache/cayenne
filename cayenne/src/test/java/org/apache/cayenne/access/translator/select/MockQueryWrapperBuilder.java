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
import org.apache.cayenne.exp.property.BaseProperty;
import org.apache.cayenne.exp.property.Property;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.MockQueryMetadata;
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.query.Select;

/**
 * @since 4.2
 */
class MockQueryWrapperBuilder {

    private boolean distinct;

    private QueryMetadata metaData;

    private PrefetchTreeNode prefetchTreeNode;

    private Expression qualifier;

    private Collection<Ordering> orderings;

    private Collection<Property<?>> columns;

    private Expression havingQualifier;

    private Select<?> mockSelect;
    private boolean needsResultSetMapping;

    MockQueryWrapperBuilder withDistinct(boolean distinct) {
        this.distinct = distinct;
        return this;
    }

    MockQueryWrapperBuilder withMetaData(QueryMetadata metaData) {
        this.metaData = metaData;
        return this;
    }

    MockQueryWrapperBuilder withPrefetchTreeNode(PrefetchTreeNode prefetchTreeNode) {
        this.prefetchTreeNode = prefetchTreeNode;
        return this;
    }

    MockQueryWrapperBuilder withQualifier(Expression qualifier) {
        this.qualifier = qualifier;
        return this;
    }

    MockQueryWrapperBuilder withOrderings(Collection<Ordering> orderings) {
        this.orderings = orderings;
        return this;
    }

    MockQueryWrapperBuilder withColumns(Collection<Property<?>> columns) {
        this.columns = columns;
        return this;
    }

    MockQueryWrapperBuilder withHavingQualifier(Expression havingQualifier) {
        this.havingQualifier = havingQualifier;
        return this;
    }

    MockQueryWrapperBuilder withSelect(Select<?> select) {
        this.mockSelect = select;
        return this;
    }

    MockQueryWrapperBuilder withNeedsResultSetMapping(boolean needsResultSetMapping) {
        this.needsResultSetMapping = needsResultSetMapping;
        return this;
    }

    TranslatableQueryWrapper build() {
        return new TranslatableQueryWrapper() {
            @Override
            public boolean isDistinct() {
                return distinct;
            }

            @Override
            public QueryMetadata getMetaData(EntityResolver resolver) {
                return metaData != null ? metaData : new MockQueryMetadata();
            }

            @Override
            public Expression getQualifier() {
                return qualifier;
            }

            @Override
            public Collection<Ordering> getOrderings() {
                return orderings;
            }

            @Override
            public Collection<Property<?>> getColumns() {
                return columns;
            }

            @Override
            public Expression getHavingQualifier() {
                return havingQualifier;
            }

            @Override
            public Select<?> unwrap() {
                return mockSelect;
            }

            @Override
            public boolean needsResultSetMapping() {
                return needsResultSetMapping;
            }
        };
    }
}
