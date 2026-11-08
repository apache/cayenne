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

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.compiled.CompiledTable;
import org.apache.cayenne.map.compiled.SchemaCompiler;

import java.util.List;

/**
 * Compiles the ad hoc DbEntities the adapter tests build by hand, as the adapters only take compiled tables.
 */
public final class AdhocTables {

    private AdhocTables() {
    }

    /**
     * Compiles the entity (and its DataMap, or a throwaway one if it has none) as a schema and
     * returns the compiled table.
     */
    public static CompiledTable compile(DbEntity entity) {
        DataMap map = entity.getDataMap();
        if (map == null) {
            map = new DataMap("adhoc");
            map.addDbEntity(entity);
        }
        return SchemaCompiler.compile(List.of(map)).table(entity.getName());
    }
}
