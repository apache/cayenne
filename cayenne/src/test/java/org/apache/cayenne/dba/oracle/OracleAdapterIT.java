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

package org.apache.cayenne.dba.oracle;

import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.query.InsertBatchQuery;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.CayenneTestsExt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.sql.Types;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OracleAdapterIT {

    @RegisterExtension
    static final CayenneTestsExt env = CayenneTestsExt.forProject(CayenneProjects.LOB_PROJECT);

    private CayenneRuntime runtime;
    private AdhocObjectFactory objectFactory;

    @BeforeEach
    public void setUp() {
        runtime = env.runtime();
        objectFactory = env.getInstance(AdhocObjectFactory.class);
    }

    @Test
    public void updatesLOBColumns() throws Exception {
        DataMap map = runtime.getDataDomain().getDataMap("lob");
        assertTrue(OracleAdapter.updatesLOBColumns(new InsertBatchQuery(map
                .getDbEntity("BLOB_TEST"), 1)));
        assertTrue(OracleAdapter.updatesLOBColumns(new InsertBatchQuery(map
                .getDbEntity("CLOB_TEST"), 1)));
        assertFalse(OracleAdapter.updatesLOBColumns(new InsertBatchQuery(map
                .getDbEntity("TEST"), 1)));
    }

    @Test
    public void timestampMapping() throws Exception {
        
        OracleAdapter adapter = objectFactory.newInstance(
                OracleAdapter.class, 
                OracleAdapter.class.getName());

        String[] types = adapter.externalTypesForJdbcType(Types.TIMESTAMP);
        assertNotNull(types);
        assertEquals(1, types.length);
        assertEquals("TIMESTAMP", types[0]);
    }
}
