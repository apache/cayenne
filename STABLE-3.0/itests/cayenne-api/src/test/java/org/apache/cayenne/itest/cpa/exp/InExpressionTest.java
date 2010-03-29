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
package org.apache.cayenne.itest.cpa.exp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.itest.cpa.CPAContextCase;
import org.apache.cayenne.itest.cpa.Enum1;
import org.apache.cayenne.itest.cpa.EnumEntity1;
import org.apache.cayenne.query.SelectQuery;

// inspired by CAY-832
public class InExpressionTest extends CPAContextCase {

	public void testInEnumsMappedAsChar() throws Exception {
		getDbHelper().deleteAll("enum_entity1");
		getDbHelper().insert("enum_entity1",
				new String[] { "id", "char_enum" }, new Object[] { 1, "One" });
		getDbHelper().insert("enum_entity1",
				new String[] { "id", "char_enum" }, new Object[] { 2, "Two" });
		getDbHelper()
				.insert("enum_entity1", new String[] { "id", "char_enum" },
						new Object[] { 3, "Three" });

		List<Enum1> enums = new ArrayList<Enum1>();
		enums.add(Enum1.Two);
		enums.add(Enum1.Four);

		Expression charMatch = ExpressionFactory.inExp(
				EnumEntity1.CHAR_ENUM_PROPERTY, enums);
		List results = getContext().performQuery(
				new SelectQuery(EnumEntity1.class, charMatch));
		assertEquals(1, results.size());

		EnumEntity1 o1 = (EnumEntity1) results.get(0);
		assertSame(Enum1.Two, o1.getCharEnum());
	}

	public void testInEnumsMappedAsInt() throws Exception {
		getDbHelper().deleteAll("enum_entity1");
		getDbHelper().insert("enum_entity1", new String[] { "id", "int_enum" },
				new Object[] { 1, 0 });
		getDbHelper().insert("enum_entity1", new String[] { "id", "int_enum" },
				new Object[] { 2, 1 });
		getDbHelper().insert("enum_entity1", new String[] { "id", "int_enum" },
				new Object[] { 3, 2 });

		List<Enum1> enums = new ArrayList<Enum1>();
		enums.add(Enum1.Two);
		enums.add(Enum1.Four);

		Expression charMatch = ExpressionFactory.inExp(
				EnumEntity1.INT_ENUM_PROPERTY, enums);
		List results = getContext().performQuery(
				new SelectQuery(EnumEntity1.class, charMatch));
		assertEquals(1, results.size());

		EnumEntity1 o1 = (EnumEntity1) results.get(0);
		assertSame(Enum1.Two, o1.getIntEnum());
	}

	public void testInEnumsMappedAsIntFromString() throws Exception {
		getDbHelper().deleteAll("enum_entity1");
		getDbHelper().insert("enum_entity1", new String[] { "id", "int_enum" },
				new Object[] { 1, 0 });
		getDbHelper().insert("enum_entity1", new String[] { "id", "int_enum" },
				new Object[] { 2, 1 });
		getDbHelper().insert("enum_entity1", new String[] { "id", "int_enum" },
				new Object[] { 3, 2 });

		List<Enum1> enums = new ArrayList<Enum1>();
		enums.add(Enum1.Two);
		enums.add(Enum1.Four);

		Expression charMatch = Expression.fromString("intEnum in $l")
				.expWithParameters(Collections.singletonMap("l", enums));

		List results = getContext().performQuery(
				new SelectQuery(EnumEntity1.class, charMatch));
		assertEquals(1, results.size());

		EnumEntity1 o1 = (EnumEntity1) results.get(0);
		assertSame(Enum1.Two, o1.getIntEnum());
	}
}
