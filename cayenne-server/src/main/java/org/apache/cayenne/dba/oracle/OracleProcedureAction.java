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

package org.apache.cayenne.dba.oracle;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import org.apache.cayenne.DataRow;
import org.apache.cayenne.access.OperationObserver;
import org.apache.cayenne.access.jdbc.ColumnDescriptor;
import org.apache.cayenne.access.jdbc.ProcedureAction;
import org.apache.cayenne.access.jdbc.RowDescriptor;
import org.apache.cayenne.access.jdbc.RowReaderFactory;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ProcedureParameter;
import org.apache.cayenne.query.ProcedureQuery;

/**
 * Oracle-specific ProcedureAction that supports ResultSet OUT parameters.
 * 
 * @since 1.2
 */
class OracleProcedureAction extends ProcedureAction {

    OracleProcedureAction(ProcedureQuery query, JdbcAdapter adapter, EntityResolver entityResolver,
            RowReaderFactory rowReaderFactory) {
        super(query, adapter, entityResolver, rowReaderFactory);
    }

    /**
     * Helper method that reads OUT parameters of a CallableStatement.
     */
    @Override
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

            // ==== start Oracle-specific part
            if (parameter.getType() == OracleAdapter.getOracleCursorType()) {
                ResultSet rs = (ResultSet) statement.getObject(i + 1);

                try {
                    RowDescriptor rsDescriptor = describeResultSet(
                            rs,
                            processedResultSets++);
                    readResultSet(rs, rsDescriptor, query, delegate);
                }
                finally {
                    try {
                        rs.close();
                    }
                    catch (SQLException ex) {
                    }
                }
            }
            // ==== end Oracle-specific part
            else {
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
        }

        if (result != null && !result.isEmpty()) {
            // treat out parameters as a separate data row set
            adapter.getJdbcEventLogger().logSelectCount(1, System.currentTimeMillis() - t1);
            delegate.nextRows(query, Collections.singletonList(result));
        }
    }
}
