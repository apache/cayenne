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
package org.apache.cayenne.access;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.dba.AutoAdapter;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.hsqldb.HSQLDBAdapter;

/**
 * A helper class that handles postprocessing after the schema generation operation. E.g.
 * some databases require a checkpoint command to be run for the schema changes to be
 * flushed to disk.
 * 
 */
class DbGeneratorPostprocessor {

    private static final Map<String, HSQLDBPostprocessor> postprocessors;

    static {
        postprocessors = new HashMap<String, HSQLDBPostprocessor>();
        postprocessors.put(HSQLDBAdapter.class.getName(), new HSQLDBPostprocessor());
    }

    void execute(Connection connection, DbAdapter adapter) throws SQLException {

        if (adapter != null) {
            Postprocessor postprocessor = postprocessors.get(adapter
                    .getClass()
                    .getName());
            if (postprocessor != null) {
                postprocessor.execute(connection);
            }
        }
    }

    static abstract class Postprocessor {

        abstract void execute(Connection c) throws SQLException;
    }

    static class HSQLDBPostprocessor extends Postprocessor {

        @Override
        void execute(Connection c) throws SQLException {
            PreparedStatement st = c.prepareStatement("CHECKPOINT");
            try {
                st.execute();
            }
            finally {
                st.close();
            }
        }
    }
}
