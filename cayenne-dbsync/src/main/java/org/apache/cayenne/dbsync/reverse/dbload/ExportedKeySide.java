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

package org.apache.cayenne.dbsync.reverse.dbload;

import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.util.CompareToBuilder;
import org.apache.cayenne.util.Util;

import java.util.Objects;

/**
 * One side (PK or FK) of an {@link ExportedKey} row.
 *
 * @since 5.0
 */
public record ExportedKeySide(String catalog, String schema, String table, String column, String name)
        implements Comparable<ExportedKeySide> {

    @Override
    public String toString() {
        return catalog + "." + schema + "." + table + "." + column;
    }

    @Override
    public int compareTo(ExportedKeySide rhs) {
        Objects.requireNonNull(rhs);
        if (rhs == this) {
            return 0;
        }

        return new CompareToBuilder()
                .append(catalog, rhs.catalog)
                .append(schema, rhs.schema)
                .append(table, rhs.table)
                .append(column, rhs.column)
                .append(name, rhs.name)
                .toComparison();
    }

    /**
     * Validate that entity is for this key (exists and has same catalog/schema)
     */
    public boolean validateEntity(DbEntity entity) {
        if (entity == null) {
            return false;
        }

        if (Util.isEmptyString(catalog)) {
            if (!Util.isEmptyString(entity.getCatalog())) {
                return false;
            }
        } else {
            if (!catalog.equals(entity.getCatalog())) {
                return false;
            }
        }

        if (Util.isEmptyString(schema)) {
            return Util.isEmptyString(entity.getSchema());
        } else {
            return schema.equals(entity.getSchema());
        }
    }
}
