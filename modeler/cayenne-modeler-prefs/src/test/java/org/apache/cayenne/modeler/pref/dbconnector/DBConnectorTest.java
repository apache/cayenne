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

package org.apache.cayenne.modeler.pref.dbconnector;

import org.apache.cayenne.configuration.DataSourceDescriptor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DBConnectorTest {

    @Test
    public void copyToConnectorReturnsFalseWhenIdentical() {
        DBConnector src = connector("jdbc:h2:mem", "sa", "pw", "org.h2.Driver", "org.h2.H2Adapter");
        DBConnector dst = connector("jdbc:h2:mem", "sa", "pw", "org.h2.Driver", "org.h2.H2Adapter");

        assertFalse(src.copyTo(dst));
    }

    @Test
    public void copyToConnectorReturnsTrueOnChange() {
        DBConnector src = connector("jdbc:h2:new", "sa", "pw", "org.h2.Driver", "org.h2.H2Adapter");
        DBConnector dst = connector("jdbc:h2:old", "sa", "pw", "org.h2.Driver", "org.h2.H2Adapter");

        assertTrue(src.copyTo(dst));
        assertEquals("jdbc:h2:new", dst.getUrl());
    }

    @Test
    public void copyToConnectorCopiesAllFiveFields() {
        DBConnector src = connector("url", "user", "pass", "driver", "adapter");
        DBConnector dst = new DBConnector();

        src.copyTo(dst);

        assertEquals("url", dst.getUrl());
        assertEquals("user", dst.getUserName());
        assertEquals("pass", dst.getPassword());
        assertEquals("driver", dst.getJdbcDriver());
        assertEquals("adapter", dst.getDbAdapter());
    }

    @Test
    public void copyToDataSourceDescriptorCopiesDriverUrlUserPassword() {
        DBConnector src = connector("jdbc:pg://db", "admin", "secret", "org.postgresql.Driver", "PgAdapter");
        DataSourceDescriptor dst = new DataSourceDescriptor();

        src.copyTo(dst);

        assertEquals("jdbc:pg://db", dst.getDataSourceUrl());
        assertEquals("admin", dst.getUserName());
        assertEquals("secret", dst.getPassword());
        assertEquals("org.postgresql.Driver", dst.getJdbcDriver());
    }

    @Test
    public void copyToDataSourceDescriptorDoesNotCopyDbAdapter() {
        // All 4 copyable fields already match; only dbAdapter differs.
        // copyTo must return false — dbAdapter is not part of the DataSourceDescriptor copy.
        DBConnector src = connector("url", "user", "pass", "driver", "SomeAdapter");
        DataSourceDescriptor dst = new DataSourceDescriptor();
        dst.setDataSourceUrl("url");
        dst.setUserName("user");
        dst.setPassword("pass");
        dst.setJdbcDriver("driver");

        assertFalse(src.copyTo(dst));
    }

    @Test
    public void copyToConnectorHandlesNullFields() {
        DBConnector src = new DBConnector();
        DBConnector dst = connector("url", "user", "pass", "driver", "adapter");

        assertTrue(src.copyTo(dst));
        assertNull(dst.getUrl());
        assertNull(dst.getUserName());
        assertNull(dst.getPassword());
        assertNull(dst.getJdbcDriver());
        assertNull(dst.getDbAdapter());
    }

    private static DBConnector connector(
            String url,
            String userName,
            String password,
            String jdbcDriver,
            String dbAdapter) {

        DBConnector c = new DBConnector();
        c.setUrl(url);
        c.setUserName(userName);
        c.setPassword(password);
        c.setJdbcDriver(jdbcDriver);
        c.setDbAdapter(dbAdapter);
        return c;
    }
}
