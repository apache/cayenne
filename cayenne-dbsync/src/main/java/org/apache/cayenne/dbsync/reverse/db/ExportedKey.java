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
package org.apache.cayenne.dbsync.reverse.db;

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
class ExportedKey implements Comparable {

    private final String pkCatalog;
    private final String pkSchema;
    private final String pkTable;
    private final String pkColumn;
    private final String fkCatalog;
    private final String fkSchema;
    private final String fkTable;
    private final String fkColumn;
    private final String fkName;
    private final String pkName;
    private final short keySeq;

    public ExportedKey(String pkTable, String pkColumn, String pkName,
                       String fkTable, String fkColumn, String fkName, short keySeq) {
        this(null, null, pkTable, pkColumn, pkName, null, null, fkTable, fkColumn, fkName, keySeq);
    }

    public ExportedKey(String pkCatalog, String pkSchema, String pkTable, String pkColumn, String pkName,
                       String fkCatalog, String fkSchema, String fkTable, String fkColumn, String fkName, short keySeq) {
        this.pkCatalog = pkCatalog;
        this.pkSchema = pkSchema;
        this.pkTable = pkTable;
        this.pkColumn = pkColumn;
        this.pkName = pkName;
        this.fkCatalog = fkCatalog;
        this.fkSchema = fkSchema;
        this.fkTable = fkTable;
        this.fkColumn = fkColumn;
        this.fkName = fkName;
        this.keySeq = keySeq;
    }

    /**
     * Extracts data from a resultset pointing to a exported key to
     * ExportedKey class instance
     *
     * @param rs ResultSet pointing to a exported key, fetched using
     *           DataBaseMetaData.getExportedKeys(...)
     */
    public static ExportedKey extractData(ResultSet rs) throws SQLException {
        return new ExportedKey(
                rs.getString("PKTABLE_CAT"),
                rs.getString("PKTABLE_SCHEM"),
                rs.getString("PKTABLE_NAME"),
                rs.getString("PKCOLUMN_NAME"),
                rs.getString("PK_NAME"),
                rs.getString("FKTABLE_CAT"),
                rs.getString("FKTABLE_SCHEM"),
                rs.getString("FKTABLE_NAME"),
                rs.getString("FKCOLUMN_NAME"),
                rs.getString("FK_NAME"),
                rs.getShort("KEY_SEQ")
        );
    }


    public String getPkCatalog() {
        return pkCatalog;
    }

    public String getPkSchema() {
        return pkSchema;
    }

    public String getFkCatalog() {
        return fkCatalog;
    }

    public String getFkSchema() {
        return fkSchema;
    }

    /**
     * @return source table name
     */
    public String getPKTableName() {
        return pkTable;
    }

    /**
     * @return destination table name
     */
    public String getFKTableName() {
        return fkTable;
    }

    /**
     * @return source column name
     */
    public String getPKColumnName() {
        return pkColumn;
    }

    /**
     * @return destination column name
     */
    public String getFKColumnName() {
        return fkColumn;
    }

    /**
     * @return PK name
     */
    public String getPKName() {
        return pkName;
    }

    /**
     * @return FK name
     */
    public String getFKName() {
        return fkName;
    }

    public short getKeySeq() {
        return keySeq;
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
                .append(this.pkCatalog, rhs.pkCatalog)
                .append(this.pkSchema, rhs.pkSchema)
                .append(this.pkTable, rhs.pkTable)
                .append(this.pkColumn, rhs.pkColumn)
                .append(this.fkCatalog, rhs.fkCatalog)
                .append(this.fkSchema, rhs.fkSchema)
                .append(this.fkTable, rhs.fkTable)
                .append(this.fkColumn, rhs.fkColumn)
                .append(this.fkName, rhs.fkName)
                .append(this.pkName, rhs.pkName)
                .append(this.keySeq, rhs.keySeq)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(pkCatalog)
                .append(pkSchema)
                .append(pkTable)
                .append(pkColumn)
                .append(fkCatalog)
                .append(fkSchema)
                .append(fkTable)
                .append(fkColumn)
                .append(fkName)
                .append(pkName)
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
                .append(pkCatalog, rhs.pkCatalog)
                .append(pkSchema, rhs.pkSchema)
                .append(pkTable, rhs.pkTable)
                .append(pkName, rhs.pkName)
                .append(fkCatalog, rhs.fkCatalog)
                .append(fkSchema, rhs.fkSchema)
                .append(fkTable, rhs.fkTable)
                .append(fkName, rhs.fkName)
                .append(keySeq, rhs.keySeq)
                .append(pkColumn, rhs.pkColumn)
                .append(fkColumn, rhs.fkColumn)
                .toComparison();
    }

    @Override
    public String toString() {
        return getStrKey() + " # " + keySeq;
    }

    public String getStrKey() {
        return pkCatalog + "." + pkSchema + "." + pkTable + "." + pkColumn
                + " <- " + fkCatalog + "." + fkSchema + "." + fkTable + "." + fkColumn;
    }
}
