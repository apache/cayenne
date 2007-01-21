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
package org.apache.cayenne.jpa.itest.ch2;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.apache.cayenne.itest.jpa.EntityManagerCase;
import org.apache.cayenne.jpa.itest.ch2.entity.Embeddable1;
import org.apache.cayenne.jpa.itest.ch2.entity.EmbeddedImpliedEntity;
import org.apache.cayenne.jpa.itest.ch2.entity.EnumType;
import org.apache.cayenne.jpa.itest.ch2.entity.PropertyDefaultsDatesEntity;
import org.apache.cayenne.jpa.itest.ch2.entity.PropertyDefaultsOtherEntity;
import org.apache.cayenne.jpa.itest.ch2.entity.PropertyDefaultsPrimitiveEntity;
import org.apache.cayenne.jpa.itest.ch2.entity.PropertyDefaultsWrapperEntity;
import org.apache.cayenne.jpa.itest.ch2.entity.SerializableType;

public class _2_1_6_MappingDefaultsNonRelationshipTest extends EntityManagerCase {

    // TODO: andrus 8/10/2006 - fails
    public void _testEmbeddedDefault() throws Exception {
        getDbHelper().deleteAll("EmbeddedImpliedEntity");

        EmbeddedImpliedEntity o1 = new EmbeddedImpliedEntity();
        Embeddable1 o2 = new Embeddable1();
        o2.setProperty1("p1");
        o1.setEmbeddable(o2);

        getEntityManager().persist(o1);
        getEntityManager().getTransaction().commit();

        assertEquals("p1", getDbHelper().getObject("EmbeddedImpliedEntity", "property1"));
    }

    public void testPrimitiveWrapperDefaults() throws Exception {
        getDbHelper().deleteAll("PropertyDefaultsWrapperEntity");

        PropertyDefaultsWrapperEntity o1 = new PropertyDefaultsWrapperEntity();

        o1.setBooleanWrapper(Boolean.TRUE);
        o1.setByteWrapper(new Byte("3"));
        o1.setShortWrapper(new Short("2"));
        o1.setIntWrapper(new Integer("4"));
        o1.setCharWrapper(new Character('a'));
        o1.setLongWrapper(new Long(1234567890l));
        o1.setFloatWrapper(new Float("5.5"));
        o1.setDoubleWrapper(new Double("6.5"));

        getEntityManager().persist(o1);
        getEntityManager().getTransaction().commit();

        assertTrue(getDbHelper().getBoolean(
                "PropertyDefaultsWrapperEntity",
                "booleanWrapper"));
        assertEquals(2, getDbHelper().getInt(
                "PropertyDefaultsWrapperEntity",
                "shortWrapper"));
        assertEquals(3, getDbHelper().getInt(
                "PropertyDefaultsWrapperEntity",
                "byteWrapper"));
        assertEquals(4, getDbHelper().getInt(
                "PropertyDefaultsWrapperEntity",
                "intWrapper"));
        assertEquals("a", getDbHelper().getObject(
                "PropertyDefaultsWrapperEntity",
                "charWrapper"));
        assertEquals(1234567890l, getDbHelper().getLong(
                "PropertyDefaultsWrapperEntity",
                "longWrapper"));
        assertEquals(5.5d, getDbHelper().getDouble(
                "PropertyDefaultsWrapperEntity",
                "floatWrapper"));
        assertEquals(6.5d, getDbHelper().getDouble(
                "PropertyDefaultsWrapperEntity",
                "doubleWrapper"));
    }

    public void testPrimitiveDefaults() throws Exception {
        getDbHelper().deleteAll("PropertyDefaultsPrimitiveEntity");

        PropertyDefaultsPrimitiveEntity o1 = new PropertyDefaultsPrimitiveEntity();
        o1.setPrimitiveBoolean(true);

        o1.setPrimitiveByte((byte) 3);
        o1.setPrimitiveShort((short) 2);
        o1.setPrimitiveInt(4);
        o1.setPrimitiveChar('a');
        o1.setPrimitiveLong(1234567890l);
        o1.setPrimitiveFloat(5.5f);
        o1.setPrimitiveDouble(6.5d);

        getEntityManager().persist(o1);
        getEntityManager().getTransaction().commit();

        assertTrue(getDbHelper().getBoolean(
                "PropertyDefaultsPrimitiveEntity",
                "primitiveBoolean"));
        assertEquals(2, getDbHelper().getInt(
                "PropertyDefaultsPrimitiveEntity",
                "primitiveShort"));
        assertEquals(3, getDbHelper().getInt(
                "PropertyDefaultsPrimitiveEntity",
                "primitiveByte"));
        assertEquals(4, getDbHelper().getInt(
                "PropertyDefaultsPrimitiveEntity",
                "primitiveInt"));
        assertEquals("a", getDbHelper().getObject(
                "PropertyDefaultsPrimitiveEntity",
                "primitiveChar"));
        assertEquals(1234567890l, getDbHelper().getLong(
                "PropertyDefaultsPrimitiveEntity",
                "primitiveLong"));
        assertEquals(5.5d, getDbHelper().getDouble(
                "PropertyDefaultsPrimitiveEntity",
                "primitiveFloat"));
        assertEquals(6.5d, getDbHelper().getDouble(
                "PropertyDefaultsPrimitiveEntity",
                "primitiveDouble"));
    }

    public void testDatesDefaults() throws Exception {
        getDbHelper().deleteAll("PropertyDefaultsDatesEntity");

        java.util.Date utilDate = new java.util.Date(System.currentTimeMillis() - 1000);
        java.util.Calendar calendar = new java.util.GregorianCalendar();

        java.sql.Date sqlDate = new java.sql.Date(new GregorianCalendar(2005, 4, 3)
                .getTimeInMillis());

        Calendar timeCal = new GregorianCalendar();
        timeCal.clear();
        timeCal.set(Calendar.HOUR_OF_DAY, 4);
        timeCal.set(Calendar.MINUTE, 44);
        timeCal.set(Calendar.SECOND, 32);
        Time time = new Time(timeCal.getTimeInMillis());
        Timestamp timestamp = new Timestamp(utilDate.getTime() - 50);

        PropertyDefaultsDatesEntity o1 = new PropertyDefaultsDatesEntity();
        o1.setUtilDate(utilDate);
        o1.setCalendar(calendar);
        o1.setSqlDate(sqlDate);
        o1.setSqlTime(time);
        o1.setSqlTimestamp(timestamp);

        getEntityManager().persist(o1);
        getEntityManager().getTransaction().commit();

        assertEquals(utilDate, getDbHelper().getUtilDate(
                "PropertyDefaultsDatesEntity",
                "utilDate"));
        assertEquals(calendar.getTimeInMillis(), getDbHelper().getTimestamp(
                "PropertyDefaultsDatesEntity",
                "calendar").getTime());
        assertEquals(sqlDate, getDbHelper().getSqlDate(
                "PropertyDefaultsDatesEntity",
                "sqlDate"));
        assertEquals(time, getDbHelper()
                .getTime("PropertyDefaultsDatesEntity", "sqlTime"));
        assertEquals(timestamp, getDbHelper().getTimestamp(
                "PropertyDefaultsDatesEntity",
                "sqlTimestamp"));
    }

    public void testOtherDefaults() throws Exception {
        getDbHelper().deleteAll("PropertyDefaultsOtherEntity");

        PropertyDefaultsOtherEntity o1 = new PropertyDefaultsOtherEntity();
        o1.setString("abc");
        o1.setBigInt(new BigInteger("2"));
        o1.setBigDecimal(new BigDecimal("3.5"));
        o1.setByteArray(new byte[] {
                1, 2
        });
        o1.setByteWrapperArray(new Byte[] {
                new Byte("1"), new Byte("2")
        });

        o1.setCharArray(new char[] {
                'a', 'b'
        });
        o1.setCharWrapperArray(new Character[] {
                new Character('a'), new Character('b')
        });

        o1.setEnumType(EnumType.b);
        o1.setSerializableType(new SerializableType("c"));

        getEntityManager().persist(o1);
        getEntityManager().getTransaction().commit();

        assertEquals("abc", getDbHelper().getObject(
                "PropertyDefaultsOtherEntity",
                "string"));
        assertEquals(2, getDbHelper().getInt("PropertyDefaultsOtherEntity", "bigInt"));
        assertEquals(3.5d, getDbHelper().getDouble(
                "PropertyDefaultsOtherEntity",
                "bigDecimal"));

        byte[] b1 = getDbHelper().getBytes("PropertyDefaultsOtherEntity", "byteArray");
        assertNotNull(b1);
        assertEquals(2, b1.length);
        assertEquals((byte) 1, b1[0]);
        assertEquals((byte) 2, b1[1]);

        byte[] b2 = getDbHelper().getBytes(
                "PropertyDefaultsOtherEntity",
                "byteWrapperArray");
        assertNotNull(b2);
        assertEquals(2, b2.length);
        assertEquals((byte) 1, b2[0]);
        assertEquals((byte) 2, b2[1]);

        assertEquals("ab", getDbHelper().getObject(
                "PropertyDefaultsOtherEntity",
                "charArray"));

        assertEquals("ab", getDbHelper().getObject(
                "PropertyDefaultsOtherEntity",
                "charWrapperArray"));
        assertEquals(1, getDbHelper().getInt("PropertyDefaultsOtherEntity", "enumType"));

        byte[] ser = getDbHelper().getBytes(
                "PropertyDefaultsOtherEntity",
                "serializableType");
        assertNotNull(ser);
        SerializableType so = (SerializableType) new ObjectInputStream(
                new ByteArrayInputStream(ser)).readObject();
        assertNotNull(so);
        assertEquals("c", so.getKey());
    }
}
