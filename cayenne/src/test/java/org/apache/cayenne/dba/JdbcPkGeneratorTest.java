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

package org.apache.cayenne.dba;

import java.sql.Types;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.compiled.CompiledColumn;
import org.apache.cayenne.unit.CompiledTables;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class JdbcPkGeneratorTest {

    // the binary key branch uses neither the adapter nor the DataNode
    private final JdbcPkGenerator generator = new JdbcPkGenerator();

    @ParameterizedTest
    @ValueSource(ints = {Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY, Types.BLOB})
    public void binaryPk(int type) {
        assertEquals(16, generateBinaryPk(type, 16).length);
        assertEquals(20, generateBinaryPk(type, 20).length);
    }

    @ParameterizedTest
    @ValueSource(ints = {Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY, Types.BLOB})
    public void binaryPk_NoLength(int type) {
        assertEquals(16, generateBinaryPk(type, -1).length);
    }

    @Test
    public void binaryPk_TooShort() {
        assertThrows(CayenneRuntimeException.class, () -> generateBinaryPk(Types.VARBINARY, 8));
    }

    @Test
    public void binaryPk_NoTrailingZero() {
        for (int i = 0; i < 4096; i++) {
            byte[] pk = generateBinaryPk(Types.BINARY, 16);
            assertNotEquals(0, pk[pk.length - 1]);
        }
    }

    private byte[] generateBinaryPk(int type, int length) {
        DbEntity entity = new DbEntity("T");
        DbAttribute pk = new DbAttribute("ID", type, entity);
        pk.setPrimaryKey(true);
        pk.setMaxLength(length);
        entity.addAttribute(pk);

        DataMap map = new DataMap("M");
        map.addDbEntity(entity);
        CompiledColumn column = CompiledTables.compile(map).table("T").column("ID");
        return (byte[]) generator.generatePk(null, column);
    }
}
