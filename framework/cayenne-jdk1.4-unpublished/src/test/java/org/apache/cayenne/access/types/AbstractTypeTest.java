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

package org.apache.cayenne.access.types;

import java.sql.Types;

import junit.framework.TestCase;

import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.validation.ValidationResult;

import com.mockrunner.mock.jdbc.MockConnection;
import com.mockrunner.mock.jdbc.MockPreparedStatement;

/**
 * @author Andrus Adamchik
 */
public class AbstractTypeTest extends TestCase {

    /**
     * @deprecated since 3.0 as validation should not be done at the DataNode level.
     */
    public void testValidateProperty() {

        // should always return true... not sure how else to test it?
        MockAbstractType type = new MockAbstractType("testclass");
        assertTrue(type.validateProperty(new Object(),
                "dummy",
                new Object(),
                new DbAttribute("test"),
                new ValidationResult()));
    }

    public void testToString() {
        MockAbstractType type = new MockAbstractType("testclass");
        String string = type.toString();
        assertNotNull(string);
        assertTrue(string.indexOf("testclass") > 0);
    }

    public void testSetJdbcObject() throws Exception {
        MockConnection c = new MockConnection();
        MockPreparedStatement st = new MockPreparedStatement(c, "");
        Object value = new Object();

        MockAbstractType type = new MockAbstractType("testclass");
        type.setJdbcObject(st, value, 35, Types.INTEGER, -1);

        assertSame(value, st.getParameter(35));
    }
}
