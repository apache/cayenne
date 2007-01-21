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

package org.apache.cayenne.access;

import java.util.List;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.unit.MultiNodeCase;

public class DbGeneratorCrossDBTest extends MultiNodeCase {

    public void testCreateFkConstraintsQueries() {
        // can't test this if adapter doesn't support constraints
        if (!getNode2().getAdapter().supportsFkConstraints()) {
            return;
        }

        DataMap m2 = getDomain().getMap("map-db2");
        DbEntity m2e2 = m2.getDbEntity("CROSSDB_M2E2");

        DbGenerator g2 = new DbGenerator(getNode2().getAdapter(), m2, null, getDomain());

        List fk = g2.createFkConstraintsQueries(m2e2);
        assertNotNull(fk);

        // same-db FK should be included
        // cross-db FK should not be included
        assertEquals(1, fk.size());
    }
}
