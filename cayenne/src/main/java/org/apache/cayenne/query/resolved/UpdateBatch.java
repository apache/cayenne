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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * A batch of updates of the rows of a single DbEntity. The updated attributes are bound first, followed by the
 * qualifier attributes, so the parameter index tells which of the two row snapshots a value comes from.
 *
 * @since 6.0
 */
public record UpdateBatch(
        DbEntity dbEntity,
        List<DbAttribute> dbAttributes,
        List<DbAttribute> qualifierAttributes,
        List<DbAttribute> updatedAttributes,
        Set<String> nullQualifierNames,
        boolean usingOptimisticLocking,
        List<BatchRow> rows) implements ResolvedBatch {

    public UpdateBatch(
            DbEntity dbEntity,
            List<DbAttribute> qualifierAttributes,
            List<DbAttribute> updatedAttributes,
            Collection<String> nullQualifierNames,
            boolean usingOptimisticLocking,
            List<BatchRow> rows) {
        this(dbEntity, concat(updatedAttributes, qualifierAttributes), qualifierAttributes, updatedAttributes,
                Set.copyOf(nullQualifierNames), usingOptimisticLocking, rows);
    }

    private static List<DbAttribute> concat(List<DbAttribute> updated, List<DbAttribute> qualifier) {
        List<DbAttribute> all = new ArrayList<>(updated.size() + qualifier.size());
        all.addAll(updated);
        all.addAll(qualifier);
        return all;
    }

    /**
     * Returns true if a given qualifier attribute is matched against NULL (with "IS NULL") instead of a bound value.
     */
    public boolean isNull(DbAttribute attribute) {
        return nullQualifierNames.contains(attribute.getName());
    }

    @Override
    public Object value(BatchRow row, int attributeIndex) {
        return (attributeIndex < updatedAttributes.size() ? row.values() : row.qualifier())
                .get(dbAttributes.get(attributeIndex).getName());
    }
}
