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
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.runtime.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JdbcPkGeneratorIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    private DataNode node;

    @BeforeEach
    public void setUp() throws Exception {
        node = env.dataNode();
        CayenneTestsEnv.SCHEMAS.dropPKSupport();
    }

    @AfterEach
    public void tearDown() throws Exception {

        if (JdbcPkGenerator.class.isAssignableFrom(node.getAdapter().getPkGenerator().getClass())) {
            // reset PK gen properly before updating PKs in DB
            JdbcPkGenerator pkGenerator = (JdbcPkGenerator) node.getAdapter().getPkGenerator();

            pkGenerator.setPkStartValue(JdbcPkGenerator.DEFAULT_PK_START_VALUE);

            CayenneTestsEnv.SCHEMAS.dropPKSupport();
            CayenneTestsEnv.SCHEMAS.createPKSupport();
        }
    }

    @Test
    public void longPk() throws Exception {

        if (!JdbcPkGenerator.class.isAssignableFrom(node.getAdapter().getPkGenerator().getClass())) {
            return;
        }

        DbEntity artistEntity = node.getEntityResolver().getObjEntity(Artist.class).getDbEntity();

        DbAttribute pkAttribute = artistEntity.getAttribute(Artist.ARTIST_ID_PK_COLUMN);

        JdbcPkGenerator pkGenerator = (JdbcPkGenerator) node.getAdapter().getPkGenerator();

        pkGenerator.setPkStartValue(Integer.MAX_VALUE * 2L);
        if (!JdbcPkGenerator.class.equals(node.getAdapter().getPkGenerator().getClass()) &&
        		!DerbyPkGenerator.class.equals(node.getAdapter().getPkGenerator().getClass())) { // AUTO_PK_SUPPORT doesn't allow dropping PK support for a single entity
            pkGenerator.dropAutoPk(node, Collections.singletonList(artistEntity));
        }
        pkGenerator.createAutoPk(node, Collections.singletonList(artistEntity));
        pkGenerator.reset();
        
        Object pk = pkGenerator.generatePk(node, pkAttribute);
        assertInstanceOf(Long.class, pk);
        assertTrue((Long) pk > Integer.MAX_VALUE, "PK is too small: " + pk);
    }
}
