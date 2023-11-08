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

package org.apache.cayenne.util;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class ListResponseTest {

    @Test
    public void testCreation() {

        Object object = new Object();
        ListResponse r = new ListResponse(object);

        assertEquals(1, r.size());

        assertTrue(r.next());
        assertTrue(r.isList());

        List currentList = r.currentList();
        assertEquals(1, currentList.size());
        assertTrue(currentList.contains(object));

        assertFalse(r.next());

        r.reset();
        assertTrue(r.next());

        assertSame(currentList, r.firstList());
    }

    @Test
    public void testNext() {
        List<Integer> result = List.of(1, 2, 3);
        ListResponse r = new ListResponse(result);

        assertTrue(r.next());
        assertTrue(r.isList());
        assertEquals(result, r.currentList());
        assertFalse(r.next());
    }

    @Test
    public void testSerialization() throws Exception {

        ListResponse r = new ListResponse(67);

        ListResponse sr = Util.cloneViaSerialization(r);
        assertNotNull(sr);
        assertEquals(1, sr.size());

        assertTrue(sr.firstList().contains(67));
    }

}
