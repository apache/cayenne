/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.access.types;

import java.sql.SQLException;
import java.sql.Types;

import com.mockrunner.mock.jdbc.MockResultSet;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;


/**
 */
public class BooleanTypeTest {

    @Test
    public void testClassName() {
        BooleanType type = new BooleanType();
        assertEquals(Boolean.class.getName(), type.getClassName());
    }

    @Test
    public void testMaterializeObjectFromResultSet() throws Exception {
        MockResultSet rs = new MockResultSet("") {

            @Override
            public boolean getBoolean(int i) throws SQLException {
                return (i + 2) % 2 == 0;
            }
        };

        BooleanType type = new BooleanType();

        // assert identity as well as equality (see CAY-320)
        assertSame(Boolean.FALSE, type.materializeObject(rs, 1, Types.BIT));
        assertSame(Boolean.TRUE, type.materializeObject(rs, 2, Types.BIT));
    }
}
