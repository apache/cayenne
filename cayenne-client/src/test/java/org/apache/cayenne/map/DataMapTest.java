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
package org.apache.cayenne.map;

import junit.framework.TestCase;

import org.apache.cayenne.remote.hessian.service.HessianUtil;
import org.apache.cayenne.util.Util;

public class DataMapTest extends TestCase {

    public void testSerializabilityWithHessian() throws Exception {
        DataMap m1 = new DataMap("abc");
        DataMap d1 = (DataMap) HessianUtil.cloneViaClientServerSerialization(m1, new EntityResolver());
        assertEquals(m1.getName(), d1.getName());

        ObjEntity oe1 = new ObjEntity("oe1");
        m1.addObjEntity(oe1);

        DataMap d2 = (DataMap) Util.cloneViaSerialization(m1);
        assertNotNull(d2.getObjEntity(oe1.getName()));
    }
}
