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

import junit.framework.TestCase;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;

public class DefaultQuotingStrategyTest extends TestCase {

    public void testQuotedIdentifer() {

        DataMap dm = new DataMap();
        dm.setQuotingSQLIdentifiers(true);
        DbEntity de = new DbEntity();
        de.setDataMap(dm);

        DefaultQuotingStrategy strategy = new DefaultQuotingStrategy("[", "]");
        assertEquals("[a]", strategy.quotedIdentifier(de, "a"));
        assertEquals("[a]", strategy.quotedIdentifier(de, null, null, "a"));
        assertEquals("[c].[b].[a]", strategy.quotedIdentifier(de, "c", "b", "a"));
    }

    public void testUnQuotedIdentifer() {

        DataMap dm = new DataMap();
        dm.setQuotingSQLIdentifiers(false);
        DbEntity de = new DbEntity();
        de.setDataMap(dm);

        DefaultQuotingStrategy strategy = new DefaultQuotingStrategy("[", "]");
        assertEquals("a", strategy.quotedIdentifier(de, "a"));
        assertEquals("a", strategy.quotedIdentifier(de, null, null, "a"));
        assertEquals("c.b.a", strategy.quotedIdentifier(de, "c", "b", "a"));
    }
}
