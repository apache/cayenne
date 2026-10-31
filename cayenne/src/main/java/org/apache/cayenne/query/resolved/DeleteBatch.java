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

package org.apache.cayenne.query.resolved;

import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * A batch of deletes of the rows of a single DbEntity, identified by their qualifier attributes.
 *
 * @since 6.0
 */
public record DeleteBatch(
        DbEntity dbEntity,
        List<DbAttribute> qualifierAttributes,
        Set<String> nullQualifierNames,
        boolean usingOptimisticLocking,
        List<BatchRow> rows) implements ResolvedBatch {

    public DeleteBatch(
            DbEntity dbEntity,
            List<DbAttribute> qualifierAttributes,
            Collection<String> nullQualifierNames,
            boolean usingOptimisticLocking,
            List<BatchRow> rows) {
        this(dbEntity, qualifierAttributes, Set.copyOf(nullQualifierNames), usingOptimisticLocking, rows);
    }

    @Override
    public List<DbAttribute> dbAttributes() {
        return qualifierAttributes;
    }

    /**
     * Returns true if a given qualifier attribute is matched against NULL (with "IS NULL") instead of a bound value.
     */
    public boolean isNull(DbAttribute attribute) {
        return nullQualifierNames.contains(attribute.getName());
    }

    @Override
    public Object value(BatchRow row, int attributeIndex) {
        return row.qualifier().get(qualifierAttributes.get(attributeIndex).getName());
    }
}
