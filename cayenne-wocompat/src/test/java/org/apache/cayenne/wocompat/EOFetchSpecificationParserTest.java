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

package org.apache.cayenne.wocompat;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.QueryDescriptor;
import org.apache.cayenne.map.SelectQueryDescriptor;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EOFetchSpecificationParserTest {

    @Test
    public void loadedFetchSpecification() throws Exception {

        URL url = getClass().getClassLoader().getResource("wotests/fetchspec.eomodeld/");
        assertNotNull(url);

        DataMap map = new EOModelProcessor().loadEOModel(url);

        QueryDescriptor descriptor = map.getQueryDescriptor("Entity1_E1FS1");
        assertNotNull(descriptor);
        SelectQueryDescriptor selectDescriptor = assertInstanceOf(SelectQueryDescriptor.class, descriptor);
        assertSame(map.getObjEntity("Entity1"), selectDescriptor.getRoot());

        assertNotNull(selectDescriptor.getQualifier());
        assertEquals(
                "(name = \"aa\") and (db:ID >= 7) and ((e2.name = \"bb\") or (db:e2.ID != 5))",
                selectDescriptor.getQualifier().toString());

        ObjectSelect<?> query = selectDescriptor.buildQuery();

        assertNotNull(query.getPrefetches());
        Collection<PrefetchTreeNode> prefetches = query.getPrefetches().getChildren();
        assertEquals(1, prefetches.size());
        assertEquals("e2", prefetches.iterator().next().getName());

        Collection<Ordering> orderings = query.getOrderings();
        assertEquals(1, orderings.size());
        Ordering ordering = orderings.iterator().next();
        assertEquals("name", ordering.getSortSpecString());
        assertFalse(ordering.isAscending());

        assertTrue(query.isFetchingDataRows());
        assertEquals(500, query.getLimit());
        assertEquals(0, query.getPageSize());
        assertTrue(query.isDistinct());
    }
}
