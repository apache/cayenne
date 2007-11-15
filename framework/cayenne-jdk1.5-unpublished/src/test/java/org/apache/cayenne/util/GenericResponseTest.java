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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.remote.hessian.service.HessianUtil;
import org.apache.cayenne.util.GenericResponse;
import org.apache.cayenne.util.Util;

import junit.framework.TestCase;

public class GenericResponseTest extends TestCase {

    public void testCreation() throws Exception {
        List list = new ArrayList();
        list.add(new HashMap());

        GenericResponse r = new GenericResponse();
        r.addBatchUpdateCount(new int[] {
                1, 2, 3
        });
        r.addResultList(list);

        assertEquals(2, r.size());

        assertTrue(r.next());
        assertFalse(r.isList());

        int[] srInt = r.currentUpdateCount();
        assertEquals(3, srInt.length);
        assertEquals(2, srInt[1]);

        assertTrue(r.next());
        assertTrue(r.isList());

        assertEquals(list, r.currentList());

        assertFalse(r.next());
    }

    public void testSerialization() throws Exception {
        List list = new ArrayList();
        list.add(new HashMap());

        GenericResponse r = new GenericResponse();
        r.addBatchUpdateCount(new int[] {
                1, 2, 3
        });
        r.addResultList(list);

        GenericResponse sr = (GenericResponse) Util.cloneViaSerialization(r);
        assertNotNull(sr);
        assertEquals(2, sr.size());

        assertTrue(sr.next());
        assertFalse(sr.isList());

        int[] srInt = sr.currentUpdateCount();
        assertEquals(3, srInt.length);
        assertEquals(2, srInt[1]);

        assertTrue(sr.next());
        assertTrue(sr.isList());

        assertEquals(list, sr.currentList());

        assertFalse(sr.next());
    }
    
    public void testSerializationWithHessian() throws Exception {
        List list = new ArrayList();
        list.add(new HashMap());

        GenericResponse r = new GenericResponse();
        r.addBatchUpdateCount(new int[] {
                1, 2, 3
        });
        r.addResultList(list);

        GenericResponse sr = (GenericResponse) HessianUtil.cloneViaClientServerSerialization(r, new EntityResolver());
        assertNotNull(sr);
        assertEquals(2, sr.size());

        assertTrue(sr.next());
        assertFalse(sr.isList());

        int[] srInt = sr.currentUpdateCount();
        assertEquals(3, srInt.length);
        assertEquals(2, srInt[1]);

        assertTrue(sr.next());
        assertTrue(sr.isList());

        assertEquals(list, sr.currentList());

        assertFalse(sr.next());
    }
}
