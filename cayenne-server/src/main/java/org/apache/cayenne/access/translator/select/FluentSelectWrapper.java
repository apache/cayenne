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
import org.apache.cayenne.query.FluentSelect;
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.query.QueryMetadata;

/**
 * @since 4.2
 */
public class FluentSelectWrapper implements TranslatableQueryWrapper {

    private final FluentSelect<?, ?> select;

    public FluentSelectWrapper(FluentSelect<?, ?> select) {
        this.select = Objects.requireNonNull(select);
    }

    @Override
    public boolean isDistinct() {
        return select.isDistinct();
    }

    @Override
    public QueryMetadata getMetaData(EntityResolver resolver) {
        return select.getMetaData(resolver);
    }

    @Override
    public Expression getQualifier() {
        return select.getWhere();
    }

    @Override
    public Collection<Ordering> getOrderings() {
        return select.getOrderings();
    }

    @Override
    public Collection<Property<?>> getColumns() {
        return select.getColumns();
    }

    @Override
    public Expression getHavingQualifier() {
        return select.getHaving();
    }

    @Override
    public FluentSelect<?, ?> unwrap() {
        return select;
    }
}
