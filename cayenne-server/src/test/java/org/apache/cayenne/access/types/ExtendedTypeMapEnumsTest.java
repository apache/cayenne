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

package org.apache.cayenne.access.types;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.apache.cayenne.access.types.InnerEnumHolder.InnerEnum;
import org.junit.Before;
import org.junit.Test;

public class ExtendedTypeMapEnumsTest {

	private ExtendedTypeMap map;

	@Before
	public void before() {
		this.map = new ExtendedTypeMap();
	}

	@Test
	public void testCreateType_NoFactory() {
		assertNull(map.createType(Object.class.getName()));
	}

	@Test
	public void testCreateType_Enum() {

		ExtendedType type1 = map.createType(MockEnum.class.getName());
		assertTrue(type1 instanceof EnumType);
		assertEquals(MockEnum.class, ((EnumType<?>) type1).enumClass);

		ExtendedType type2 = map.createType(MockEnum2.class.getName());
		assertNotSame(type1, type2);
	}

	@Test
	public void testCreateType_InnerEnum() {

		ExtendedType type = map.createType(InnerEnumHolder.InnerEnum.class.getName());
		assertTrue(type instanceof EnumType);
		assertEquals(InnerEnumHolder.InnerEnum.class, ((EnumType<?>) type).enumClass);

		// use a string name with $
		ExtendedType type1 = map.createType(InnerEnumHolder.class.getName() + "$InnerEnum");
		assertNotNull(type1);
		assertEquals(type.getClassName(), type1.getClassName());

		// use a string name with .
		ExtendedType type2 = map.createType(InnerEnumHolder.class.getName() + ".InnerEnum");
		assertNotNull(type2);
		assertEquals(type.getClassName(), type2.getClassName());
	}

	@Test
	public void testGetRegisteredType() {
		ExtendedType type = map.getRegisteredType(MockEnum.class);
		assertNotNull(type);
		assertTrue(type instanceof EnumType);

		assertSame(type, map.getRegisteredType(MockEnum.class));
		assertSame(type, map.getRegisteredType(MockEnum.class.getName()));
	}

	@Test
	public void testGetRegisteredType_InnerEnum() {

		assertEquals(0, map.extendedTypeFactories.size());

		ExtendedType byType = map.getRegisteredType(InnerEnum.class);

		// this and subsequent tests verify that no memory leak occurs per CAY-2066
		assertEquals(0, map.extendedTypeFactories.size());

		assertSame(byType, map.getRegisteredType(InnerEnum.class));
		assertEquals(0, map.extendedTypeFactories.size());

		assertSame(byType, map.getRegisteredType(InnerEnumHolder.class.getName() + "$InnerEnum"));
		assertEquals(0, map.extendedTypeFactories.size());

		assertSame(byType, map.getRegisteredType(InnerEnumHolder.class.getName() + ".InnerEnum"));
		assertEquals(0, map.extendedTypeFactories.size());
	}
}
