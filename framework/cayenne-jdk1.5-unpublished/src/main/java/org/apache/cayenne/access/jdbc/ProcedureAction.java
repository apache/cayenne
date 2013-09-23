/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.access.jdbc;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.access.OperationObserver;
import org.apache.cayenne.access.trans.ProcedureTranslator;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.ProcedureParameter;
import org.apache.cayenne.query.ProcedureQuery;

/**
 * A SQLAction that runs a stored procedure. Note that ProcedureAction has internal state
 * and is not thread-safe.
 * 
 * @since 1.2
 */
public class ProcedureAction extends BaseSQLAction {

    protected ProcedureQuery query;

    /**
     * Holds a number of ResultSets processed by the action. This value is reset to zero
     * on every "performAction" call.
     */
    protected int processedResultSets;

    public ProcedureAction(ProcedureQuery query, JdbcAdapter adapter,
            EntityResolver entityResolver) {
        super(adapter, entityResolver);
        this.query = query;
    }

    public void performAction(Connection connection, OperationObserver observer)
            throws SQLException, Exception {

        processedResultSets = 0;

        ProcedureTranslator transl = createTranslator(connection);

        CallableStatement statement = (CallableStatement) transl.createStatement();

        try {
            initStatement(statement);

            // stored procedure may contain a mixture of update counts and result sets,
            // and out parameters. Read out parameters first, then
            // iterate until we exhaust all results

            // TODO: andrus, 4/2/2007 - according to the docs we should store the boolean
            // return value of this method and avoid calling 'getMoreResults' if it is
            // true.
            // some db's handle this well, some don't (MySQL).
            
            // 09/23/2013: almost all adapters except Oracle (and maybe a few
            // more?) are actually using the correct strategy, so making it a
            // default in the superclass, and isolating hack to subclasses is
            // probably a good idea

            statement.execute();

            // read out parameters
            readProcedureOutParameters(statement, observer);

            // read the rest of the query
            while (true) {
                if (statement.getMoreResults()) {
                    ResultSet rs = statement.getResultSet();

                    try {
                        RowDescriptor descriptor = describeResultSet(
                                rs,
                                processedResultSets++);
                        readResultSet(rs, descriptor, query, observer);
                    }
                    finally {
                        try {
                            rs.close();
                        }
                        catch (SQLException ex) {
                        }
                    }
                }
                else {
                    int updateCount = statement.getUpdateCount();
                    if (updateCount == -1) {
                        break;
                    }
                    adapter.getJdbcEventLogger().logUpdateCount(updateCount);
                    observer.nextCount(query, updateCount);
                }
            }
        }
        finally {
            try {
                statement.close();
            }
            catch (SQLException ex) {

            }
        }
    }

    /**
     * Returns the ProcedureTranslator to use for this ProcedureAction.
     * 
     * @param connection JDBC connection
     */
    protected ProcedureTranslator createTranslator(Connection connection) {
        ProcedureTranslator translator = new ProcedureTranslator();
        translator.setAdapter(getAdapter());
        translator.setQuery(query);
        translator.setEntityResolver(getEntityResolver());
        translator.setConnection(connection);
        translator.setJdbcEventLogger(adapter.getJdbcEventLogger());
        return translator;
    }

    /**
     * Creates a RowDescriptor for result set.
     * 
     * @param resultSet JDBC ResultSet
     * @param setIndex a zero-based index of the ResultSet in the query results.
     */
    protected RowDescriptor describeResultSet(ResultSet resultSet, int setIndex)
            throws SQLException {

        if (setIndex < 0) {
            throw new IllegalArgumentException(
                    "Expected a non-negative result set index. Got: " + setIndex);
        }

        RowDescriptorBuilder builder = new RowDescriptorBuilder();

        List<ColumnDescriptor[]> descriptors = query.getResultDescriptors();

        if (descriptors.isEmpty()) {
            builder.setResultSet(resultSet);
        }
        else {

            // if one result is described, all of them must be present...
            if (setIndex >= descriptors.size() || descriptors.get(setIndex) == null) {
                throw new CayenneRuntimeException(
                        "No descriptor for result set at index '"
                                + setIndex
                                + "' configured.");
            }

            ColumnDescriptor[] columns = descriptors.get(setIndex);
            builder.setColumns(columns);
        }

        switch (query.getColumnNamesCapitalization()) {
            case LOWER:
                builder.useLowercaseColumnNames();
                break;
            case UPPER:
                builder.useUppercaseColumnNames();
                break;
        }

        return builder.getDescriptor(getAdapter().getExtendedTypes());
    }

    /**
     * Returns stored procedure for an internal query.
     */
    protected Procedure getProcedure() {
        return getEntityResolver().lookupProcedure(query);
    }

    /**
     * Helper method that reads OUT parameters of a CallableStatement.
     */
    protected void readProcedureOutParameters(
            CallableStatement statement,
            OperationObserver delegate) throws SQLException, Exception {

        long t1 = System.currentTimeMillis();

        // build result row...
        DataRow result = null;
        List<ProcedureParameter> parameters = getProcedure().getCallParameters();
        for (int i = 0; i < parameters.size(); i++) {
            ProcedureParameter parameter = parameters.get(i);

            if (!parameter.isOutParam()) {
                continue;
            }

            if (result == null) {
                result = new DataRow(2);
            }

            ColumnDescriptor descriptor = new ColumnDescriptor(parameter);
            ExtendedType type = getAdapter().getExtendedTypes().getRegisteredType(
                    descriptor.getJavaClass());
            Object val = type.materializeObject(statement, i + 1, descriptor
                    .getJdbcType());

            result.put(descriptor.getDataRowKey(), val);
        }

        if (result != null && !result.isEmpty()) {
            // treat out parameters as a separate data row set
            adapter.getJdbcEventLogger().logSelectCount(1, System.currentTimeMillis() - t1);
            delegate.nextRows(query, Collections.singletonList(result));
        }
    }

    /**
     * Initializes statement with query parameters
     * 
     * @throws Exception
     */
    protected void initStatement(CallableStatement statement) throws Exception {
        int statementFetchSize = query
                .getMetaData(getEntityResolver())
                .getStatementFetchSize();
        if (statementFetchSize != 0) {
            statement.setFetchSize(statementFetchSize);
        }
    }
}
