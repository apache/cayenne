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

package org.apache.cayenne.dba;

import java.util.ArrayList;
import java.util.List;

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.unit.UnitDbAdapter;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class PkGeneratorTest extends ServerCase {

    @Inject
    private UnitDbAdapter accessStackAdapter;

    @Inject
    private DataNode node;

    private PkGenerator pkGenerator;
    private DbEntity paintingEntity;

    @Override
    protected void setUpAfterInjection() throws Exception {

        pkGenerator = node.getAdapter().getPkGenerator();
        paintingEntity = node.getEntityResolver().getDbEntity("PAINTING");

        List<DbEntity> list = new ArrayList<DbEntity>();
        list.add(paintingEntity);
        pkGenerator.createAutoPk(node, list);
        pkGenerator.reset();
    }

    public void testGeneratePkForDbEntity() throws Exception {
        List<Object> pkList = new ArrayList<Object>();

        int testSize = (pkGenerator instanceof JdbcPkGenerator)
                ? ((JdbcPkGenerator) pkGenerator).getPkCacheSize() * 2
                : 25;
        if (testSize < 25) {
            testSize = 25;
        }

        for (int i = 0; i < testSize; i++) {
            Object pk = pkGenerator.generatePk(node, paintingEntity
                    .getPrimaryKeys()
                    .iterator()
                    .next());
            assertNotNull(pk);
            assertTrue(pk instanceof Number);

            // check that the number is continuous
            // of course this assumes a single-threaded test
            if (accessStackAdapter.supportsBatchPK() && pkList.size() > 0) {
                Number last = (Number) pkList.get(pkList.size() - 1);
                assertEquals(last.intValue() + 1, ((Number) pk).intValue());
            }

            pkList.add(pk);
        }
    }
}
