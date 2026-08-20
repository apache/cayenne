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
import org.apache.cayenne.unit.CayenneProjects;
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

    @BeforeEach
    public void setUp() throws Exception {
        // TODO: we should have a dedicated DbSchemaManager for such destructive operations working off of its own DB
        CayenneTestsEnv.COMMON_SCHEMA.dropPKSupport();
    }

    @AfterEach
    public void tearDown() throws Exception {

        // the test leaves PK support in the DB starting way past Integer.MAX_VALUE, so rebuild it for whoever runs
        // next.

        // TODO: we should have a dedicated DbSchemaManager for such destructive operations working off of its own DB
        CayenneTestsEnv.COMMON_SCHEMA.dropPKSupport();
        CayenneTestsEnv.COMMON_SCHEMA.createPKSupport();
    }

    @Test
    public void longPk() {

        DataNode node = env.dataNode();

        DbEntity artistEntity = node.getEntityResolver().getObjEntity(Artist.class).getDbEntity();
        DbAttribute pkAttribute = artistEntity.getAttribute(Artist.ARTIST_ID_PK_COLUMN);

        JdbcPkGenerator pkGenerator = (JdbcPkGenerator) node.getPkGenerator();

        pkGenerator.setPkStartValue(Integer.MAX_VALUE * 2L);
        if (!JdbcPkGenerator.class.equals(node.getPkGenerator().getClass()) &&
                // AUTO_PK_SUPPORT doesn't allow dropping PK support for a single entity
                !DerbyPkGenerator.class.equals(node.getPkGenerator().getClass())) {
            pkGenerator.dropAutoPk(node, Collections.singletonList(artistEntity));
        }
        pkGenerator.createAutoPk(node, Collections.singletonList(artistEntity));
        pkGenerator.reset();

        Object pk = pkGenerator.generatePk(node, pkAttribute, null);
        assertInstanceOf(Long.class, pk);
        assertTrue((Long) pk > Integer.MAX_VALUE, "PK is too small: " + pk);
    }
}
