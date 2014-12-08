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
package org.apache.cayenne.map.naming;

import org.apache.cayenne.util.EqualsBuilder;
import org.apache.cayenne.util.HashCodeBuilder;
import org.apache.commons.lang.builder.CompareToBuilder;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * ExportedKey is an representation of relationship between two tables 
 * in database. It can be used for creating names for relationships
 * 
 */
public class ExportedKey implements Comparable {
    /**
     * Name of source table
     */
    public final String pkTable;

    /**
     * Name of source column
     */
    public final String pkColumn;

    /**
     * Name of destination table
     */
    public final String fkTable;
    
    /**
     * Name of destination column
     */
    public final String fkColumn;
    
    /**
     * Name of foreign key (might be null)
     */
    public final String fkName;

    /**
     * Name of primary key (might be null)
     */
    public final String pkName;


    public final short keySeq;

    public ExportedKey(String pkTable, String pkColumn, String pkName,
                       String fkTable, String fkColumn, String fkName, short keySeq) {
       this.pkTable  = pkTable;
       this.pkColumn = pkColumn;
       this.pkName   = pkName;
       this.fkTable  = fkTable;
       this.fkColumn = fkColumn;
       this.fkName   = fkName;
       this.keySeq = keySeq;
    }
    
    /**
     * Extracts data from a resultset pointing to a exported key to
     * ExportedKey class instance
     * 
     * @param rs ResultSet pointing to a exported key, fetched using
     * DataBaseMetaData.getExportedKeys(...) 
     */
    public static ExportedKey extractData(ResultSet rs) throws SQLException {
        return new ExportedKey(
                rs.getString("PKTABLE_NAME"),
                rs.getString("PKCOLUMN_NAME"),
                rs.getString("PK_NAME"),
                rs.getString("FKTABLE_NAME"),
                rs.getString("FKCOLUMN_NAME"),
                rs.getString("FK_NAME"),
                rs.getShort("KEY_SEQ")
        );
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
                .append(this.pkTable, rhs.pkTable)
                .append(this.pkColumn, rhs.pkColumn)
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
                .append(pkTable)
                .append(pkColumn)
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
                .append(pkTable, rhs.pkTable)
                .append(pkName, rhs.pkName)
                .append(fkTable, rhs.fkTable)
                .append(fkName, rhs.fkName)
                .append(keySeq, rhs.keySeq)
                .append(pkColumn, rhs.pkColumn)
                .append(fkColumn, rhs.fkColumn)
                .toComparison();
    }

    @Override
    public String toString() {
        return getStrKey() + " # " + keySeq
                + "(" + pkColumn + " <- " + fkColumn + ")";
    }

    public String getStrKey() {
        return pkTable + "." + pkName + " <- " + fkTable + "." + fkName;
    }
}
