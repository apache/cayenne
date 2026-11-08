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
package org.apache.cayenne.map.compiled;

import java.util.Collection;
import java.util.Map;

/**
 * The database side of a compiled mapping: the tables of the DataMaps with their columns, joins, foreign keys and
 * key sequences, in a dependency order, and the stored procedures. A schema is compiled by the
 * {@link SchemaCompiler} without the persistent classes, for the tools that generate or compare a DB schema, and is
 * the base a {@link CompiledMapping} layers its entities on.
 *
 * @since 6.0
 */
public final class CompiledSchema {

    private final Map<String, CompiledDataMap> maps;
    private final Map<String, CompiledTable> tables;
    private final Map<String, CompiledProcedure> procedures;

    CompiledSchema(
            Map<String, CompiledDataMap> maps,
            Map<String, CompiledTable> tables,
            Map<String, CompiledProcedure> procedures) {

        this.maps = maps;
        this.tables = tables;
        this.procedures = procedures;
    }

    /**
     * Returns a schema with no maps.
     */
    static CompiledSchema empty() {
        return new CompiledSchema(Map.of(), Map.of(), Map.of());
    }

    public Collection<CompiledDataMap> maps() {
        return maps.values();
    }

    /**
     * Returns a map by name, or null if there is none.
     */
    public CompiledDataMap map(String name) {
        return name != null ? maps.get(name) : null;
    }

    public Collection<CompiledTable> tables() {
        return tables.values();
    }

    /**
     * Returns a table by name, or null if there is none.
     */
    public CompiledTable table(String name) {
        return name != null ? tables.get(name) : null;
    }

    public Collection<CompiledProcedure> procedures() {
        return procedures.values();
    }

    /**
     * Returns a procedure by name, or null if there is none.
     */
    public CompiledProcedure procedure(String name) {
        return name != null ? procedures.get(name) : null;
    }

    @Override
    public String toString() {
        return "CompiledSchema" + maps.keySet();
    }
}
