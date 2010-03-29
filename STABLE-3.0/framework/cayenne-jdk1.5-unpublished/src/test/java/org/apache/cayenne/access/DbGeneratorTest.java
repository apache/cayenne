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

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.unit.CayenneCase;

/**
 * Test cases for DbGenerator.
 * 
 */
public class DbGeneratorTest extends CayenneCase {

    protected DbGenerator gen;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        gen = new DbGenerator(getNode().getAdapter(), getDomain().getMap("testmap"));
    }

    public void testAdapter() throws Exception {
        assertSame(getNode().getAdapter(), gen.getAdapter());
    }

    public void testPkFilteringLogic() throws Exception {
        DataMap map = getDomain().getMap("testmap");
        DbEntity artistExhibit = map.getDbEntity("ARTIST_EXHIBIT");
        DbEntity exhibit = map.getDbEntity("EXHIBIT");

        // sanity check
        assertNotNull(artistExhibit);
        assertNotNull(exhibit);
        assertNotNull(gen.dbEntitiesRequiringAutoPK);

        // real test
        assertTrue(gen.dbEntitiesRequiringAutoPK.contains(exhibit));
        assertFalse(gen.dbEntitiesRequiringAutoPK.contains(artistExhibit));
    }

    public void testCreatePkSupport() throws Exception {
        assertTrue(gen.shouldCreatePKSupport());
        gen.setShouldCreatePKSupport(false);
        assertFalse(gen.shouldCreatePKSupport());

    }

    public void testShouldCreateTables() throws Exception {
        assertTrue(gen.shouldCreateTables());
        gen.setShouldCreateTables(false);
        assertFalse(gen.shouldCreateTables());
    }

    public void testDropPkSupport() throws Exception {

        assertFalse(gen.shouldDropPKSupport());
        gen.setShouldDropPKSupport(true);
        assertTrue(gen.shouldDropPKSupport());
    }

    public void testShouldDropTables() throws Exception {
        assertFalse(gen.shouldDropTables());
        gen.setShouldDropTables(true);
        assertTrue(gen.shouldDropTables());
    }
}
