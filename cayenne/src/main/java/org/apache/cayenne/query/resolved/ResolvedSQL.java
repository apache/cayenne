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

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.CapsStrategy;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.QueryCacheStrategy;
import org.apache.cayenne.query.ResultSegments;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.util.Util;

import java.util.List;
import java.util.Map;

/**
 * A resolved raw SQL statement. The SQL is a template (with optional per-adapter dialects) rendered by the DataNode's
 * {@link org.apache.cayenne.access.translator.SQLTemplateTranslator} with either named or positional parameters. The
 * statement is routed by its DataMap when it has a root (a persistent type, an entity name or a DataMap name), and by
 * its {@link #dataNodeName()} otherwise.
 *
 * @since 6.0
 */
public record ResolvedSQL(
        Query<?> source,
        ObjEntity objEntity,
        DbEntity dbEntity,
        DataMap dataMap,
        ClassDescriptor classDescriptor,
        String dataNodeName,
        String defaultTemplate,
        Map<String, String> templates,
        Map<String, ?> params,
        List<Object> positionalParams,
        CapsStrategy columnNameCaps,
        boolean returnGeneratedKeys,
        List<Class<?>> resultColumnsTypes,
        boolean useScalar,
        boolean fetchingDataRows,
        int fetchLimit,
        int fetchOffset,
        int pageSize,
        int statementFetchSize,
        int queryTimeout,
        QueryCacheStrategy cacheStrategy,
        String cacheKey,
        String cacheGroup,
        PrefetchTreeNode prefetchTree,
        ResultSegments resultSegments) implements ResolvedQuery {

    /**
     * Creates a rootless statement with no parameters and default settings, for SQL the framework runs outside a
     * query, e.g. PK generation, so that it is logged like any other statement.
     */
    public static ResolvedSQL raw(String sql) {
        return new ResolvedSQL(null, null, null, null, null, null, sql, Map.of(), Map.of(), List.of(),
                CapsStrategy.DEFAULT, false, List.of(), false, true, FETCH_LIMIT_DEFAULT, FETCH_OFFSET_DEFAULT,
                PAGE_SIZE_DEFAULT, STATEMENT_FETCH_SIZE_DEFAULT, QUERY_TIMEOUT_DEFAULT,
                QueryCacheStrategy.getDefaultStrategy(), null, null, null, null);
    }

    /**
     * Returns the SQL template for a given adapter: the dialect registered for the adapter class, or the default
     * template. Line breaks are replaced with spaces, as some databases (DB2) can't run statements with breaks, and
     * the rest are affected by CAY-726.
     */
    public String template(DbAdapter adapter) {
        String dialect = templates.get(adapter.getClass().getName());
        String sql = dialect != null ? dialect : defaultTemplate;
        return sql != null ? Util.stripLineBreaks(sql, ' ') : null;
    }
}
