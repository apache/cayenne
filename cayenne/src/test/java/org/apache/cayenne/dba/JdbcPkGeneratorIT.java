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
package org.apache.cayenne.dba;

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.dba.derby.DerbyPkGenerator;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.SchemaBuilder;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertTrue;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class JdbcPkGeneratorIT extends RuntimeCase {

    @Inject
    private DbAdapter adapter;

    @Inject
    private DataNode node;
    
    @Inject
    private SchemaBuilder schemaBuilder;

    @Before
    public void setUp() throws Exception {
        schemaBuilder.dropPKSupport();
    }
    
    @After
    public void tearDown() throws Exception {

        if (JdbcPkGenerator.class.isAssignableFrom(adapter.getPkGenerator().getClass())) {
            // reset PK gen properly before updating PKs in DB
            JdbcPkGenerator pkGenerator = (JdbcPkGenerator) adapter.getPkGenerator();

            pkGenerator.setPkStartValue(JdbcPkGenerator.DEFAULT_PK_START_VALUE);

            schemaBuilder.dropPKSupport();
            schemaBuilder.createPKSupport();
        }
    }

    @Test
    public void testLongPk() throws Exception {

        if (!JdbcPkGenerator.class.isAssignableFrom(adapter.getPkGenerator().getClass())) {
            return;
        }

        DbEntity artistEntity = node.getEntityResolver().getObjEntity(Artist.class).getDbEntity();

        DbAttribute pkAttribute = artistEntity.getAttribute(Artist.ARTIST_ID_PK_COLUMN);

        JdbcPkGenerator pkGenerator = (JdbcPkGenerator) adapter.getPkGenerator();

        pkGenerator.setPkStartValue(Integer.MAX_VALUE * 2L);
        if (!JdbcPkGenerator.class.equals(adapter.getPkGenerator().getClass()) &&
        		!DerbyPkGenerator.class.equals(adapter.getPkGenerator().getClass())) { // AUTO_PK_SUPPORT doesn't allow dropping PK support for a single entity
            pkGenerator.dropAutoPk(node, Collections.singletonList(artistEntity));
        }
        pkGenerator.createAutoPk(node, Collections.singletonList(artistEntity));
        pkGenerator.reset();
        
        Object pk = pkGenerator.generatePk(node, pkAttribute);
        assertTrue(pk instanceof Long);
        assertTrue("PK is too small: " + pk, ((Long) pk).longValue() > Integer.MAX_VALUE);
    }
}
