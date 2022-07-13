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

package org.apache.cayenne.reflect;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.types.MockEnum;
import org.apache.cayenne.access.types.MockEnumHolder;
import org.junit.Test;

public class PropertyUtilsTest {

	@Test
	public void testAccessor() {

		Accessor accessor = PropertyUtils.accessor("byteArrayField");
		assertNotNull(accessor);

		TstJavaBean o1 = createBean();
		assertSame(o1.getByteArrayField(), accessor.getValue(o1));

		TstJavaBean o2 = new TstJavaBean();
		assertNull(o2.getByteArrayField());
		accessor.setValue(o2, o1.getByteArrayField());
		assertNotNull(o2.getByteArrayField());
		assertSame(o1.getByteArrayField(), o2.getByteArrayField());
	}

	@Test
	public void testAccessor_Cache() {

		Accessor accessor = PropertyUtils.accessor("p1");
		assertNotNull(accessor);
		assertSame(accessor, PropertyUtils.accessor("p1"));
		assertSame(accessor, PropertyUtils.accessor("p1"));
		assertNotSame(accessor, PropertyUtils.accessor("p2"));
	}
	
	@Test
	public void testAccessor_CacheNested() {

		Accessor accessor = PropertyUtils.accessor("p1.p2");
		assertNotNull(accessor);
		assertSame(accessor, PropertyUtils.accessor("p1.p2"));
		assertNotSame(accessor, PropertyUtils.accessor("p1"));
		assertNotSame(accessor, PropertyUtils.accessor("p2"));
	}

	@Test
	public void testAccessorNested() {

		Accessor accessor = PropertyUtils.accessor("related.byteArrayField");
		assertNotNull(accessor);

		TstJavaBean o1 = createBean();
		o1.setRelated(new TstJavaBean());
		o1.getRelated().setByteArrayField(new byte[] { 3, 4, 5 });
		assertSame(o1.getRelated().getByteArrayField(), accessor.getValue(o1));

		TstJavaBean o2 = new TstJavaBean();
		o2.setRelated(new TstJavaBean());

		byte[] b1 = new byte[] { 6, 7, 8 };
		accessor.setValue(o2, b1);
		assertSame(b1, o2.getRelated().getByteArrayField());
	}

	@Test
	public void testAccessorNested_Null() {

		Accessor accessor = PropertyUtils.accessor("related.byteArrayField");
		assertNotNull(accessor);

		TstJavaBean o1 = createBean();
		assertNull(accessor.getValue(o1));
	}

	@Test
	public void testGetProperty() {
		TstJavaBean o1 = createBean();

		assertSame(o1.getByteArrayField(), PropertyUtils.getProperty(o1, "byteArrayField"));
		assertSame(o1.getIntegerField(), PropertyUtils.getProperty(o1, "integerField"));
		assertEquals(o1.getIntField(), PropertyUtils.getProperty(o1, "intField"));
		assertSame(o1.getNumberField(), PropertyUtils.getProperty(o1, "numberField"));
		assertSame(o1.getObjectField(), PropertyUtils.getProperty(o1, "objectField"));
		assertSame(o1.getStringField(), PropertyUtils.getProperty(o1, "stringField"));
		assertEquals(o1.isBooleanField(), PropertyUtils.getProperty(o1, "booleanField"));
	}

	@Test
	public void testGetProperty_Nested() {
		TstJavaBean o1 = createBean();
		assertNull(PropertyUtils.getProperty(o1, "related.integerField"));

		TstJavaBean o1related = new TstJavaBean();
		o1related.setIntegerField(44);
		o1.setRelated(o1related);

		assertEquals(44, PropertyUtils.getProperty(o1, "related.integerField"));
	}

	@Test
	public void testGetProperty_NestedOuter() {
		TstJavaBean o1 = createBean();
		assertNull(PropertyUtils.getProperty(o1, "related+.integerField"));

		TstJavaBean o1related = new TstJavaBean();
		o1related.setIntegerField(42);
		o1.setRelated(o1related);

		assertEquals(42, PropertyUtils.getProperty(o1, "related+.integerField"));
	}

	@Test
	public void testSetProperty() {
		TstJavaBean o1 = createBean();
		TstJavaBean o2 = new TstJavaBean();

		PropertyUtils.setProperty(o2, "byteArrayField", o1.getByteArrayField());
		PropertyUtils.setProperty(o2, "integerField", o1.getIntegerField());
		PropertyUtils.setProperty(o2, "intField", o1.getIntField());
		PropertyUtils.setProperty(o2, "numberField", o1.getNumberField());
		PropertyUtils.setProperty(o2, "objectField", o1.getObjectField());
		PropertyUtils.setProperty(o2, "stringField", o1.getStringField());
		PropertyUtils.setProperty(o2, "booleanField", o1.isBooleanField());
	}

	@Test
	public void testGetPropertyMap() {
		Map<String, Object> o1 = createMap();

		assertSame(o1.get("byteArrayField"), PropertyUtils.getProperty(o1, "byteArrayField"));
		assertSame(o1.get("integerField"), PropertyUtils.getProperty(o1, "integerField"));
		assertEquals(o1.get("intField"), PropertyUtils.getProperty(o1, "intField"));
		assertSame(o1.get("numberField"), PropertyUtils.getProperty(o1, "numberField"));
		assertSame(o1.get("objectField"), PropertyUtils.getProperty(o1, "objectField"));
		assertSame(o1.get("stringField"), PropertyUtils.getProperty(o1, "stringField"));
		assertEquals(o1.get("booleanField"), PropertyUtils.getProperty(o1, "booleanField"));
	}

	@Test
	public void testSetProperty_Nested() {
		TstJavaBean o1 = createBean();
		TstJavaBean o1related = new TstJavaBean();
		o1related.setIntegerField(44);
		o1.setRelated(o1related);

		PropertyUtils.setProperty(o1, "related.integerField", 55);
		assertEquals(Integer.valueOf(55), o1related.getIntegerField());
	}

	@Test
	public void testSetProperty_Null() {
		TstJavaBean o1 = createBean();

		PropertyUtils.setProperty(o1, "related.integerField", 55);
	}

	@Test
	public void testSetPropertyMap() {
		Map<String, Object> o1 = createMap();
		Map<String, Object> o2 = new HashMap<>();

		PropertyUtils.setProperty(o2, "byteArrayField", o1.get("byteArrayField"));
		PropertyUtils.setProperty(o2, "integerField", o1.get("integerField"));
		PropertyUtils.setProperty(o2, "intField", o1.get("intField"));
		PropertyUtils.setProperty(o2, "numberField", o1.get("numberField"));
		PropertyUtils.setProperty(o2, "objectField", o1.get("objectField"));
		PropertyUtils.setProperty(o2, "stringField", o1.get("stringField"));
		PropertyUtils.setProperty(o2, "booleanField", o1.get("booleanField"));

		assertEquals(o1, o2);
	}

	@Test
	public void testSetConverted() {
		TstJavaBean o1 = new TstJavaBean();

		// Object -> String
		Object object = new Object();
		PropertyUtils.setProperty(o1, "stringField", object);
		assertEquals(object.toString(), o1.getStringField());

		// String to number
		PropertyUtils.setProperty(o1, "integerField", "25");
		assertEquals(Integer.valueOf(25), o1.getIntegerField());

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
		assertEquals(4.5f, o1.getFloatField(), 0);

		// string to double primitive
		PropertyUtils.setProperty(o1, "doubleField", "5.5");
		assertEquals(5.5, o1.getDoubleField(), 0);

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
		assertEquals("abc", o1.getStringBuilderField().toString());
	}

	@Test
	public void testSetConvertedWithCustomConverter() {
		// save old converter for restore
		Converter<Date> oldConverter = ConverterFactory.factory.getConverter(Date.class);

		try {
			ConverterFactory.addConverter(Date.class, (value, type) -> {
				if (value == null)
					return null;
				if (value instanceof Date) {
					return (Date) value;
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
			});

			TstJavaBean o1 = new TstJavaBean();

			// String to date
			PropertyUtils.setProperty(o1, "dateField", "2013-08-01");

			Calendar cal = new GregorianCalendar(2013, Calendar.AUGUST, 1, 0, 0, 0);
			assertEquals(cal.getTime(), o1.getDateField());
		} finally {

			// restore global date converter
			ConverterFactory.addConverter(Date.class, oldConverter);
		}
	}

	@Test
	public void testSetNull() {
		TstJavaBean o1 = new TstJavaBean();

		o1.setStringField("xyz");
		PropertyUtils.setProperty(o1, "stringField", null);
		assertNull(o1.getStringField());

		o1.setBooleanField(true);
		PropertyUtils.setProperty(o1, "booleanField", null);
		assertFalse(o1.isBooleanField());

		o1.setByteField((byte) 2);
		PropertyUtils.setProperty(o1, "byteField", null);
		assertEquals((byte) 0, o1.getByteField());

		o1.setShortField((short) 3);
		PropertyUtils.setProperty(o1, "shortField", null);
		assertEquals((short) 0, o1.getShortField());

		o1.setIntField(99);
		PropertyUtils.setProperty(o1, "intField", null);
		assertEquals(0, o1.getIntField());

		o1.setLongField(98);
		PropertyUtils.setProperty(o1, "longField", null);
		assertEquals(0L, o1.getLongField());

		o1.setFloatField(4.5f);
		PropertyUtils.setProperty(o1, "floatField", null);
		assertEquals(0.0f, o1.getFloatField(), 0);

		o1.setDoubleField(5.5f);
		PropertyUtils.setProperty(o1, "doubleField", null);
		assertEquals(0.0, o1.getDoubleField(), 0);
	}

	@Test
	public void testSetConvertedEnum() {
		MockEnumHolder o1 = new MockEnumHolder();

		// String to Enum
		PropertyUtils.setProperty(o1, "mockEnum", "b");
		assertSame(MockEnum.b, o1.getMockEnum());

		// check that regular converters still work
		PropertyUtils.setProperty(o1, "number", "445");
		assertEquals(445, o1.getNumber());
	}

	private TstJavaBean createBean() {
		TstJavaBean o1 = new TstJavaBean();
		o1.setByteArrayField(new byte[] { 1, 2, 3 });
		o1.setIntegerField(33);
		o1.setIntField(-44);
		o1.setNumberField(new BigDecimal("11111"));
		o1.setObjectField(new Object());
		o1.setStringField("aaaaa");
		o1.setBooleanField(true);

		return o1;
	}

	private Map<String, Object> createMap() {
		Map<String, Object> o1 = new HashMap<>();
		o1.put("byteArrayField", new byte[] { 1, 2, 3 });
		o1.put("integerField", 33);
		o1.put("intField", -44);
		o1.put("numberField", new BigDecimal("11111"));
		o1.put("objectField", new Object());
		o1.put("stringField", "aaaaa");
		o1.put("booleanField", Boolean.TRUE);

		return o1;
	}
}
