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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.OperationObserver;
import org.apache.cayenne.access.QueryLogger;
import org.apache.cayenne.access.trans.DeleteTranslator;
import org.apache.cayenne.access.trans.QueryAssembler;
import org.apache.cayenne.access.trans.UpdateTranslator;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.DeleteQuery;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.UpdateQuery;

/**
 * @since 1.2
 * @deprecated since 3.0 as corresponding delete and update queries are deprecated.
 */
public class UpdateAction extends BaseSQLAction {

    protected Query query;

    public UpdateAction(Query query, DbAdapter adapter, EntityResolver entityResolver) {
        super(adapter, entityResolver);
        this.query = query;
    }

    protected QueryAssembler createTranslator(Connection connection) {
        QueryAssembler translator;

        if (query instanceof UpdateQuery) {
            translator = new UpdateTranslator();
        }
        else if (query instanceof DeleteQuery) {
            translator = new DeleteTranslator();
        }
        else {
            throw new CayenneRuntimeException("Can't make a translator for query "
                    + query);
        }

        translator.setAdapter(getAdapter());
        translator.setQuery(query);
        translator.setEntityResolver(getEntityResolver());
        translator.setConnection(connection);

        return translator;
    }

    public void performAction(Connection connection, OperationObserver observer)
            throws SQLException, Exception {

        QueryAssembler translator = createTranslator(connection);

        PreparedStatement statement = translator.createStatement();

        try {
            // execute update
            int count = statement.executeUpdate();
            QueryLogger.logUpdateCount(count);

            // send results back to consumer
            observer.nextCount(query, count);
        }
        finally {
            statement.close();
        }
    }
}
