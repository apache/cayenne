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

package org.apache.cayenne;

import junit.framework.TestCase;

import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.remote.hessian.service.HessianUtil;

public class DataRowTest extends TestCase {

    public void testHessianSerializability() throws Exception {
        DataRow s1 = new DataRow(10);
        s1.put("a", "b");

        DataRow s2 = (DataRow) HessianUtil.cloneViaServerClientSerialization(
                s1,
                new EntityResolver());

        assertNotSame(s1, s2);
        assertEquals(s1, s2);
        assertEquals(s1.getVersion(), s2.getVersion());
        assertEquals(s1.getReplacesVersion(), s2.getReplacesVersion());

        // at the moment there are no serializers that can go from client to server.
        // DataRow s3 = (DataRow) HessianUtil.cloneViaClientServerSerialization(
        // s1,
        // new EntityResolver());
        //
        // assertNotSame(s1, s3);
        // assertEquals(s1, s3);
    }

    public void testVersion() throws Exception {
        DataRow s1 = new DataRow(10);
        DataRow s2 = new DataRow(10);
        DataRow s3 = new DataRow(10);
        assertFalse(s1.getVersion() == s2.getVersion());
        assertFalse(s2.getVersion() == s3.getVersion());
        assertFalse(s3.getVersion() == s1.getVersion());
    }

}
