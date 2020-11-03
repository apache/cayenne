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
import java.util.Objects;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.property.Property;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.query.SelectQuery;

/**
 * @since 4.2
 * @deprecated this class should gone with the {@link SelectQuery}
 */
public class SelectQueryWrapper implements TranslatableQueryWrapper {

    private final SelectQuery<?> selectQuery;

    public SelectQueryWrapper(SelectQuery<?> selectQuery) {
        this.selectQuery = Objects.requireNonNull(selectQuery);
    }

    @Override
    public boolean isDistinct() {
        return selectQuery.isDistinct();
    }

    @Override
    public QueryMetadata getMetaData(EntityResolver resolver) {
        return selectQuery.getMetaData(resolver);
    }

    @Override
    public Expression getQualifier() {
        return selectQuery.getQualifier();
    }

    @Override
    public Collection<Ordering> getOrderings() {
        return selectQuery.getOrderings();
    }

    @Override
    public Collection<Property<?>> getColumns() {
        return selectQuery.getColumns();
    }

    @Override
    public Expression getHavingQualifier() {
        return selectQuery.getHavingQualifier();
    }

    @Override
    public SelectQuery<?> unwrap() {
        return selectQuery;
    }
}
