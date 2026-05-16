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

package org.apache.cayenne.access;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.datasource.UnmanagedPoolingDataSource;
import org.apache.cayenne.query.SQLAction;
import org.apache.cayenne.query.SQLActionVisitor;
import org.apache.cayenne.query.SQLSelect;
import org.apache.cayenne.unit.runtime.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DataNodeQueryExceptionsIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    @Test
    public void queryException() {
        SQLSelect<Object> throwingSS = new SQLSelect<Object>(Object.class, "SELECT 1") {
            @Override
            public SQLAction createSQLAction(SQLActionVisitor visitor) {
                assertEquals(1, activeConnections());
                throw new CayenneRuntimeException("Emulated exception");
            }
        };

        assertEquals(0, activeConnections());
        assertThrows(CayenneRuntimeException.class, () -> env.dataNode().performQueries(
                Collections.singletonList(throwingSS),
                new MockOperationObserver()));

        assertEquals(0, activeConnections());
    }

    @Test
    public void queryError() {
        SQLSelect<Object> throwingSS = new SQLSelect<Object>(Object.class, "SELECT 1") {
            @Override
            public SQLAction createSQLAction(SQLActionVisitor visitor) {
                assertEquals(1, activeConnections());
                throw new Error("Emulated error");
            }
        };

        assertEquals(0, activeConnections());
        assertThrows(Error.class, () -> env.dataNode().performQueries(
                Collections.singletonList(throwingSS),
                new MockOperationObserver()));

        assertEquals(0, activeConnections());
    }

    int activeConnections() {
        try {
            UnmanagedPoolingDataSource ds = CayenneTestsEnv.COMMON_SCHEMA.dataSource().unwrap(UnmanagedPoolingDataSource.class);

            Method poolSize = UnmanagedPoolingDataSource.class.getDeclaredMethod("poolSize");
            poolSize.setAccessible(true);

            Method availableSize = UnmanagedPoolingDataSource.class.getDeclaredMethod("availableSize");
            availableSize.setAccessible(true);

            return (int) poolSize.invoke(ds) - (int) availableSize.invoke(ds);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
