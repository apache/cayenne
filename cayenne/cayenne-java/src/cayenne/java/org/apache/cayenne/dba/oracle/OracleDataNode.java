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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.OperationObserver;
import org.apache.cayenne.access.trans.BatchQueryBuilder;
import org.apache.cayenne.access.trans.LOBBatchQueryBuilder;
import org.apache.cayenne.access.trans.LOBBatchQueryWrapper;
import org.apache.cayenne.query.BatchQuery;

/**
 * DataNode subclass customized for Oracle database engine.
 * 
 * @deprecated Since 1.2 DataNode customization is done entirely via DbAdapter.
 * @author Andrei Adamchik
 */
public class OracleDataNode extends DataNode {

    public OracleDataNode() {
        super();
    }

    public OracleDataNode(String name) {
        super(name);
    }

 
    /**
     * Special update method that is called from OracleAdapter if LOB columns are to be
     * updated.
     */
    public void runBatchUpdateWithLOBColumns(
            Connection con,
            BatchQuery query,
            OperationObserver delegate) throws SQLException, Exception {

        new OracleLOBBatchAction(query, getAdapter()).performAction(con, delegate);
    }

    /**
     * Selects a LOB row and writes LOB values.
     */
    protected void processLOBRow(
            Connection con,
            LOBBatchQueryBuilder queryBuilder,
            LOBBatchQueryWrapper selectQuery,
            List qualifierAttributes) throws SQLException, Exception {

        new OracleLOBBatchAction(null, getAdapter()).processLOBRow(con,
                queryBuilder,
                selectQuery,
                qualifierAttributes);
    }

    /**
     * Configures BatchQueryBuilder to trim CHAR column values, and then invokes super
     * implementation.
     */
    protected void runBatchUpdateAsBatch(
            Connection con,
            BatchQuery query,
            BatchQueryBuilder queryBuilder,
            OperationObserver delegate) throws SQLException, Exception {

        queryBuilder.setTrimFunction(OracleAdapter.TRIM_FUNCTION);
        super.runBatchUpdateAsBatch(con, query, queryBuilder, delegate);
    }

    /**
     * Configures BatchQueryBuilder to trim CHAR column values, and then invokes super
     * implementation.
     */
    protected void runBatchUpdateAsIndividualQueries(
            Connection con,
            BatchQuery query,
            BatchQueryBuilder queryBuilder,
            OperationObserver delegate) throws SQLException, Exception {

        queryBuilder.setTrimFunction(OracleAdapter.TRIM_FUNCTION);
        super.runBatchUpdateAsIndividualQueries(con, query, queryBuilder, delegate);
    }
}
