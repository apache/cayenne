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
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.NamedQuery;
import org.apache.cayenne.map.NamedSelectQuery;
import org.apache.cayenne.query.MappedQuery;
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.query.resolved.ResolvedQuery;
import org.apache.cayenne.ql.ParsedObjectSelect;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EOFetchSpecificationParserTest {

    @Test
    public void loadedFetchSpecification() throws Exception {

        URL url = getClass().getClassLoader().getResource("wotests/fetchspec.eomodeld/");
        assertNotNull(url);

        DataMap map = new EOModelProcessor().loadEOModel(url);

        NamedQuery namedQuery = map.getNamedQuery("Entity1_E1FS1");
        assertNotNull(namedQuery);
        NamedSelectQuery namedSelectQuery = assertInstanceOf(NamedSelectQuery.class, namedQuery);

        ParsedObjectSelect query = ParsedObjectSelect.parse(namedSelectQuery.getQl());
        assertEquals("Entity1", query.getEntityName());

        assertNotNull(query.getWhere());
        assertEquals(
                "(name = \"aa\") and (db:ID >= 7) and ((e2.name = \"bb\") or (db:e2.ID != 5))",
                query.getWhere().toString());

        assertNotNull(query.getPrefetches());
        Collection<PrefetchTreeNode> prefetches = query.getPrefetches().getChildren();
        assertEquals(1, prefetches.size());
        assertEquals("e2", prefetches.iterator().next().getName());

        Collection<Ordering> orderings = query.getOrderings();
        assertEquals(1, orderings.size());
        Ordering ordering = orderings.iterator().next();
        assertEquals("name", ordering.getSortSpecString());
        assertFalse(ordering.isAscending());

        assertTrue(query.toQL().startsWith("select distinct self from Entity1 "), query.toQL());

        // the settings of the named query are applied when it is run as a mapped query
        assertTrue(namedSelectQuery.isFetchingDataRows());
        ResolvedQuery resolved = MappedQuery.query("Entity1_E1FS1").resolve(new EntityResolver(List.of(map)));
        assertEquals(500, resolved.fetchLimit());
        assertTrue(resolved.fetchingDataRows());
        assertEquals(0, resolved.pageSize());
    }
}
