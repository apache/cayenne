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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.util.CompareToBuilder;
import org.apache.cayenne.util.Util;

/**
 * A representation of relationship between two tables in database. It can be used for creating names
 * for relationships.
 *
 * @since 4.0
 */
public class ExportedKey implements Comparable<ExportedKey> {

    private final KeyData pk;
    private final KeyData fk;
    private final short keySeq;

    /**
     * Extracts data from a resultset pointing to a exported key to
     * ExportedKey class instance
     *
     * @param rs ResultSet pointing to a exported key, fetched using
     *           DataBaseMetaData.getExportedKeys(...)
     */
    ExportedKey(ResultSet rs) throws SQLException {
        String pkCatalog = rs.getString("PKTABLE_CAT");
        String pkSchema = rs.getString("PKTABLE_SCHEM");
        String pkTable = rs.getString("PKTABLE_NAME");
        String pkColumn = rs.getString("PKCOLUMN_NAME");
        String pkName = rs.getString("PK_NAME");
        pk = new KeyData(pkCatalog, pkSchema, pkTable, pkColumn, pkName);

        String fkCatalog = rs.getString("FKTABLE_CAT");
        String fkSchema = rs.getString("FKTABLE_SCHEM");
        String fkTable = rs.getString("FKTABLE_NAME");
        String fkColumn = rs.getString("FKCOLUMN_NAME");
        String fkName = rs.getString("FK_NAME");
        fk = new KeyData(fkCatalog, fkSchema, fkTable, fkColumn, fkName);

        keySeq = rs.getShort("KEY_SEQ");
    }

    public KeyData getPk() {
        return pk;
    }

    public KeyData getFk() {
        return fk;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        ExportedKey rhs = (ExportedKey) obj;
        return Objects.equals(pk, rhs.pk)
                && Objects.equals(fk, rhs.fk)
                && keySeq == rhs.keySeq;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pk, fk, keySeq);
    }

    @Override
    public int compareTo(ExportedKey rhs) {
        Objects.requireNonNull(rhs);
        if (rhs == this) {
            return 0;
        }
        return new CompareToBuilder()
                .append(pk, rhs.pk)
                .append(fk, rhs.fk)
                .append(keySeq, rhs.keySeq)
                .toComparison();
    }

    @Override
    public String toString() {
        return getStrKey() + " # " + keySeq;
    }

    String getStrKey() {
        return pk + " <- " + fk;
    }

    /**
     * Returns a key that identifies the single FK constraint this row belongs to, so that all columns of a
     * multi-column FK are grouped into one relationship. Uses the FK constraint name (FK_NAME) reported by the
     * driver; falls back to the per-column {@link #getStrKey()} when the name is unavailable, preserving the
     * historical behavior for drivers that don't report constraint names.
     */
    String getGroupKey() {
        String fkName = fk.getName();
        if (Util.isEmptyString(fkName)) {
            return getStrKey();
        }
        return fk.getCatalog() + "." + fk.getSchema() + "." + fk.getTable() + "." + fkName
                + " -> " + pk.getCatalog() + "." + pk.getSchema() + "." + pk.getTable();
    }

    public static class KeyData implements Comparable<KeyData> {
        private final String catalog;
        private final String schema;
        private final String table;
        private final String column;
        private final String name;

        KeyData(String catalog, String schema, String table, String column, String name) {
            this.catalog = catalog;
            this.schema = schema;
            this.table = table;
            this.column = column;
            this.name = name;
        }

        public String getCatalog() {
            return catalog;
        }

        public String getSchema() {
            return schema;
        }

        public String getTable() {
            return table;
        }

        public String getColumn() {
            return column;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return catalog + "." + schema + "." + table + "." + column;
        }

        @Override
        public int compareTo(KeyData rhs) {
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

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (obj == this) {
                return true;
            }
            if (obj.getClass() != getClass()) {
                return false;
            }
            KeyData rhs = (KeyData) obj;
            return Objects.equals(catalog, rhs.catalog)
                    && Objects.equals(schema, rhs.schema)
                    && Objects.equals(table, rhs.table)
                    && Objects.equals(column, rhs.column)
                    && Objects.equals(name, rhs.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(catalog, schema, table, column, name);
        }

        /**
         * Validate that entity is for this key (exists and has same catalog/schema)
         * @param entity to validate
         * @return is entity matches for this key
         */
        public boolean validateEntity(DbEntity entity) {
            if (entity == null) {
                return false;
            }

            if(Util.isEmptyString(catalog)) {
                if(!Util.isEmptyString(entity.getCatalog())) {
                    return false;
                }
            } else {
                if(!catalog.equals(entity.getCatalog())) {
                    return false;
                }
            }

            if(Util.isEmptyString(schema)) {
                return Util.isEmptyString(entity.getSchema());
            } else {
                return schema.equals(entity.getSchema());
            }
        }
    }
}
