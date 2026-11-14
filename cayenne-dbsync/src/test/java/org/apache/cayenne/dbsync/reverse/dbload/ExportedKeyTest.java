/*
 * Licensed to the Apache Software Foundation (ASF) under one
 *    or more contributor license agreements.  See the NOTICE file
 *    distributed with this work for additional information
 *    regarding copyright ownership.  The ASF licenses this file
 *    to you under the Apache License, Version 2.0 (the
 *    "License"); you may not use this file except in compliance
 *    with the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing,
 *    software distributed under the License is distributed on an
 *    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *    KIND, either express or implied.  See the License for the
 *    specific language governing permissions and limitations
 *    under the License.
 */

package org.apache.cayenne.dbsync.reverse.dbload;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.cayenne.unit.jdbc.TestResultSet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ExportedKeyTest {

    @Test
    public void equalsKeyData() throws SQLException {

        ExportedKeySide keyData1 = new ExportedKeySide("Catalog", null, "Table", "Column", "Name");
        ExportedKeySide keyData2 = new ExportedKeySide("Catalog", null, "Table", "Column", "Name");

        assertTrue(keyData1.equals(keyData2));
        assertTrue(keyData2.equals(keyData1));

        assertEquals(keyData1.hashCode(), keyData2.hashCode());
    }

    @Test
    public void equalsExportedKey() throws SQLException {
        ExportedKey keyData1 = ExportedKey.fromResultSet(exportedKeyRow());
        ExportedKey keyData2 = ExportedKey.fromResultSet(exportedKeyRow());

        assertTrue(keyData1.equals(keyData2));
        assertTrue(keyData2.equals(keyData1));

        assertEquals(keyData1.hashCode(), keyData2.hashCode());
    }

    // a result set positioned on a row of DatabaseMetaData.getExportedKeys()
    private static ResultSet exportedKeyRow() {
        TestResultSet rs = new TestResultSet("exportedKeys");
        rs.addColumn("PKTABLE_CAT", new Object[]{"PKCatalog"});
        rs.addColumn("PKTABLE_SCHEM", new Object[]{null});
        rs.addColumn("PKTABLE_NAME", new Object[]{"PKTable"});
        rs.addColumn("PKCOLUMN_NAME", new Object[]{"PKColumn"});
        rs.addColumn("PK_NAME", new Object[]{"PKName"});
        rs.addColumn("FKTABLE_CAT", new Object[]{"FKCatalog"});
        rs.addColumn("FKTABLE_SCHEM", new Object[]{"FKSchema"});
        rs.addColumn("FKTABLE_NAME", new Object[]{"FKTable"});
        rs.addColumn("FKCOLUMN_NAME", new Object[]{"FKColumn"});
        rs.addColumn("FK_NAME", new Object[]{"FKName"});
        rs.addColumn("KEY_SEQ", new Object[]{(short) 1});
        rs.next();
        return rs;
    }
}
