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
package org.apache.cayenne.crypto.transformer.value;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Types;

import junit.framework.TestCase;

import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;

public class JceTransformerFactoryTest extends TestCase {

    private DbEntity t1;
    private DbEntity t2;

    @Override
    protected void setUp() throws Exception {
        ServerRuntime runtime = new ServerRuntime("cayenne-crypto.xml");
        this.t1 = runtime.getChannel().getEntityResolver().getDbEntity("TABLE1");
        this.t2 = runtime.getChannel().getEntityResolver().getDbEntity("TABLE2");
    }

    public void testGetJavaType() {

        JceTransformerFactory f = new JceTransformerFactory();

        DbAttribute t1_ct = t1.getAttribute("CRYPTO_STRING");
        assertEquals("java.lang.String", f.getJavaType(t1_ct));

        DbAttribute t2_cb = t2.getAttribute("CRYPTO_BYTES");
        assertEquals("byte[]", f.getJavaType(t2_cb));

        DbEntity fakeEntity = mock(DbEntity.class);
        when(fakeEntity.getDataMap()).thenReturn(t1.getDataMap());

        DbAttribute fakeA1 = mock(DbAttribute.class);
        when(fakeA1.getName()).thenReturn("fake1");
        when(fakeA1.getEntity()).thenReturn(fakeEntity);
        when(fakeA1.getType()).thenReturn(Types.VARBINARY);

        assertEquals("byte[]", f.getJavaType(fakeA1));

        DbAttribute fakeA2 = mock(DbAttribute.class);
        when(fakeA2.getName()).thenReturn("fake2");
        when(fakeA2.getEntity()).thenReturn(fakeEntity);
        when(fakeA2.getType()).thenReturn(Types.VARCHAR);

        assertEquals("java.lang.String", f.getJavaType(fakeA2));
    }

    public void testCreateEncryptor() {
        JceTransformerFactory f = new JceTransformerFactory();

        DbAttribute t1_ct = t1.getAttribute("CRYPTO_STRING");

        ValueTransformer t1 = f.createEncryptor(t1_ct);
        assertNotNull(t1);
        assertTrue(t1 instanceof JceValueEncryptor);
        assertNotSame(BytesToBytesConverter.INSTANCE, ((JceValueEncryptor) t1).toBytes);

        DbAttribute t2_cb = t2.getAttribute("CRYPTO_BYTES");

        ValueTransformer t2 = f.createEncryptor(t2_cb);
        assertNotNull(t2);
        assertTrue(t2 instanceof JceValueEncryptor);
        assertSame(BytesToBytesConverter.INSTANCE, ((JceValueEncryptor) t2).toBytes);
    }

}
