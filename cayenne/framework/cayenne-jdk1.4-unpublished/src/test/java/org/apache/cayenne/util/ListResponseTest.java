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

package org.apache.cayenne.util;

import java.util.List;

import junit.framework.TestCase;

import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.remote.hessian.service.HessianUtil;

public class ListResponseTest extends TestCase {

    public void testCreation() throws Exception {

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

    public void testSerialization() throws Exception {

        ListResponse r = new ListResponse(new Integer(67));

        ListResponse sr = (ListResponse) Util.cloneViaSerialization(r);
        assertNotNull(sr);
        assertEquals(1, sr.size());

        assertTrue(sr.firstList().contains(new Integer(67)));
    }

    public void testSerializationWithHessian() throws Exception {

        ListResponse r = new ListResponse(new Integer(67));

        ListResponse sr = (ListResponse) HessianUtil.cloneViaClientServerSerialization(r, new EntityResolver());
        assertNotNull(sr);
        assertEquals(1, sr.size());

        assertTrue(sr.firstList().contains(new Integer(67)));
    }
}
