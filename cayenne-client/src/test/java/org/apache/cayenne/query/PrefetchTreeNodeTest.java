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
package org.apache.cayenne.query;

import junit.framework.TestCase;

import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.remote.hessian.service.HessianUtil;

public class PrefetchTreeNodeTest extends TestCase {

    public void testTreeSerializationWithHessian() throws Exception {
        PrefetchTreeNode n1 = new PrefetchTreeNode();
        PrefetchTreeNode n2 = n1.addPath("abc");

        PrefetchTreeNode nc1 = (PrefetchTreeNode) HessianUtil.cloneViaClientServerSerialization(n1,
                new EntityResolver());
        assertNotNull(nc1);

        PrefetchTreeNode nc2 = nc1.getNode("abc");
        assertNotNull(nc2);
        assertNotSame(nc2, n2);
        assertSame(nc1, nc2.getParent());
        assertEquals("abc", nc2.getName());
    }

    public void testSubtreeSerializationWithHessian() throws Exception {
        PrefetchTreeNode n1 = new PrefetchTreeNode();
        PrefetchTreeNode n2 = n1.addPath("abc");
        PrefetchTreeNode n3 = n2.addPath("xyz");

        // test that substree was serialized as independent tree, instead of
        // sucking
        PrefetchTreeNode nc2 = (PrefetchTreeNode) HessianUtil.cloneViaClientServerSerialization(n2,
                new EntityResolver());
        assertNotNull(nc2);
        assertNull(nc2.getParent());

        PrefetchTreeNode nc3 = nc2.getNode("xyz");
        assertNotNull(nc3);
        assertNotSame(nc3, n3);
        assertSame(nc2, nc3.getParent());
        assertEquals("xyz", nc3.getName());
    }
}
