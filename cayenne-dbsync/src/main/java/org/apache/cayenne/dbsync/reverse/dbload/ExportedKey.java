/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.dbsync.reverse.dbload;

import org.apache.cayenne.util.EqualsBuilder;
import org.apache.cayenne.util.HashCodeBuilder;
import org.apache.commons.lang.builder.CompareToBuilder;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * A representation of relationship between two tables in database. It can be used for creating names
 * for relationships.
 *
 * @since 4.0
 */
public class ExportedKey implements Comparable {

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

        this.keySeq = rs.getShort("KEY_SEQ");
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
        return new EqualsBuilder()
                .append(this.pk, rhs.pk)
                .append(this.fk, rhs.fk)
                .append(this.keySeq, rhs.keySeq)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(pk)
                .append(fk)
                .append(keySeq)
                .toHashCode();
    }

    @Override
    public int compareTo(Object obj) {
        if (obj == null || !obj.getClass().equals(getClass())) {
            throw new IllegalArgumentException();
        }
        if (obj == this) {
            return 0;
        }

        ExportedKey rhs = (ExportedKey) obj;
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

    public static class KeyData implements Comparable {
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
        public int compareTo(Object obj) {
            if (obj == null || !obj.getClass().equals(getClass())) {
                throw new IllegalArgumentException();
            }
            if (obj == this) {
                return 0;
            }

            KeyData rhs = (KeyData) obj;
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
            return new EqualsBuilder()
                    .append(this.catalog, rhs.catalog)
                    .append(this.schema, rhs.schema)
                    .append(this.table, rhs.table)
                    .append(this.column, rhs.column)
                    .append(this.name, rhs.name)
                    .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                    .append(catalog)
                    .append(schema)
                    .append(table)
                    .append(column)
                    .append(name)
                    .toHashCode();
        }

    }
}
