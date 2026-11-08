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

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.compiled.CompiledColumn;
import org.apache.cayenne.map.compiled.CompiledTable;
import org.apache.cayenne.map.compiled.CompiledSchema;
import org.apache.cayenne.map.compiled.SchemaCompiler;

import java.util.List;

/**
 * Builds throwaway compiled tables for unit tests of the code that works with the compiled model, but not with
 * persistent classes: the tables are compiled from in-memory DataMaps without binding any classes.
 */
public final class CompiledTables {

    private CompiledTables() {
    }

    /**
     * Compiles the schema of the maps.
     */
    public static CompiledSchema compile(DataMap... maps) {
        return SchemaCompiler.compile(List.of(maps));
    }

    /**
     * Returns a table with the given name in a map called "defaultMap", with a PK column "id" and a column "attr".
     */
    public static CompiledTable table(String name) {
        return table(name, "defaultMap");
    }

    /**
     * Returns a table with the given name in a map with the given name, with a PK column "id" and a column "attr".
     */
    public static CompiledTable table(String name, String mapName) {
        DataMap map = new DataMap(mapName);
        map.addDbEntity(authoredTable(name));
        return compile(map).table(name);
    }

    /**
     * Returns a non-PK column with the given name, of a throwaway table, for the code that only needs the column
     * metadata.
     */
    public static CompiledColumn column(String name) {
        DbEntity table = new DbEntity("T");
        table.addAttribute(new DbAttribute(name));
        DataMap map = new DataMap("M");
        map.addDbEntity(table);
        return compile(map).table("T").column(name);
    }

    /**
     * Returns an authored table with a PK column "id" and a column "attr", to be added to a DataMap.
     */
    public static DbEntity authoredTable(String name) {
        DbAttribute id = new DbAttribute("id");
        id.setPrimaryKey(true);
        DbEntity table = new DbEntity(name);
        table.addAttribute(id);
        table.addAttribute(new DbAttribute("attr"));
        return table;
    }
}
