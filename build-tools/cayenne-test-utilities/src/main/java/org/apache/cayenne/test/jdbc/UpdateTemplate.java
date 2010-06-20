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
package org.apache.cayenne.test.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

class UpdateTemplate {

    DBHelper parent;

    public UpdateTemplate(DBHelper parent) {
        this.parent = parent;
    }

    protected void bindParameters(PreparedStatement statement, Object... bindings)
            throws SQLException {

        if (bindings != null && bindings.length > 0) {
            for (int i = 0; i < bindings.length; i++) {
                statement.setObject(i + 1, bindings[i]);
            }
        }
    }

    int execute(String sql, Object... bindings) throws SQLException {
        UtilityLogger.log(sql);
        Connection c = parent.getConnection();
        try {

            PreparedStatement st = c.prepareStatement(sql);

            try {
                bindParameters(st, bindings);
                return st.executeUpdate();
            }
            finally {
                st.close();
            }
        }
        finally {
            c.close();
        }
    }
}
