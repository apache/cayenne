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

package org.apache.cayenne.wocompat;

import java.net.URL;
import java.util.Collection;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.query.PrefetchTreeNode;

public class EOQueryTest extends TestCase {

    public void testConstructor() throws Exception {
        
        URL url = getClass().getClassLoader().getResource("wotests/fetchspec.eomodeld/");
        assertNotNull(url);

        EOModelProcessor processor = new EOModelProcessor();
        DataMap map = processor.loadEOModel(url);

        Map fspecMap = (Map) PropertyListSerialization.propertyListFromStream(getClass()
                .getClassLoader()
                .getResourceAsStream("wotests/fetchspec.eomodeld/Entity1.fspec"));
        assertNotNull(fspecMap);
        assertNotNull(fspecMap.get("E1FS1"));

        EOQuery query = new EOQuery(map.getObjEntity("Entity1"), (Map) fspecMap
                .get("E1FS1"));
        assertNull(query.getName());

        assertNotNull(query.getQualifier());
        assertEquals(
                "(name = \"aa\") and (db:ID >= 7) and ((e2.name = \"bb\") or (db:e2.ID != 5))",
                query.getQualifier().toString());
        
        assertNotNull(query.getPrefetchTree());
        
        Collection children= query.getPrefetchTree().getChildren();
        assertEquals(1, children.size());
        assertEquals("e2", ((PrefetchTreeNode) children.iterator().next()).getName());
        
        assertTrue(query.isFetchingDataRows());
        assertEquals(500, query.getFetchLimit());
        assertEquals(0, query.getPageSize());
        assertTrue(query.isDistinct());
    }
}
