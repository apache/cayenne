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

package org.apache.cayenne.dba.oracle;

import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.log.JdbcEventLogger;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbKeyGenerator;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class OraclePkGeneratorTest extends ServerCase {
    
    @Inject
    protected JdbcEventLogger logger;
    
    @Inject
    protected AdhocObjectFactory objectFactory;

    private OraclePkGenerator pkGenerator;

    @Override
    protected void setUpAfterInjection() throws Exception {
        OracleAdapter adapter = objectFactory.newInstance(OracleAdapter.class, OracleAdapter.class.getName());
        pkGenerator = new OraclePkGenerator(adapter);
    }

    public void testSequenceNameDefault() throws Exception {
        DbEntity entity = new DbEntity("TEST_ENTITY");
        assertEquals("pk_test_entity", pkGenerator.sequenceName(entity));
    }

    public void testSequenceNameCustom1() throws Exception {
        DbEntity entity = new DbEntity("TEST_ENTITY");
        DbKeyGenerator customGenerator = new DbKeyGenerator();
        customGenerator.setGeneratorType(DbKeyGenerator.ORACLE_TYPE);
        customGenerator.setGeneratorName("CUSTOM_GENERATOR");
        entity.setPrimaryKeyGenerator(customGenerator);
        assertEquals("custom_generator", pkGenerator.sequenceName(entity));
    }

    public void testSequenceNameCustom2() throws Exception {
        DbEntity entity = new DbEntity("TEST_ENTITY");
        DbKeyGenerator customGenerator = new DbKeyGenerator();
        customGenerator.setGeneratorType(DbKeyGenerator.NAMED_SEQUENCE_TABLE_TYPE);
        customGenerator.setGeneratorName("CUSTOM_GENERATOR");
        assertEquals("pk_test_entity", pkGenerator.sequenceName(entity));
    }
}
