package org.apache.cayenne.joda.access.types;

import com.mockrunner.mock.jdbc.MockResultSet;
import junit.framework.TestCase;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * **************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * **************************************************************
 */

public abstract class JodaTestCase extends TestCase {

    ResultSet resultSet(Object value) throws SQLException {
        MockResultSet rs = new MockResultSet("Test");
        rs.addColumn("Col");
        rs.addRow(new Object[]{value});
        rs.next();
        return rs;
    }

}
