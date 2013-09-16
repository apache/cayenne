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

package org.apache.cayenne.reflect;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.types.MockEnum;
import org.apache.cayenne.access.types.MockEnumHolder;

public class PropertyUtilsTest extends TestCase {

    public void testCreateAccessor() {

        Accessor accessor = PropertyUtils.createAccessor(
                TestJavaBean.class,
                "byteArrayField");
        assertNotNull(accessor);

        TestJavaBean o1 = createBean();
        assertSame(o1.getByteArrayField(), accessor.getValue(o1));

        TestJavaBean o2 = new TestJavaBean();
        assertNull(o2.getByteArrayField());
        accessor.setValue(o2, o1.getByteArrayField());
        assertNotNull(o2.getByteArrayField());
        assertSame(o1.getByteArrayField(), o2.getByteArrayField());
    }

    public void testCreateAccessorNested() {

        Accessor accessor = PropertyUtils.createAccessor(
                TestJavaBean.class,
                "related.byteArrayField");
        assertNotNull(accessor);

        TestJavaBean o1 = createBean();
        o1.setRelated(new TestJavaBean());
        o1.getRelated().setByteArrayField(new byte[] {
                3, 4, 5
        });
        assertSame(o1.getRelated().getByteArrayField(), accessor.getValue(o1));

        TestJavaBean o2 = new TestJavaBean();
        o2.setRelated(new TestJavaBean());

        byte[] b1 = new byte[] {
                6, 7, 8
        };
        accessor.setValue(o2, b1);
        assertSame(b1, o2.getRelated().getByteArrayField());
    }

    public void testGetProperty() {
        TestJavaBean o1 = createBean();

        assertSame(o1.getByteArrayField(), PropertyUtils
                .getProperty(o1, "byteArrayField"));
        assertSame(o1.getIntegerField(), PropertyUtils.getProperty(o1, "integerField"));
        assertEquals(new Integer(o1.getIntField()), PropertyUtils.getProperty(
                o1,
                "intField"));
        assertSame(o1.getNumberField(), PropertyUtils.getProperty(o1, "numberField"));
        assertSame(o1.getObjectField(), PropertyUtils.getProperty(o1, "objectField"));
        assertSame(o1.getStringField(), PropertyUtils.getProperty(o1, "stringField"));
        assertEquals(Boolean.valueOf(o1.isBooleanField()), PropertyUtils.getProperty(
                o1,
                "booleanField"));
    }

    public void testSetProperty() {
        TestJavaBean o1 = createBean();
        TestJavaBean o2 = new TestJavaBean();

        PropertyUtils.setProperty(o2, "byteArrayField", o1.getByteArrayField());
        PropertyUtils.setProperty(o2, "integerField", o1.getIntegerField());
        PropertyUtils.setProperty(o2, "intField", new Integer(o1.getIntField()));
        PropertyUtils.setProperty(o2, "numberField", o1.getNumberField());
        PropertyUtils.setProperty(o2, "objectField", o1.getObjectField());
        PropertyUtils.setProperty(o2, "stringField", o1.getStringField());
        PropertyUtils.setProperty(o2, "booleanField", Boolean.valueOf(o1.isBooleanField()));
    }

    public void testGetPropertyMap() {
        Map o1 = createMap();

        assertSame(o1.get("byteArrayField"), PropertyUtils.getProperty(
                o1,
                "byteArrayField"));
        assertSame(o1.get("integerField"), PropertyUtils.getProperty(o1, "integerField"));
        assertEquals(o1.get("intField"), PropertyUtils.getProperty(o1, "intField"));
        assertSame(o1.get("numberField"), PropertyUtils.getProperty(o1, "numberField"));
        assertSame(o1.get("objectField"), PropertyUtils.getProperty(o1, "objectField"));
        assertSame(o1.get("stringField"), PropertyUtils.getProperty(o1, "stringField"));
        assertEquals(o1.get("booleanField"), PropertyUtils
                .getProperty(o1, "booleanField"));
    }

    public void testSetPropertyMap() {
        Map o1 = createMap();
        Map o2 = new HashMap();

        PropertyUtils.setProperty(o2, "byteArrayField", o1.get("byteArrayField"));
        PropertyUtils.setProperty(o2, "integerField", o1.get("integerField"));
        PropertyUtils.setProperty(o2, "intField", o1.get("intField"));
        PropertyUtils.setProperty(o2, "numberField", o1.get("numberField"));
        PropertyUtils.setProperty(o2, "objectField", o1.get("objectField"));
        PropertyUtils.setProperty(o2, "stringField", o1.get("stringField"));
        PropertyUtils.setProperty(o2, "booleanField", o1.get("booleanField"));

        assertEquals(o1, o2);
    }

    public void testSetConverted() {
        TestJavaBean o1 = new TestJavaBean();

        // Object -> String
        Object object = new Object();
        PropertyUtils.setProperty(o1, "stringField", object);
        assertEquals(object.toString(), o1.getStringField());

        // String to number
        PropertyUtils.setProperty(o1, "integerField", "25");
        assertEquals(new Integer(25), o1.getIntegerField());

        // string to byte primitive
        PropertyUtils.setProperty(o1, "byteField", "2");
        assertEquals(2, o1.getByteField());
        
        // string to short primitive
        PropertyUtils.setProperty(o1, "shortField", "3");
        assertEquals(3, o1.getShortField());
        
        // string to int primitive
        PropertyUtils.setProperty(o1, "intField", "28");
        assertEquals(28, o1.getIntField());
        
        // string to long primitive
        PropertyUtils.setProperty(o1, "longField", "29");
        assertEquals(29, o1.getLongField());
        
        // string to float primitive
        PropertyUtils.setProperty(o1, "floatField", "4.5");
        assertEquals(4.5f, o1.getFloatField());
        
        // string to double primitive
        PropertyUtils.setProperty(o1, "doubleField", "5.5");
        assertEquals(5.5, o1.getDoubleField());
        
        // string to boolean
        PropertyUtils.setProperty(o1, "booleanField", "true");
        assertTrue(o1.isBooleanField());
        PropertyUtils.setProperty(o1, "booleanField", "false");
        assertFalse(o1.isBooleanField());
        
        // int to boolean
        PropertyUtils.setProperty(o1, "booleanField", 1);
        assertTrue(o1.isBooleanField());
        PropertyUtils.setProperty(o1, "booleanField", 0);
        assertFalse(o1.isBooleanField());
        
        // long to boolean
        PropertyUtils.setProperty(o1, "booleanField", 1L);
        assertTrue(o1.isBooleanField());
        PropertyUtils.setProperty(o1, "booleanField", 0L);
        assertFalse(o1.isBooleanField());
        
        // long to date
        PropertyUtils.setProperty(o1, "dateField", 0L);
        assertEquals(new Date(0L), o1.getDateField());
        
        // long to timestamp
        PropertyUtils.setProperty(o1, "timestampField", 0L);
        assertEquals(new Timestamp(0L), o1.getTimestampField());
        
        // arbitrary string/object to field
        PropertyUtils.setProperty(o1, "stringBuilderField", "abc");
        assertEquals(new StringBuilder("abc").toString(), o1.getStringBuilderField().toString());
    }

    public void testSetConvertedWithCustomConverter() {
    	ConverterFactory.addConverter(Date.class, new Converter<Date>() {
			@Override
			protected Date convert(Object value, Class<Date> type) {
				if (value == null) return null;
				if (value instanceof Date) {
					return (Date)value;
				}
				if (value instanceof String) {
					SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
					try {
						return format.parse((String) value);
					} catch (ParseException e) {
						throw new CayenneRuntimeException("Unable to convert '" + value + "' to a Date", e);
					}
				}
				throw new CayenneRuntimeException("Unable to convert '" + value + "' to a Date");
			}
    	});
    	
        TestJavaBean o1 = new TestJavaBean();

        // String to date
        PropertyUtils.setProperty(o1, "dateField", "2013-08-01");
        
        Calendar cal = new GregorianCalendar(2013, 7, 1, 0, 0, 0);
        assertEquals(cal.getTime(), o1.getDateField());
    }
    
    public void testSetNull() {
        TestJavaBean o1 = new TestJavaBean();

        o1.setStringField("xyz");
        PropertyUtils.setProperty(o1, "stringField", null);
        assertNull(o1.getStringField());

        o1.setBooleanField(true);
        PropertyUtils.setProperty(o1, "booleanField", null);
        assertEquals(false, o1.isBooleanField());
        
        o1.setByteField((byte) 2);
        PropertyUtils.setProperty(o1, "byteField", null);
        assertEquals((byte)0, o1.getByteField());
        
        o1.setShortField((short) 3);
        PropertyUtils.setProperty(o1, "shortField", null);
        assertEquals((short)0, o1.getShortField());
        
        o1.setIntField(99);
        PropertyUtils.setProperty(o1, "intField", null);
        assertEquals(0, o1.getIntField());
        
        o1.setLongField(98);
        PropertyUtils.setProperty(o1, "longField", null);
        assertEquals(0L, o1.getLongField());
        
        o1.setFloatField(4.5f);
        PropertyUtils.setProperty(o1, "floatField", null);
        assertEquals(0.0f, o1.getFloatField());
        
        o1.setDoubleField(5.5f);
        PropertyUtils.setProperty(o1, "doubleField", null);
        assertEquals(0.0, o1.getDoubleField());
    }

    public void testSetConvertedEnum() {
        MockEnumHolder o1 = new MockEnumHolder();

        // String to Enum
        PropertyUtils.setProperty(o1, "mockEnum", "b");
        assertSame(MockEnum.b, o1.getMockEnum());

        // check that regular converters still work
        PropertyUtils.setProperty(o1, "number", "445");
        assertEquals(445, o1.getNumber());
    }

    protected TestJavaBean createBean() {
        TestJavaBean o1 = new TestJavaBean();
        o1.setByteArrayField(new byte[] {
                1, 2, 3
        });
        o1.setIntegerField(new Integer(33));
        o1.setIntField(-44);
        o1.setNumberField(new BigDecimal("11111"));
        o1.setObjectField(new Object());
        o1.setStringField("aaaaa");
        o1.setBooleanField(true);

        return o1;
    }

    protected Map createMap() {
        Map o1 = new HashMap();
        o1.put("byteArrayField", new byte[] {
                1, 2, 3
        });
        o1.put("integerField", new Integer(33));
        o1.put("intField", new Integer(-44));
        o1.put("numberField", new BigDecimal("11111"));
        o1.put("objectField", new Object());
        o1.put("stringField", "aaaaa");
        o1.put("booleanField", Boolean.TRUE);

        return o1;
    }
}
