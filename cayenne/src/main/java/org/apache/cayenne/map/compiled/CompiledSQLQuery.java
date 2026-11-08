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

import org.apache.cayenne.query.CapsStrategy;
import org.apache.cayenne.query.QueryCacheStrategy;

import java.util.Map;

/**
 * A compiled named raw SQL query.
 *
 * @param rootEntity     the name of the entity the query selects, or null if it selects none
 * @param rootMap        the name of the DataMap the query is routed by, or null if it is routed by the entity or
 *                       not at all
 * @param sql            the default SQL String, or null if there is only adapter-specific SQL
 * @param adapterSql     the SQL by adapter class name, for the adapters the default does not fit
 * @param prefetches     the prefetch paths with their semantics
 * @param columnNameCaps the capitalization of the column names in the result, or null for the default
 * @since 6.0
 */
public record CompiledSQLQuery(
        CompiledDataMap map,
        String name,
        QueryCacheStrategy cacheStrategy,
        String cacheGroup,
        boolean fetchingDataRows,
        int pageSize,
        int statementFetchSize,
        String rootEntity,
        String rootMap,
        String sql,
        Map<String, String> adapterSql,
        Map<String, Integer> prefetches,
        CapsStrategy columnNameCaps) implements CompiledQuery {

    @Override
    public String toString() {
        return "CompiledSQLQuery[" + name + "]";
    }
}
