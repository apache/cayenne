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

import org.apache.cayenne.CayenneContext;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.testdo.mt.ClientMtTable1;
import org.apache.cayenne.testdo.mt.ClientMtTable2;
import org.apache.cayenne.unit.AccessStack;
import org.apache.cayenne.unit.CayenneCase;
import org.apache.cayenne.unit.CayenneResources;

public class GeneratedMethodsInClientObjectContextTest extends CayenneCase {

    @Override
    protected AccessStack buildAccessStack() {
        return CayenneResources
                .getResources()
                .getAccessStack(MULTI_TIER_ACCESS_STACK);
    }

    public void testAddToList() throws Exception {

        EntityResolver resolver = getDomain()
                .getEntityResolver()
                .getClientEntityResolver();

        CayenneContext context = new CayenneContext();
        context.setEntityResolver(resolver);

        ClientMtTable1 t1 = context.newObject(ClientMtTable1.class);
        ClientMtTable2 t2 = context.newObject(ClientMtTable2.class);

        t1.addToTable2Array(t2);
        assertEquals(1, t1.getTable2Array().size());
        assertSame(t1, t2.getTable1());
        
        // do it again to make sure action can handle series of changes
        ClientMtTable1 t3 = context.newObject(ClientMtTable1.class);
        ClientMtTable2 t4 = context.newObject(ClientMtTable2.class);

        t3.addToTable2Array(t4);
        assertEquals(1, t3.getTable2Array().size());
        assertSame(t3, t4.getTable1());
    }

    public void testSetValueHolder() throws Exception {

        EntityResolver resolver = getDomain()
                .getEntityResolver()
                .getClientEntityResolver();

        CayenneContext context = new CayenneContext();
        context.setEntityResolver(resolver);

        ClientMtTable1 t1 = context.newObject(ClientMtTable1.class);
        ClientMtTable2 t2 = context.newObject(ClientMtTable2.class);

        t2.setTable1(t1);
        assertEquals(1, t1.getTable2Array().size());
        assertSame(t1, t2.getTable1());
    }
}
