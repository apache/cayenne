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

import junit.framework.TestCase;

import org.apache.cayenne.CayenneException;

public class UtilExtTest extends TestCase {

	public void testPackagePath1() throws java.lang.Exception {
		String expectedPath = "org/apache/cayenne/util";
		assertEquals(
			expectedPath,
			Util.getPackagePath(UtilExtTest.class.getName()));
	}

	public void testPackagePath2() throws java.lang.Exception {
		// inner class
		class TmpTest extends Object {
		}

		String expectedPath = "org/apache/cayenne/util";
		assertEquals(
			expectedPath,
			Util.getPackagePath(TmpTest.class.getName()));
	}

	public void testPackagePath3() throws java.lang.Exception {
		assertEquals("", Util.getPackagePath("ClassWithNoPackage"));
	}

	public void testIsEmptyString1() throws Exception {
		assertTrue(Util.isEmptyString(""));
	}

	public void testIsEmptyString2() throws Exception {
		assertFalse(Util.isEmptyString("  "));
	}

	public void testIsEmptyString3() throws Exception {
		assertTrue(Util.isEmptyString(null));
	}

	public void testBackslashFix() throws java.lang.Exception {
		String strBefore = "abcd\\12345\\";
		String strAfter = "abcd/12345/";
		assertEquals(strAfter, Util.substBackslashes(strBefore));
	}

	public void testNullSafeEquals() throws java.lang.Exception {
		// need a special subclass of Object to make "clone" method public
		class CloneableObject implements Cloneable {

			@Override
            public Object clone() throws CloneNotSupportedException {
				return super.clone();
			}

			@Override
            public boolean equals(Object obj) {
				if (obj == null)
					return false;

				// for the purpose of this test
				// all objects of this class considered equal
				// (since they carry no state)
				return obj.getClass() == this.getClass();
			}
		}

		CloneableObject o1 = new CloneableObject();
		Object o2 = new Object();
		Object o3 = o1.clone();

		assertTrue(o3.equals(o1));
		assertTrue(Util.nullSafeEquals(o1, o1));
		assertFalse(Util.nullSafeEquals(o1, o2));
		assertTrue(Util.nullSafeEquals(o1, o3));
		assertFalse(Util.nullSafeEquals(o1, null));
		assertFalse(Util.nullSafeEquals(null, o1));
		assertTrue(Util.nullSafeEquals(null, null));
	}

	public void testExtractFileExtension1() throws Exception {
		String fullName = "n.ext";
		assertEquals("ext", Util.extractFileExtension(fullName));
	}

	public void testExtractFileExtension2() throws Exception {
		String fullName = "n";
		assertNull(Util.extractFileExtension(fullName));
	}

	public void testExtractFileExtension3() throws Exception {
		String fullName = ".ext";
		assertNull(Util.extractFileExtension(fullName));
	}

	public void testStripFileExtension1() throws Exception {
		String fullName = "n.ext";
		assertEquals("n", Util.stripFileExtension(fullName));
	}

	public void testStripFileExtension2() throws Exception {
		String fullName = "n";
		assertEquals("n", Util.stripFileExtension(fullName));
	}

	public void testStripFileExtension3() throws Exception {
		String fullName = ".ext";
		assertEquals(".ext", Util.stripFileExtension(fullName));
	}

	public void testEncodeXmlAttribute1() throws Exception {
		String unencoded = "normalstring";
		assertEquals(unencoded, Util.encodeXmlAttribute(unencoded));
	}

	public void testEncodeXmlAttribute2() throws Exception {
		String unencoded = "<a>";
		assertEquals("&lt;a&gt;", Util.encodeXmlAttribute(unencoded));
	}

	public void testEncodeXmlAttribute3() throws Exception {
		String unencoded = "a&b";
		assertEquals("a&amp;b", Util.encodeXmlAttribute(unencoded));
	}

	public void testUnwindException1() throws Exception {
		Throwable e = new Throwable();
		assertSame(e, Util.unwindException(e));
	}

	public void testUnwindException2() throws Exception {
		CayenneException e = new CayenneException();
		assertSame(e, Util.unwindException(e));
	}

	public void testUnwindException3() throws Exception {
		Throwable root = new Throwable();
		CayenneException e = new CayenneException(root);
		assertSame(root, Util.unwindException(e));
	}

	public void testPrettyTrim1() throws Exception {
		// size is too short, must throw
		try {
			Util.prettyTrim("abc", 4);
		} catch (IllegalArgumentException ex) {
			// expected
		}
	}

	public void testPrettyTrim2() throws Exception {
		assertEquals("123", Util.prettyTrim("123", 6));
		assertEquals("123456", Util.prettyTrim("123456", 6));
		assertEquals("1...67", Util.prettyTrim("1234567", 6));
		assertEquals("1...78", Util.prettyTrim("12345678", 6));
	}
}
