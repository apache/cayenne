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

import org.apache.cayenne.map.compiled.CompiledColumn;
import org.apache.cayenne.map.compiled.CompiledTable;

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
        CompiledTable table,
        List<CompiledColumn> dbAttributes,
        List<CompiledColumn> qualifierAttributes,
        List<CompiledColumn> updatedAttributes,
        Set<String> nullQualifierNames,
        boolean usingOptimisticLocking,
        List<BatchRow> rows) implements ResolvedBatch {

    public UpdateBatch(
            CompiledTable table,
            List<CompiledColumn> qualifierColumns,
            List<CompiledColumn> updatedColumns,
            Collection<String> nullQualifierNames,
            boolean usingOptimisticLocking,
            List<BatchRow> rows) {
        this(table, concat(updatedColumns, qualifierColumns), qualifierColumns, updatedColumns,
                Set.copyOf(nullQualifierNames), usingOptimisticLocking, rows);
    }

    private static List<CompiledColumn> concat(List<CompiledColumn> updated,
                                                    List<CompiledColumn> qualifier) {
        List<CompiledColumn> all = new ArrayList<>(updated.size() + qualifier.size());
        all.addAll(updated);
        all.addAll(qualifier);
        return all;
    }

    /**
     * Returns true if a given qualifier attribute is matched against NULL (with "IS NULL") instead of a bound value.
     */
    public boolean isNull(CompiledColumn column) {
        return nullQualifierNames.contains(column.name());
    }

    @Override
    public Object value(BatchRow row, int attributeIndex) {
        return (attributeIndex < updatedAttributes.size() ? row.values() : row.qualifier())
                .get(dbAttributes.get(attributeIndex).name());
    }
}
