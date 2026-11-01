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
package org.apache.cayenne.query;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.QueryResult;
import org.apache.cayenne.UpdateResult;

import java.util.List;

/**
 * The former name of {@link ProcedureQuery}, kept for one release for compatibility.
 *
 * @since 4.0
 * @deprecated since 6.0 in favor of {@link ProcedureQuery}, which has the same API
 */
@Deprecated(since = "6.0", forRemoval = true)
public class ProcedureCall<T> extends ProcedureQuery<T> {

    /**
     * @deprecated in favor of {@link ProcedureQuery#query(String)}
     */
    @Deprecated(since = "6.0", forRemoval = true)
    public static ProcedureCall<DataRow> query(String procedure) {
        return new ProcedureCall<>(procedure);
    }

    /**
     * @deprecated in favor of {@link ProcedureQuery#query(String)}
     */
    @Deprecated(since = "6.0", forRemoval = true)
    public static ProcedureCall<DataRow> dataRowQuery(String procedure) {
        return new ProcedureCall<>(procedure);
    }

    /**
     * @deprecated in favor of {@link ProcedureQuery#query(String, Class)}
     */
    @Deprecated(since = "6.0", forRemoval = true)
    public static <T> ProcedureCall<T> query(String procedure, Class<T> resultClass) {
        return new ProcedureCall<>(procedure, resultClass);
    }

    public ProcedureCall(String procedureName) {
        super(procedureName);
        fetchDataRows();
    }

    public ProcedureCall(String procedureName, Class<T> resultClass) {
        super(procedureName);
        this.resultClass = resultClass;
    }

    /**
     * @deprecated since 6.0 in favor of {@link #execute(ObjectContext)}
     */
    @Deprecated(since = "6.0", forRemoval = true)
    public List<QueryResult> call(ObjectContext context) {
        return execute(context);
    }

    /**
     * @deprecated since 6.0, read the counts from the {@link UpdateResult} items of {@link #execute(ObjectContext)}
     */
    @Deprecated(since = "6.0", forRemoval = true)
    public int[] batchUpdate(ObjectContext context) {
        for (QueryResult item : execute(context)) {
            if (item instanceof UpdateResult update) {
                return update.counts();
            }
        }
        throw new CayenneRuntimeException("Procedure '%s' produced no updates", procedureName);
    }
}
