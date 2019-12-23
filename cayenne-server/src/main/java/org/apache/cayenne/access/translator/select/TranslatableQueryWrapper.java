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
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.query.Select;

/**
 * This interface allows transparently use different queries (namely SelectQuery, ObjectSelect and ColumnSelect)
 * in translator and as subqueries.
 *
 * @since 4.2
 */
public interface TranslatableQueryWrapper {

    boolean isDistinct();

    QueryMetadata getMetaData(EntityResolver resolver);

    Expression getQualifier();

    Collection<Ordering> getOrderings();

    Collection<Property<?>> getColumns();

    Expression getHavingQualifier();

    Select<?> unwrap();

    default boolean needsResultSetMapping() {
        return getColumns() != null && !getColumns().isEmpty();
    }
}
