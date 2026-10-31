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

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.QueryResult;
import org.apache.cayenne.UpdateResult;

import java.util.Map;

/**
 * A query that represents a named parameterized non selecting query stored in the mapping. The actual query is
 * resolved during execution.
 *
 * @since 4.0
 * @deprecated in favor of {@link MappedQuery}, that combines selecting and non-selecting mapped queries in
 * a single query. Update counts are read from the {@link UpdateResult} items of
 * {@link MappedQuery#execute(ObjectContext)}. */
@Deprecated(since = "6.0", forRemoval = true)
public class MappedExec extends MappedQuery<Object> {

    /**
     * @deprecated in favor of {@link MappedQuery#query(String)}
     */
    @Deprecated(since = "6.0", forRemoval = true)
    public static MappedExec query(String queryName) {
        return new MappedExec(queryName);
    }

    protected MappedExec(String queryName) {
        super(queryName);
    }

    @Override
    public MappedExec params(Map<String, ?> parameters) {
        super.params(parameters);
        return this;
    }

    @Override
    public MappedExec param(String name, Object value) {
        super.param(name, value);
        return this;
    }

    /**
     * Executes the query, returning the counts of its first update result.
     *
     * @throws CayenneRuntimeException if the query produced no update results.
     * @deprecated Read the counts from the {@link UpdateResult} returned by {@link #execute(ObjectContext)}.
     */
    @Deprecated(since = "6.0", forRemoval = true)
    public int[] update(ObjectContext context) {
        for (QueryResult item : execute(context)) {
            if (item instanceof UpdateResult update) {
                return update.counts();
            }
        }
        throw new CayenneRuntimeException("Expected an update result from query '%s'", queryName);
    }

    @Override
    public MappedExec name(String queryName) {
        super.name(queryName);
        return this;
    }
}
