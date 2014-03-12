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

package org.apache.cayenne.dba.mysql;

import org.apache.cayenne.access.jdbc.RowReaderFactory;
import org.apache.cayenne.dba.JdbcActionBuilder;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.ProcedureQuery;
import org.apache.cayenne.query.SQLAction;
import org.apache.cayenne.query.SelectQuery;

/**
 * @since 1.2
 */
class MySQLActionBuilder extends JdbcActionBuilder {

    MySQLActionBuilder(JdbcAdapter adapter, EntityResolver resolver, RowReaderFactory rowReaderFactory) {
        super(adapter, resolver, rowReaderFactory);
    }

    @Override
    public <T> SQLAction objectSelectAction(SelectQuery<T> query) {
        return new MySQLSelectAction(query, adapter, entityResolver, rowReaderFactory);
    }

    @Override
    public SQLAction procedureAction(ProcedureQuery query) {
        return new MySQLProcedureAction(query, getAdapter(), getEntityResolver(), rowReaderFactory);
    }
}
