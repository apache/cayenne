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
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.unit.CayenneCase;

public class PkGeneratorTest extends CayenneCase {

    protected PkGenerator pkGen;
    protected DataNode node;
    protected DbEntity paintEnt;

    @Override
    protected void setUp() throws Exception {
        deleteTestData();
        node = getDomain().getDataNodes().iterator().next();
        pkGen = node.getAdapter().getPkGenerator();
        paintEnt = getDbEntity("PAINTING");
        List list = new ArrayList();
        list.add(paintEnt);
        pkGen.createAutoPk(node, list);
        pkGen.reset();
    }

    public void testGeneratePkForDbEntity() throws Exception {
        List pkList = new ArrayList();

        int testSize = (pkGen instanceof JdbcPkGenerator) ? ((JdbcPkGenerator) pkGen)
                .getPkCacheSize() * 2 : 25;
        if (testSize < 25) {
            testSize = 25;
        }

        for (int i = 0; i < testSize; i++) {
            Object pk = pkGen.generatePk(node, paintEnt
                    .getPrimaryKeys()
                    .iterator()
                    .next());
            assertNotNull(pk);
            assertTrue(pk instanceof Number);

            // check that the number is continuous
            // of course this assumes a single-threaded test
            if (getAccessStackAdapter().supportsBatchPK() && pkList.size() > 0) {
                Number last = (Number) pkList.get(pkList.size() - 1);
                assertEquals(last.intValue() + 1, ((Number) pk).intValue());
            }

            pkList.add(pk);
        }
    }

    /**
     * @deprecated since 3.0
     */
    public void testBinaryPK1() throws Exception {
        if (!(pkGen instanceof JdbcPkGenerator)) {
            return;
        }

        DbEntity artistEntity = getDbEntity("ARTIST");
        assertNull(((JdbcPkGenerator) pkGen).binaryPK(artistEntity));
    }

    /**
     * @deprecated since 3.0
     */
    public void testBinaryPK2() throws Exception {
        if (!(pkGen instanceof JdbcPkGenerator)) {
            return;
        }

        DbEntity binPKEntity = getDbEntity("BINARY_PK_TEST1");
        assertNotNull(((JdbcPkGenerator) pkGen).binaryPK(binPKEntity));
    }
}
