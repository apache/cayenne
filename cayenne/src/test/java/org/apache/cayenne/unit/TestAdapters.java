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
package org.apache.cayenne.unit;

import org.apache.cayenne.DefaultRuntimeProperties;
import org.apache.cayenne.access.types.DefaultValueObjectTypeRegistry;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.dba.PkGenerator;

import java.util.List;
import java.util.Map;

/**
 * Builds DbAdapters for unit tests that need an adapter, but no database.
 */
public final class TestAdapters {

    private TestAdapters() {
    }

    /**
     * Returns a generic JDBC adapter with no properties, no user types and no value object types.
     */
    public static JdbcAdapter jdbc() {
        return new JdbcAdapter(
                new DefaultRuntimeProperties(Map.of()),
                List.of(),
                List.of(),
                List.of(),
                new DefaultValueObjectTypeRegistry(List.of()));
    }

    /**
     * Returns a generic JDBC adapter, whose default PK generator is the given one.
     */
    public static JdbcAdapter jdbc(PkGenerator pkGenerator) {
        return new JdbcAdapter(
                new DefaultRuntimeProperties(Map.of()),
                List.of(),
                List.of(),
                List.of(),
                new DefaultValueObjectTypeRegistry(List.of())) {

            @Override
            public PkGenerator createPkGenerator() {
                return pkGenerator;
            }
        };
    }
}
