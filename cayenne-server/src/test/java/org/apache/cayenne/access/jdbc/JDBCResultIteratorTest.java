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

import static org.mockito.Mockito.mock;

import java.sql.Connection;
import java.sql.Statement;

import junit.framework.TestCase;

import org.apache.cayenne.DataRow;
import org.apache.cayenne.access.types.ExtendedTypeMap;
import org.apache.cayenne.query.MockQueryMetadata;

import com.mockrunner.mock.jdbc.MockConnection;
import com.mockrunner.mock.jdbc.MockResultSet;
import com.mockrunner.mock.jdbc.MockStatement;

public class JDBCResultIteratorTest extends TestCase {

    public void testNextDataRow() throws Exception {
        Connection c = new MockConnection();
        Statement s = new MockStatement(c);
        MockResultSet rs = new MockResultSet("rs");
        rs.addColumn("a", new Object[] { "1", "2", "3" });
        
        RowDescriptor descriptor = new RowDescriptorBuilder().setResultSet(rs).getDescriptor(new ExtendedTypeMap());
        RowReader<?> rowReader = new DefaultRowReaderFactory().createRowReader(descriptor, new MockQueryMetadata());

        JDBCResultIterator it = new JDBCResultIterator(s, rs, rowReader);

        DataRow row = (DataRow) it.nextRow();

        assertNotNull(row);
        assertEquals(1, row.size());
        assertEquals("1", row.get("a"));
    }

    public void testClose() throws Exception {
        Connection c = new MockConnection();
        MockStatement s = new MockStatement(c);
        MockResultSet rs = new MockResultSet("rs");
        rs.addColumn("a", new Object[] { "1", "2", "3" });

        RowReader<?> rowReader = mock(RowReader.class);

        JDBCResultIterator it = new JDBCResultIterator(s, rs, rowReader);

        assertFalse(rs.isClosed());
        assertFalse(s.isClosed());
        assertFalse(c.isClosed());

        it.close();

        assertTrue(rs.isClosed());
        assertTrue(s.isClosed());
    }

}
