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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NativeColumnTypeTest {

    @Test
    public void of() {
        NativeColumnType type = NativeColumnType.of(Types.VARCHAR, "varchar");
        assertEquals(Types.VARCHAR, type.jdbcType());
        assertEquals("varchar", type.nativeType());
        assertFalse(type.autoIncrement());
        assertFalse(type.unconstrained());
    }

    @Test
    public void asAutoIncrement() {
        NativeColumnType base = NativeColumnType.of(Types.INTEGER, "serial");
        NativeColumnType auto = base.asAutoIncrement();

        // only the flag flips; everything else is preserved
        assertTrue(auto.autoIncrement());
        assertFalse(auto.unconstrained());
        assertEquals(base.jdbcType(), auto.jdbcType());
        assertEquals(base.nativeType(), auto.nativeType());

        // the original is unchanged
        assertFalse(base.autoIncrement());
    }

    @Test
    public void asUnconstrained() {
        NativeColumnType base = NativeColumnType.of(Types.VARCHAR, "text");
        NativeColumnType unconstrained = base.asUnconstrained();

        // only the flag flips; everything else is preserved
        assertTrue(unconstrained.unconstrained());
        assertFalse(unconstrained.autoIncrement());
        assertEquals(base.jdbcType(), unconstrained.jdbcType());
        assertEquals(base.nativeType(), unconstrained.nativeType());

        // the original is unchanged
        assertFalse(base.unconstrained());
    }
}
