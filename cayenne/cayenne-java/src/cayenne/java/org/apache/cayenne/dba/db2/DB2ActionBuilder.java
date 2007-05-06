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

import org.apache.cayenne.access.jdbc.SQLTemplateAction;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.JdbcActionBuilder;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SQLAction;
import org.apache.cayenne.query.SQLTemplate;

/**
 * @author Andrei Adamchik
 */
public class DB2ActionBuilder extends JdbcActionBuilder {

    public DB2ActionBuilder(DbAdapter adapter, EntityResolver resolver) {
        super(adapter, resolver);
    }

    /**
     * Creates a SQLTemplate handling action that removes line breaks from SQL template
     * string to satisfy DB2 JDBC driver.
     */
    protected SQLAction interceptRawSQL(SQLAction action) {

        // DB2 requires single line queries...
        if (action instanceof SQLTemplateAction) {
            ((SQLTemplateAction) action).setRemovingLineBreaks(true);
        }
        return action;
    }

    public SQLAction sqlAction(SQLTemplate query) {
        return interceptRawSQL(super.sqlAction(query));
    }

    public SQLAction updateAction(Query query) {
        // normally SQLTemplates are executed via "sqlAction", but there is a possibility
        // that this method will be called with SQLTemplate as well.
        return interceptRawSQL(super.updateAction(query));
    }
}
