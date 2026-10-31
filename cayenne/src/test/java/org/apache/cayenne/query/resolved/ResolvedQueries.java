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

import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.QueryCacheStrategy;

import java.util.List;
import java.util.Map;

/**
 * Bare resolved queries for the tests that need a statement of some kind, but no real query behind it.
 */
public final class ResolvedQueries {

    private ResolvedQueries() {
    }

    /**
     * Returns a select with no root and default settings.
     */
    public static ResolvedSelect select() {
        return select(null, null, ResolvedQuery.STATEMENT_FETCH_SIZE_DEFAULT);
    }

    /**
     * Returns a select rooted in the given entities (either may be null) with default settings.
     */
    public static ResolvedSelect select(ObjEntity objEntity, DbEntity dbEntity) {
        return select(objEntity, dbEntity, ResolvedQuery.STATEMENT_FETCH_SIZE_DEFAULT);
    }

    /**
     * Returns a select with no root and the given statement fetch size.
     */
    public static ResolvedSelect select(int statementFetchSize) {
        return select(null, null, statementFetchSize);
    }

    private static ResolvedSelect select(ObjEntity objEntity, DbEntity dbEntity, int statementFetchSize) {
        return new ResolvedSelect(null, objEntity, dbEntity, dbEntity != null ? dbEntity.getDataMap() : null, null,
                null, null, List.of(), List.of(), false, false, false, ResolvedQuery.FETCH_LIMIT_DEFAULT,
                ResolvedQuery.FETCH_OFFSET_DEFAULT, ResolvedQuery.PAGE_SIZE_DEFAULT, statementFetchSize,
                ResolvedQuery.QUERY_TIMEOUT_DEFAULT, QueryCacheStrategy.NO_CACHE, null, null, null, Map.of(), null,
                null, List.of(), List.of());
    }

    /**
     * Returns a rootless raw SQL statement.
     */
    public static ResolvedSQL sql(String sql) {
        return ResolvedSQL.raw(sql);
    }

    /**
     * Returns a procedure call with no procedure and default settings.
     */
    public static ResolvedProcedure procedure() {
        return new ResolvedProcedure(null, null, null, null, null, null, Map.of(), List.of(), null, true,
                ResolvedQuery.FETCH_LIMIT_DEFAULT, ResolvedQuery.FETCH_OFFSET_DEFAULT, ResolvedQuery.PAGE_SIZE_DEFAULT,
                ResolvedQuery.STATEMENT_FETCH_SIZE_DEFAULT, ResolvedQuery.QUERY_TIMEOUT_DEFAULT,
                QueryCacheStrategy.NO_CACHE, null, null);
    }

    /**
     * Returns an empty insert batch of a throwaway entity.
     */
    public static InsertBatch batch() {
        return new InsertBatch(new DbEntity("T"), List.of());
    }
}
