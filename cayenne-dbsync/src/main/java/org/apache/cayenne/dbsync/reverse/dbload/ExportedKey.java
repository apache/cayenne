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

import org.apache.cayenne.util.CompareToBuilder;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

/**
 * A representation of relationship between two tables in database. It can be used for creating names
 * for relationships.
 *
 * @since 4.0
 */
public record ExportedKey(ExportedKeySide pk, ExportedKeySide fk, short keySeq) implements Comparable<ExportedKey> {

    /**
     * Extracts data from a resultset pointing to an exported key into an ExportedKey instance.
     *
     * @param rs ResultSet pointing to an exported key, fetched using DataBaseMetaData.getExportedKeys(...)
     */
    static ExportedKey fromResultSet(ResultSet rs) throws SQLException {
        ExportedKeySide pk = new ExportedKeySide(
                rs.getString("PKTABLE_CAT"),
                rs.getString("PKTABLE_SCHEM"),
                rs.getString("PKTABLE_NAME"),
                rs.getString("PKCOLUMN_NAME"),
                rs.getString("PK_NAME"));

        ExportedKeySide fk = new ExportedKeySide(
                rs.getString("FKTABLE_CAT"),
                rs.getString("FKTABLE_SCHEM"),
                rs.getString("FKTABLE_NAME"),
                rs.getString("FKCOLUMN_NAME"),
                rs.getString("FK_NAME"));

        return new ExportedKey(pk, fk, rs.getShort("KEY_SEQ"));
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
        return fk + " -> " + pk + " # " + keySeq;
    }

    /**
     * Returns a key that identifies the single FK constraint this row belongs to, so that all columns of a
     * multi-column FK are grouped into one relationship.
     */
    public String groupKey() {
        return "%s.%s.%s.%s -> %s.%s.%s".formatted(
                fk.catalog(), fk.schema(), fk.table(), fk.name(),
                pk.catalog(), pk.schema(), pk.table());
    }
}
