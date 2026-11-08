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

import org.apache.cayenne.query.QueryCacheStrategy;

/**
 * A query stored in the mapping under a name, compiled to the settings the runtime needs to assemble it. Its roots
 * are names: a compiled query does not refer to the authored model.
 *
 * @since 6.0
 */
public sealed interface CompiledQuery permits CompiledSelectQuery, CompiledSQLQuery, CompiledProcedureQuery {

    /**
     * Returns the map the query belongs to.
     */
    CompiledDataMap map();

    String name();

    QueryCacheStrategy cacheStrategy();

    /**
     * Returns the cache group of the query, or null if there is none.
     */
    String cacheGroup();

    boolean fetchingDataRows();

    int pageSize();

    int statementFetchSize();
}
