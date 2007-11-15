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
import java.sql.Statement;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.cayenne.access.types.ExtendedTypeMap;

import com.mockrunner.mock.jdbc.MockConnection;
import com.mockrunner.mock.jdbc.MockResultSet;
import com.mockrunner.mock.jdbc.MockStatement;

/**
 * @author Andrus Adamchik
 */
public class JDBCResultIteratorTest extends TestCase {

    public void testClosingConnection() throws Exception {
        JDBCResultIterator it = makeIterator();

        it.setClosingConnection(true);
        assertTrue(it.isClosingConnection());

        it.setClosingConnection(false);
        assertFalse(it.isClosingConnection());
    }

    public void testNextDataRow() throws Exception {
        JDBCResultIterator it = makeIterator();

        Map row = it.nextDataRow();

        assertNotNull(row);
        assertEquals(1, row.size());
        assertEquals("1", row.get("a"));
    }

    public void testClose() throws Exception {
        MockConnection c = new MockConnection();
        MockStatement s = new MockStatement(c);
        MockResultSet rs = new MockResultSet("rs");
        rs.addColumn("a", new Object[] {
                "1", "2", "3"
        });
        RowDescriptor descriptor = new RowDescriptor(rs, new ExtendedTypeMap());

        JDBCResultIterator it = new JDBCResultIterator(c, s, rs, descriptor, 0);

        assertFalse(rs.isClosed());
        assertFalse(s.isClosed());
        assertFalse(c.isClosed());

        it.setClosingConnection(false);
        it.close();

        assertTrue(rs.isClosed());
        assertTrue(s.isClosed());
        assertFalse(c.isClosed());
    }

    JDBCResultIterator makeIterator() throws Exception {

        Connection c = new MockConnection();
        Statement s = new MockStatement(c);
        MockResultSet rs = new MockResultSet("rs");
        rs.addColumn("a", new Object[] {
                "1", "2", "3"
        });

        RowDescriptor descriptor = new RowDescriptor(rs, new ExtendedTypeMap());
        return new JDBCResultIterator(c, s, rs, descriptor, 0);
    }

}
