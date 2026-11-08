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

/**
 * A compiled DataMap: the unit of query routing to a DataNode and of SQL identifier quoting. The entities, tables
 * and procedures of a mapping each belong to a map and refer to it; the map itself holds nothing, the mapping is
 * the namespace they are looked up in.
 *
 * @param name                  the name of the map, unique in the mapping
 * @param quotingSQLIdentifiers true if the SQL identifiers of the tables and columns of this map are to be quoted in
 *                              generated SQL
 * @since 6.0
 */
public record CompiledDataMap(String name, boolean quotingSQLIdentifiers) {

    @Override
    public String toString() {
        return "CompiledDataMap[" + name + "]";
    }
}
