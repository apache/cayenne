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
import java.util.Collection;

class UpdateTemplate {

    DBHelper parent;

    public UpdateTemplate(DBHelper parent) {
        this.parent = parent;
    }

    protected void bindParameters(
            PreparedStatement statement,
            Collection<Object> bindings,
            Collection<Integer> bindingTypes) throws SQLException {

        if (bindings != null && !bindings.isEmpty()) {

            Object[] values = bindings.toArray();
            Integer[] types = bindingTypes.toArray(new Integer[bindingTypes.size()]);

            for (int i = 0; i < values.length; i++) {

                if (values[i] == null) {
                    if (types[i] != SQLBuilder.NO_TYPE) {
                        statement.setNull(i + 1, types[i]);
                    }
                    else {
                        throw new IllegalStateException(
                                "No type information for null value at index " + i);
                    }
                }
                else {
                    if (types[i] != SQLBuilder.NO_TYPE) {
                        statement.setObject(i + 1, values[i], types[i]);
                    }
                    else {
                        statement.setObject(i + 1, values[i]);
                    }
                }
            }
        }
    }

    int execute(String sql, Collection<Object> bindings, Collection<Integer> bindingTypes)
            throws SQLException {
        UtilityLogger.log(sql);
        Connection c = parent.getConnection();
        try {

            PreparedStatement st = c.prepareStatement(sql);

            int count;
            try {
                bindParameters(st, bindings, bindingTypes);
                count = st.executeUpdate();
            }
            finally {
                st.close();
            }

            c.commit();
            return count;
        }
        finally {
            c.close();
        }
    }
}
