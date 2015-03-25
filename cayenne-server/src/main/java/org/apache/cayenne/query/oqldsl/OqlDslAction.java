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
package org.apache.cayenne.query.oqldsl;

import de.jexp.jequel.sql.Sql;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.OperationObserver;
import org.apache.cayenne.access.jdbc.BaseSQLAction;
import org.apache.cayenne.query.SQLActionVisitor;
import org.apache.cayenne.query.sqldsl.SqlDslAction;
import org.apache.cayenne.query.sqldsl.SqlQuery;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Oql execution class. Here we do following steps:
 *  1) transform object-select into sql-select
 *  2) execute sql-select
 *
 * Here also probably will be necessary to add reverse mapping from db-layer result to obj-layer but currently
 * all this stuff handled by observer/readers and upper layer DataNode (???)
 *
 * @since 4.0
 */
public class OqlDslAction extends BaseSQLAction {

    private final OqlQuery<?> query;
    private final SQLActionVisitor actionVisitor;

    /**
     * @since 4.0
     */
    public OqlDslAction(SQLActionVisitor actionVisitor, OqlQuery<?> query, DataNode dataNode) {
        super(dataNode);
        this.actionVisitor = actionVisitor;
        this.query = query;
    }

    @Override
    public void performAction(Connection connection, OperationObserver observer) throws SQLException, Exception {

        Sql sql = getOqlToSqlTransformer().transform(query.getSelect());

        new SqlQuery(sql).createSQLAction(actionVisitor).performAction(connection, observer);
    }

    /**
     * Since all execution logic straight forward I expect that transformer is the only thing that
     * can be useful to customize
     * */
    protected OqlToSqlTransformer getOqlToSqlTransformer() {
        return new OqlToSqlTransformer();
    }
}
