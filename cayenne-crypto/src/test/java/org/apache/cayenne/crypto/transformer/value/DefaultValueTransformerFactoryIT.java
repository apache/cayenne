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
package org.apache.cayenne.crypto.transformer.value;

import org.apache.cayenne.crypto.key.KeySource;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Types;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultValueTransformerFactoryIT {

    private static DbEntity t1;
    private static DbEntity t2;
    private static DbEntity t3;
    private static DbEntity t5;

    private static Map<String, BytesConverter<?>> dbToBytes, objectToBytes;

    private DefaultValueTransformerFactory f;

    @BeforeClass
    public static void beforeClass() throws Exception {
        CayenneRuntime runtime = CayenneRuntime.builder().addConfig("cayenne-crypto.xml").build();
        t1 = runtime.getChannel().getEntityResolver().getDbEntity("TABLE1");
        t2 = runtime.getChannel().getEntityResolver().getDbEntity("TABLE2");
        t3 = runtime.getChannel().getEntityResolver().getDbEntity("TABLE3");
        t5 = runtime.getChannel().getEntityResolver().getDbEntity("TABLE5");

        dbToBytes = getDefaultDbConverters();
        objectToBytes = getDefaultObjectConverters();
    }

    @Before
    public void before() {
        f = new DefaultValueTransformerFactory(mock(KeySource.class), dbToBytes, objectToBytes);
    }

    @Test
    public void testGetJavaType() {

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
    public void testGetAmbiguousJavaType() {
        // this one have two bound ObjAttributes, warn should be in log
        DbAttribute a1 = t5.getAttribute("CRYPTO_INT1");
        assertEquals("java.lang.String", f.getJavaType(a1));

        // this one doesn't have any bindings, warn should be in log
        DbAttribute a2 = t5.getAttribute("CRYPTO_INT2");
        assertEquals("byte[]", f.getJavaType(a2));

        // this one have one binding
        DbAttribute a3 = t5.getAttribute("CRYPTO_INT3");
        assertEquals("int", f.getJavaType(a3));

        // this one have two bindings but with the same int type
        DbAttribute a4 = t5.getAttribute("CRYPTO_INT4");
        assertEquals("int", f.getJavaType(a4));
    }

    @Test
    public void testCreateEncryptor() {

        DbAttribute t1_ct = t1.getAttribute("CRYPTO_STRING");

        ValueEncryptor t1 = f.createEncryptor(t1_ct);
        assertNotNull(t1);
        assertTrue(t1 instanceof DefaultValueEncryptor);
        assertSame(Utf8StringConverter.INSTANCE, ((DefaultValueEncryptor) t1).getPreConverter());
        assertSame(Base64StringConverter.INSTANCE, ((DefaultValueEncryptor) t1).getPostConverter());

        DbAttribute t2_cb = t2.getAttribute("CRYPTO_BYTES");

        ValueEncryptor t2 = f.createEncryptor(t2_cb);
        assertNotNull(t2);
        assertTrue(t2 instanceof DefaultValueEncryptor);
        assertSame(BytesToBytesConverter.INSTANCE, ((DefaultValueEncryptor) t2).getPreConverter());
        assertSame(BytesToBytesConverter.INSTANCE, ((DefaultValueEncryptor) t2).getPostConverter());
    }

    @Test
    public void testCreateDecryptor() {

        DbAttribute t1_ct = t1.getAttribute("CRYPTO_STRING");

        ValueDecryptor t1 = f.createDecryptor(t1_ct);
        assertNotNull(t1);
        assertTrue(t1 instanceof DefaultValueDecryptor);
        assertSame(Base64StringConverter.INSTANCE, ((DefaultValueDecryptor) t1).getPreConverter());
        assertSame(Utf8StringConverter.INSTANCE, ((DefaultValueDecryptor) t1).getPostConverter());

        DbAttribute t2_cb = t2.getAttribute("CRYPTO_BYTES");

        ValueDecryptor t2 = f.createDecryptor(t2_cb);
        assertNotNull(t2);
        assertTrue(t2 instanceof DefaultValueDecryptor);
        assertSame(BytesToBytesConverter.INSTANCE, ((DefaultValueDecryptor) t2).getPreConverter());
        assertSame(BytesToBytesConverter.INSTANCE, ((DefaultValueDecryptor) t2).getPostConverter());

        DbAttribute t3_cb = t3.getAttribute("CRYPTO_BYTES");

        ValueDecryptor t3 = f.createDecryptor(t3_cb);
        assertNotNull(t3);
        assertTrue(t3 instanceof DefaultValueDecryptor);
        assertSame(BytesToBytesConverter.INSTANCE, ((DefaultValueDecryptor) t3).getPreConverter());
        assertSame(Utf8StringConverter.INSTANCE, ((DefaultValueDecryptor) t3).getPostConverter());
    }

    @Test
    public void testEncryptor() {

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

    private static Map<String, BytesConverter<?>> getDefaultDbConverters() {
        Map<String, BytesConverter<?>> dbToBytes = new HashMap<>();

        dbToBytes.put(String.valueOf(Types.BINARY), BytesToBytesConverter.INSTANCE);
        dbToBytes.put(String.valueOf(Types.BLOB), BytesToBytesConverter.INSTANCE);
        dbToBytes.put(String.valueOf(Types.VARBINARY), BytesToBytesConverter.INSTANCE);
        dbToBytes.put(String.valueOf(Types.LONGVARBINARY), BytesToBytesConverter.INSTANCE);

        dbToBytes.put(String.valueOf(Types.CHAR), Base64StringConverter.INSTANCE);
        dbToBytes.put(String.valueOf(Types.NCHAR), Base64StringConverter.INSTANCE);
        dbToBytes.put(String.valueOf(Types.CLOB), Base64StringConverter.INSTANCE);
        dbToBytes.put(String.valueOf(Types.NCLOB), Base64StringConverter.INSTANCE);
        dbToBytes.put(String.valueOf(Types.LONGVARCHAR), Base64StringConverter.INSTANCE);
        dbToBytes.put(String.valueOf(Types.LONGNVARCHAR), Base64StringConverter.INSTANCE);
        dbToBytes.put(String.valueOf(Types.VARCHAR), Base64StringConverter.INSTANCE);
        dbToBytes.put(String.valueOf(Types.NVARCHAR), Base64StringConverter.INSTANCE);

        return dbToBytes;
    }

    private static Map<String, BytesConverter<?>> getDefaultObjectConverters() {
        Map<String, BytesConverter<?>> objectToBytes = new HashMap<>();

        objectToBytes.put("byte[]", BytesToBytesConverter.INSTANCE);
        objectToBytes.put(String.class.getName(), Utf8StringConverter.INSTANCE);

        objectToBytes.put(Double.class.getName(), DoubleConverter.INSTANCE);
        objectToBytes.put(Double.TYPE.getName(), DoubleConverter.INSTANCE);

        objectToBytes.put(Float.class.getName(), FloatConverter.INSTANCE);
        objectToBytes.put(Float.TYPE.getName(), FloatConverter.INSTANCE);

        objectToBytes.put(Long.class.getName(), LongConverter.INSTANCE);
        objectToBytes.put(Long.TYPE.getName(), LongConverter.INSTANCE);

        objectToBytes.put(Integer.class.getName(), IntegerConverter.INSTANCE);
        objectToBytes.put(Integer.TYPE.getName(), IntegerConverter.INSTANCE);

        objectToBytes.put(Short.class.getName(), ShortConverter.INSTANCE);
        objectToBytes.put(Short.TYPE.getName(), ShortConverter.INSTANCE);

        objectToBytes.put(Byte.class.getName(), ByteConverter.INSTANCE);
        objectToBytes.put(Byte.TYPE.getName(), ByteConverter.INSTANCE);

        objectToBytes.put(Boolean.class.getName(), BooleanConverter.INSTANCE);
        objectToBytes.put(Boolean.TYPE.getName(), BooleanConverter.INSTANCE);

        objectToBytes.put(Date.class.getName(), UtilDateConverter.INSTANCE);
        objectToBytes.put(BigInteger.class.getName(), BigIntegerConverter.INSTANCE);
        objectToBytes.put(BigDecimal.class.getName(), BigDecimalConverter.INSTANCE);

        return objectToBytes;
    }
}
