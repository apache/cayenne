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

import java.util.List;
import java.util.Map;

/**
 * A query that represents a named parameterized non selecting query stored in the mapping. The
 * actual query is resolved during execution.
 *
 * @since 4.0
 */
public class MappedExec extends AbstractMappedQuery {

    public static MappedExec query(String queryName) {
        return new MappedExec(queryName);
    }

    protected MappedExec(String queryName) {
        super(queryName);
    }

    @Override
    public MappedExec params(Map<String, ?> parameters) {
        return (MappedExec) super.params(parameters);
    }

    @Override
    public MappedExec param(String name, Object value) {
        return (MappedExec) super.param(name, value);
    }


    /**
     * Executes the query, returning all of its result sets, update counts and OUT parameters in the order they
     * were produced.
     */
    public List<QueryResult> execute(ObjectContext context) {
        return context.execute(this);
    }

    /**
     * Executes the query, returning the counts of its first update result.
     *
     * @throws CayenneRuntimeException if the query produced no update results.
     */
    public int[] update(ObjectContext context) {
        for (QueryResult item : execute(context)) {
            if (item instanceof QueryResult.Update update) {
                return update.counts();
            }
        }
        throw new CayenneRuntimeException("Expected an update result from query '%s'", queryName);
    }
}
