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


import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ProcedureResult;
import org.apache.cayenne.QueryResponse;
import org.apache.cayenne.access.jdbc.ColumnDescriptor;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.util.ProcedureResultBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fluent API for calling stored procedures.
 *
 * @since 4.0
 */
public class ProcedureCall<T> extends IndirectQuery {

    /**
     * Creates procedure call using name of stored procedure defined in the mapping file.
     */
    public static ProcedureCall query(String procedure) {
        return new ProcedureCall(procedure);
    }

    /**
     * Creates procedure call returning data rows using name of stored procedure defined in the mapping file.
     */
    public static ProcedureCall<DataRow> dataRowQuery(String procedure) {
        ProcedureCall<DataRow> procedureCall = new ProcedureCall<>(procedure);
        procedureCall.fetchingDataRows = true;
        return procedureCall;
    }

    /**
     * Creates procedure call using name of stored procedure defined in the mapping file and specifies data type
     * of the objects it should return.
     */
    public static <T> ProcedureCall<T> query(String procedure, Class<T> resultClass) {
        ProcedureCall<T> procedureCall = new ProcedureCall<>(procedure, resultClass);
        procedureCall.fetchingDataRows = false;
        return procedureCall;
    }

    protected String procedureName;
    protected Class<T> resultClass;
    protected Map<String, Object> params;
    protected Integer fetchLimit;
    protected Integer fetchOffset;
    protected CapsStrategy capsStrategy;
    protected Boolean fetchingDataRows;
    protected ColumnDescriptor[] resultDescriptor;

    public ProcedureCall(String procedureName) {
        this.procedureName = procedureName;
    }

    public ProcedureCall(String procedureName, Class<T> resultClass) {
        this.procedureName = procedureName;
        this.resultClass = resultClass;
    }

    public ProcedureCall<T> params(Map<String, ?> parameters) {
        if (this.params == null) {
            this.params = new HashMap<>(parameters);
        } else {
            Map bareMap = parameters;
            this.params.putAll(bareMap);
        }

        this.replacementQuery = null;

        return this;
    }

    public ProcedureCall<T> param(String name, Object value) {
        if (this.params == null) {
            this.params = new HashMap<>();
        }

        this.params.put(name, value);

        this.replacementQuery = null;

        return this;
    }

    public ProcedureCall<T> limit(int fetchLimit) {
        this.fetchLimit = fetchLimit;

        return this;
    }

    public ProcedureCall<T> offset(int fetchOffset) {
        this.fetchOffset = fetchOffset;

        return this;
    }

    public ProcedureCall<T> capsStrategy(CapsStrategy capsStrategy) {
        this.capsStrategy = capsStrategy;

        return this;
    }

    public ProcedureCall<T> resultDescriptor(ColumnDescriptor[] resultDescriptor) {
        this.resultDescriptor = resultDescriptor;

        return this;
    }

    public ProcedureResult<T> call(ObjectContext context) {
        QueryResponse response = context.performGenericQuery(this);

        ProcedureResultBuilder<T> builder = ProcedureResultBuilder.builder(response.size(), resultClass);

        for (response.reset(); response.next(); ) {

            if (response.isList()) {
                builder.addSelectResult(response.currentList());
            } else {
                builder.addBatchUpdateResult(response.currentUpdateCount());
            }
        }

        return builder.build();
    }

    public List<T> select(ObjectContext context) {
        return call(context).firstList();
    }

    public int[] batchUpdate(ObjectContext context) {
        return call(context).firstBatchUpdateCount();
    }

    public int update(ObjectContext context) {
        return call(context).firstUpdateCount();
    }

    @Override
    protected Query createReplacementQuery(EntityResolver resolver) {
        ProcedureQuery procedureQuery = new ProcedureQuery(procedureName);

        if (fetchingDataRows != null) {
            procedureQuery.setFetchingDataRows(fetchingDataRows);
        }

        if (resultClass != null) {
            procedureQuery.resultClass = this.resultClass;
        }

        if (fetchLimit != null) {
            procedureQuery.setFetchLimit(fetchLimit);
        }

        if (fetchOffset != null) {
            procedureQuery.setFetchOffset(fetchOffset);
        }

        if (resultDescriptor != null) {
            procedureQuery.addResultDescriptor(resultDescriptor);
        }

        if (capsStrategy != null) {
            procedureQuery.setColumnNamesCapitalization(capsStrategy);
        }

        procedureQuery.setParameters(params);

        return procedureQuery;
    }
}
