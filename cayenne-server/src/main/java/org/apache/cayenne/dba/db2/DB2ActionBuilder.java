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
package org.apache.cayenne.dba.db2;

import org.apache.cayenne.access.jdbc.RowReaderFactory;
import org.apache.cayenne.dba.JdbcActionBuilder;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.ProcedureQuery;
import org.apache.cayenne.query.SQLAction;

/**
 * @since 3.1
 */
public class DB2ActionBuilder extends JdbcActionBuilder {

    DB2ActionBuilder(JdbcAdapter adapter, EntityResolver resolver, RowReaderFactory rowReaderFactory) {
        super(adapter, resolver, rowReaderFactory);
    }

    @Override
    public SQLAction procedureAction(ProcedureQuery query) {
        return new DB2ProcedureAction(query, getAdapter(), getEntityResolver(), rowReaderFactory);
    }
}
