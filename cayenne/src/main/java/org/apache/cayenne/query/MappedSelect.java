/*****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ****************************************************************/
package org.apache.cayenne.query;

import java.util.Map;

/**
 * A query that represents a named parameterized selecting query stored in the mapping. The actual query is resolved
 * during execution.
 *
 * @since 4.0
 * @deprecated in favor of {@link MappedQuery}, that combines selecting and non-selecting mapped queries in
 * a single query. */
@Deprecated(since = "6.0", forRemoval = true)
public class MappedSelect<T> extends MappedQuery<T> {

    /**
     * Loads query with the given name, which selects objects of a given persistent class, from the mapping
     * configuration.
     *
     * @param queryName name of the mapped query
     * @param rootClass the Class of objects fetched by this query
     */
    public static <T> MappedSelect<T> query(String queryName, Class<T> rootClass) {
        return new MappedSelect<>(queryName, rootClass);
    }

    /**
     * Loads query with the given name from the mapping configuration.
     *
     * @param queryName name of the mapped query
     */
    public static MappedSelect<?> query(String queryName) {
        return new MappedSelect<>(queryName);
    }

    protected MappedSelect(String queryName) {
        super(queryName);
    }

    protected MappedSelect(String queryName, Class<T> resultClass) {
        super(queryName, resultClass);
    }

    @Override
    public MappedSelect<T> params(Map<String, ?> parameters) {
        super.params(parameters);
        return this;
    }

    @Override
    public MappedSelect<T> param(String name, Object value) {
        super.param(name, value);
        return this;
    }

    @Override
    public MappedSelect<T> name(String queryName) {
        super.name(queryName);
        return this;
    }

    @Override
    public MappedSelect<T> limit(int fetchLimit) {
        super.limit(fetchLimit);
        return this;
    }

    @Override
    public MappedSelect<T> offset(int fetchOffset) {
        super.offset(fetchOffset);
        return this;
    }

    @Override
    public MappedSelect<T> statementFetchSize(int statementFetchSize) {
        super.statementFetchSize(statementFetchSize);
        return this;
    }

    @Override
    public MappedSelect<T> queryTimeout(int timeout) {
        super.queryTimeout(timeout);
        return this;
    }

    @Override
    public MappedSelect<T> pageSize(int pageSize) {
        super.pageSize(pageSize);
        return this;
    }

    @Override
    public MappedSelect<T> forceNoCache() {
        super.forceNoCache();
        return this;
    }
}
