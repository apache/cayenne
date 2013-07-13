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

import static org.mockito.Mockito.mock;
import junit.framework.TestCase;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.testdo.mt.ClientMtTable1;
import org.apache.cayenne.testdo.mt.ClientMtTable2;

public class PersistentObjectHolderTest extends TestCase {

    public void testSetInitialValue() {

        ObjectContext context = mock(ObjectContext.class);

        ClientMtTable2 o = new ClientMtTable2();
        o.setPersistenceState(PersistenceState.COMMITTED);
        o.setObjectContext(context);
        PersistentObjectHolder holder = new PersistentObjectHolder(o, ClientMtTable2.TABLE1_PROPERTY);

        assertTrue(holder.isFault());
        ClientMtTable1 o1 = new ClientMtTable1();
        o1.setObjectContext(context);
        holder.setValueDirectly(o1);

        assertFalse(holder.isFault());
        assertSame(o1, holder.value);
    }

    public void testInvalidate() {
        ObjectContext context = mock(ObjectContext.class);

        ClientMtTable2 o = new ClientMtTable2();
        o.setPersistenceState(PersistenceState.COMMITTED);
        o.setObjectContext(context);
        PersistentObjectHolder holder = new PersistentObjectHolder(o, ClientMtTable2.TABLE1_PROPERTY);

        assertTrue(holder.isFault());
        ClientMtTable1 o1 = new ClientMtTable1();
        o1.setObjectContext(context);
        holder.setValueDirectly(o1);

        holder.invalidate();
        assertTrue(holder.isFault());
        assertNull(holder.value);
    }
}
