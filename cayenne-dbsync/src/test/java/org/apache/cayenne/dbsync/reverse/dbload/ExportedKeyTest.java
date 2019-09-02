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

import org.junit.Assert;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @since 4.0
 */
public class ExportedKeyTest {

    @Test
    public void testEqualsKeyData() throws SQLException {

        ExportedKey.KeyData keyData1 = new ExportedKey.KeyData("Catalog", null, "Table", "Column", "Name");
        ExportedKey.KeyData keyData2 = new ExportedKey.KeyData("Catalog", null, "Table", "Column", "Name");

        Assert.assertTrue(keyData1.equals(keyData2));
        Assert.assertTrue(keyData2.equals(keyData1));

        Assert.assertEquals(keyData1.hashCode(), keyData2.hashCode());
    }

    @Test
    public void testEqualsExportedKey() throws SQLException {
        ResultSet rs1 = mock(ResultSet.class);
        when(rs1.getString("PKTABLE_CAT")).thenReturn("PKCatalog");
        when(rs1.getString("PKTABLE_SCHEM")).thenReturn(null);
        when(rs1.getString("PKTABLE_NAME")).thenReturn("PKTable");
        when(rs1.getString("PKCOLUMN_NAME")).thenReturn("PKColumn");
        when(rs1.getString("PK_NAME")).thenReturn("PKName");

        when(rs1.getString("FKTABLE_CAT")).thenReturn("FKCatalog");
        when(rs1.getString("FKTABLE_SCHEM")).thenReturn("FKSchema");
        when(rs1.getString("FKTABLE_NAME")).thenReturn("FKTable");
        when(rs1.getString("FKCOLUMN_NAME")).thenReturn("FKColumn");
        when(rs1.getString("FK_NAME")).thenReturn("FKName");

        when(rs1.getShort("KEY_SEQ")).thenReturn((short) 1);

        ExportedKey keyData1 = new ExportedKey(rs1);

        ResultSet rs2 = mock(ResultSet.class);
        when(rs2.getString("PKTABLE_CAT")).thenReturn("PKCatalog");
        when(rs2.getString("PKTABLE_SCHEM")).thenReturn(null);
        when(rs2.getString("PKTABLE_NAME")).thenReturn("PKTable");
        when(rs2.getString("PKCOLUMN_NAME")).thenReturn("PKColumn");
        when(rs2.getString("PK_NAME")).thenReturn("PKName");

        when(rs2.getString("FKTABLE_CAT")).thenReturn("FKCatalog");
        when(rs2.getString("FKTABLE_SCHEM")).thenReturn("FKSchema");
        when(rs2.getString("FKTABLE_NAME")).thenReturn("FKTable");
        when(rs2.getString("FKCOLUMN_NAME")).thenReturn("FKColumn");
        when(rs2.getString("FK_NAME")).thenReturn("FKName");

        when(rs2.getShort("KEY_SEQ")).thenReturn((short)1);

        ExportedKey keyData2 = new ExportedKey(rs2);

        Assert.assertTrue(keyData1.equals(keyData2));
        Assert.assertTrue(keyData2.equals(keyData1));

        Assert.assertEquals(keyData1.hashCode(), keyData2.hashCode());
    }
}
