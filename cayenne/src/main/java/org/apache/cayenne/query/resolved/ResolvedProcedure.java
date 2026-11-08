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

package org.apache.cayenne.query.resolved;

import org.apache.cayenne.map.compiled.CompiledDataMap;
import org.apache.cayenne.map.compiled.CompiledTable;
import org.apache.cayenne.map.compiled.CompiledEntity;
import org.apache.cayenne.map.compiled.CompiledProcedure;
import org.apache.cayenne.query.CapsStrategy;
import org.apache.cayenne.query.ProcedureColumn;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.access.types.ValueObjectTypeRegistry;
import org.apache.cayenne.query.QueryCacheStrategy;

import java.util.List;
import java.util.Map;

/**
 * A resolved stored procedure call. The entity, if any, is the entity of the objects fetched from the procedure
 * result sets; the DataMap is the entity's one, or else the one the procedure is mapped in.
 *
 * @since 6.0
 */
public record ResolvedProcedure(
        Query<?> source,
        CompiledProcedure procedure,
        CompiledEntity entity,
        CompiledTable table,
        CompiledDataMap dataMap,
        Map<String, ?> params,
        List<ProcedureColumn[]> resultDescriptors,
        CapsStrategy columnNameCaps,
        boolean fetchingDataRows,
        int fetchLimit,
        int fetchOffset,
        int pageSize,
        int statementFetchSize,
        int queryTimeout,
        QueryCacheStrategy cacheStrategy,
        String cacheKey,
        String cacheGroup) implements ResolvedQuery {

    // the key has no value object types to render, so it is computed by the query
    @Override
    public String cacheKey(ValueObjectTypeRegistry valueObjectTypes) {
        return cacheKey;
    }
}
