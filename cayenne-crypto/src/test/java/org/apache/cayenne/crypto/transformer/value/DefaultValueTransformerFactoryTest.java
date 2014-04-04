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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Types;

import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.crypto.key.KeySource;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.junit.Before;
import org.junit.Test;

public class DefaultValueTransformerFactoryTest {

    private DbEntity t1;
    private DbEntity t2;
    private DbEntity t3;

    @Before
    public void setUp() throws Exception {
        ServerRuntime runtime = new ServerRuntime("cayenne-crypto.xml");
        this.t1 = runtime.getChannel().getEntityResolver().getDbEntity("TABLE1");
        this.t2 = runtime.getChannel().getEntityResolver().getDbEntity("TABLE2");
        this.t3 = runtime.getChannel().getEntityResolver().getDbEntity("TABLE3");
    }

    @Test
    public void testGetJavaType() {

        DefaultValueTransformerFactory f = new DefaultValueTransformerFactory(mock(KeySource.class));

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

    @Test
    public void testCreateEncryptor() {
        DefaultValueTransformerFactory f = new DefaultValueTransformerFactory(mock(KeySource.class));

        DbAttribute t1_ct = t1.getAttribute("CRYPTO_STRING");

        ValueEncryptor t1 = f.createEncryptor(t1_ct);
        assertNotNull(t1);
        assertTrue(t1 instanceof DefaultEncryptor);
        assertSame(Utf8StringConverter.INSTANCE, ((DefaultEncryptor) t1).getPreConverter());
        assertSame(Base64StringConverter.INSTANCE, ((DefaultEncryptor) t1).getPostConverter());

        DbAttribute t2_cb = t2.getAttribute("CRYPTO_BYTES");

        ValueEncryptor t2 = f.createEncryptor(t2_cb);
        assertNotNull(t2);
        assertTrue(t2 instanceof DefaultEncryptor);
        assertSame(BytesToBytesConverter.INSTANCE, ((DefaultEncryptor) t2).getPreConverter());
        assertSame(BytesToBytesConverter.INSTANCE, ((DefaultEncryptor) t2).getPostConverter());
    }

    @Test
    public void testCreateDecryptor() {
        DefaultValueTransformerFactory f = new DefaultValueTransformerFactory(mock(KeySource.class));

        DbAttribute t1_ct = t1.getAttribute("CRYPTO_STRING");

        ValueDecryptor t1 = f.createDecryptor(t1_ct);
        assertNotNull(t1);
        assertTrue(t1 instanceof DefaultDecryptor);
        assertSame(Base64StringConverter.INSTANCE, ((DefaultDecryptor) t1).getPreConverter());
        assertSame(Utf8StringConverter.INSTANCE, ((DefaultDecryptor) t1).getPostConverter());

        DbAttribute t2_cb = t2.getAttribute("CRYPTO_BYTES");

        ValueDecryptor t2 = f.createDecryptor(t2_cb);
        assertNotNull(t2);
        assertTrue(t2 instanceof DefaultDecryptor);
        assertSame(BytesToBytesConverter.INSTANCE, ((DefaultDecryptor) t2).getPreConverter());
        assertSame(BytesToBytesConverter.INSTANCE, ((DefaultDecryptor) t2).getPostConverter());

        DbAttribute t3_cb = t3.getAttribute("CRYPTO_BYTES");

        ValueDecryptor t3 = f.createDecryptor(t3_cb);
        assertNotNull(t3);
        assertTrue(t3 instanceof DefaultDecryptor);
        assertSame(BytesToBytesConverter.INSTANCE, ((DefaultDecryptor) t3).getPreConverter());
        assertSame(Utf8StringConverter.INSTANCE, ((DefaultDecryptor) t3).getPostConverter());
    }

    @Test
    public void testEncryptor() {
        DefaultValueTransformerFactory f = new DefaultValueTransformerFactory(mock(KeySource.class));

        DbAttribute t1_ct = t1.getAttribute("CRYPTO_STRING");

        ValueEncryptor t1 = f.encryptor(t1_ct);
        assertNotNull(t1);
        assertSame(t1, f.encryptor(t1_ct));
        assertSame(t1, f.encryptor(t1_ct));

        DbAttribute t2_cb = t2.getAttribute("CRYPTO_BYTES");

        ValueEncryptor t2 = f.encryptor(t2_cb);
        assertNotNull(t2);
        assertSame(t2, f.encryptor(t2_cb));
        assertSame(t2, f.encryptor(t2_cb));
    }

    @Test
    public void testDecryptor() {
        DefaultValueTransformerFactory f = new DefaultValueTransformerFactory(mock(KeySource.class));

        DbAttribute t1_ct = t1.getAttribute("CRYPTO_STRING");

        ValueDecryptor t1 = f.decryptor(t1_ct);
        assertNotNull(t1);
        assertSame(t1, f.decryptor(t1_ct));
        assertSame(t1, f.decryptor(t1_ct));

        DbAttribute t2_cb = t2.getAttribute("CRYPTO_BYTES");

        ValueDecryptor t2 = f.decryptor(t2_cb);
        assertNotNull(t2);
        assertSame(t2, f.decryptor(t2_cb));
        assertSame(t2, f.decryptor(t2_cb));
    }

}
