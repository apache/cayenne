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

package org.apache.cayenne.util;

import org.apache.cayenne.CayenneRuntimeException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class UtilTest {

	private File fTmpFileInCurrentDir;
	private String fTmpFileName;
	private File fTmpFileCopy;

	@Before
	public void setUp() throws Exception {
		fTmpFileName = "." + File.separator + System.currentTimeMillis() + ".tmp";

		fTmpFileInCurrentDir = new File(fTmpFileName);

		// right some garbage to the temp file, so that it is not empty
		try (FileWriter fout = new FileWriter(fTmpFileInCurrentDir)) {
			fout.write("This is total garbage..");
		}
		
		fTmpFileCopy = new File(fTmpFileName + ".copy");
	}

	@After
	public void tearDown() throws Exception {
		if (!fTmpFileInCurrentDir.delete())
			throw new Exception("Error deleting temporary file: " + fTmpFileInCurrentDir);

		if (fTmpFileCopy.exists() && !fTmpFileCopy.delete())
			throw new Exception("Error deleting temporary file: " + fTmpFileCopy);

	}

	@Test
	@SuppressWarnings("deprecation")
	public void testGetJavaClass() throws Exception {
		assertEquals(byte.class.getName(), Util.getJavaClass("byte").getName());
		assertEquals(byte[].class.getName(), Util.getJavaClass("byte[]").getName());
		assertEquals(String[].class.getName(), Util.getJavaClass("java.lang.String[]").getName());
		assertEquals(UtilTest[].class.getName(), Util.getJavaClass(getClass().getName() + "[]").getName());
	}

	@Test
	public void testToMap() {
		Object[] keys = new Object[] { "a", "b" };
		Object[] values = new Object[] { "1", "2" };

		Map<Object, Object> map = Util.toMap(keys, values);
		assertEquals(2, map.size());
		assertEquals("1", map.get("a"));
		assertEquals("2", map.get("b"));

		// check that map is mutable
		map.put("c", "3");

		// check that two null maps work
		Map<Object, Object> emptyMap = Util.toMap(null, new Object[0]);
		assertTrue(emptyMap.isEmpty());
		emptyMap.put("key1", "value1");

		// check arrays with different sizes
		Object[] values2 = new Object[] { "1" };
		try {
			Util.toMap(keys, values2);
			fail("must have thrown");
		} catch (IllegalArgumentException e) {
			// expected
		}
	}

	@Test
	public void testStripLineBreaks() {

		// no breaks
		assertEquals("PnMusdkams34 H AnYtk M", Util.stripLineBreaks("PnMusdkams34 H AnYtk M", 'A'));

		// Windows
		assertEquals("TyusdsdsdQaAbAc", Util.stripLineBreaks("TyusdsdsdQa\r\nb\r\nc", 'A'));

		// Mac
		assertEquals("aBbBc", Util.stripLineBreaks("a\rb\rc", 'B'));

		// UNIX
		assertEquals("aCbCc", Util.stripLineBreaks("a\nb\nc", 'C'));
	}

	@Test
	public void testCloneViaSerialization() throws Exception {
		// need a special subclass of Object to make "clone" method public
		MockSerializable o1 = new MockSerializable();
		Object o2 = Util.cloneViaSerialization(o1);
		assertEquals(o1, o2);
		assertTrue(o1 != o2);
	}

	@Test
	public void testPackagePath1() throws Exception {
		String expectedPath = "org/apache/cayenne/util";
		assertEquals(expectedPath, Util.getPackagePath(UtilTest.class.getName()));
	}

	@Test
	public void testPackagePath2() throws Exception {
		// inner class
		class TmpTest extends Object {
		}

		String expectedPath = "org/apache/cayenne/util";
		assertEquals(expectedPath, Util.getPackagePath(TmpTest.class.getName()));
	}

	@Test
	public void testPackagePath3() throws Exception {
		assertEquals("", Util.getPackagePath("ClassWithNoPackage"));
	}

	@Test
	public void testIsEmptyString1() throws Exception {
		assertTrue(Util.isEmptyString(""));
	}

	@Test
	public void testIsEmptyString2() throws Exception {
		assertFalse(Util.isEmptyString("  "));
	}

	@Test
	public void testIsEmptyString3() throws Exception {
		assertTrue(Util.isEmptyString(null));
	}

	@Test
	public void testBackslashFix() throws Exception {
		String strBefore = "abcd\\12345\\";
		String strAfter = "abcd/12345/";
		assertEquals(strAfter, Util.substBackslashes(strBefore));
	}

	@Test
	public void testNullSafeEquals() throws Exception {
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

	@Test
	public void testExtractFileExtension1() throws Exception {
		String fullName = "n.ext";
		assertEquals("ext", Util.extractFileExtension(fullName));
	}

	@Test
	public void testExtractFileExtension2() throws Exception {
		String fullName = "n";
		assertNull(Util.extractFileExtension(fullName));
	}

	@Test
	public void testExtractFileExtension3() throws Exception {
		String fullName = ".ext";
		assertNull(Util.extractFileExtension(fullName));
	}

	@Test
	public void testStripFileExtension1() throws Exception {
		String fullName = "n.ext";
		assertEquals("n", Util.stripFileExtension(fullName));
	}

	@Test
	public void testStripFileExtension2() throws Exception {
		String fullName = "n";
		assertEquals("n", Util.stripFileExtension(fullName));
	}

	@Test
	public void testStripFileExtension3() throws Exception {
		String fullName = ".ext";
		assertEquals(".ext", Util.stripFileExtension(fullName));
	}

	@Test
	public void testEncodeXmlAttribute1() throws Exception {
		String unencoded = "normalstring";
		assertEquals(unencoded, Util.encodeXmlAttribute(unencoded));
	}

	@Test
	public void testEncodeXmlAttribute2() throws Exception {
		String unencoded = "<a>";
		assertEquals("&lt;a&gt;", Util.encodeXmlAttribute(unencoded));
	}

	@Test
	public void testEncodeXmlAttribute3() throws Exception {
		String unencoded = "a&b";
		assertEquals("a&amp;b", Util.encodeXmlAttribute(unencoded));
	}

	@Test
	public void testUnwindException1() throws Exception {
		Throwable e = new Throwable();
		assertSame(e, Util.unwindException(e));
	}

	@Test
	public void testUnwindException2() throws Exception {
		CayenneRuntimeException e = new CayenneRuntimeException();
		assertSame(e, Util.unwindException(e));
	}

	@Test
	public void testUnwindException3() throws Exception {
		Throwable root = new Throwable();
		CayenneRuntimeException e = new CayenneRuntimeException(root);
		assertSame(root, Util.unwindException(e));
	}

	@Test
	public void testPrettyTrim1() throws Exception {
		// size is too short, must throw
		try {
			Util.prettyTrim("abc", 4);
		} catch (IllegalArgumentException ex) {
			// expected
		}
	}

	@Test
	public void testPrettyTrim2() throws Exception {
		assertEquals("123", Util.prettyTrim("123", 6));
		assertEquals("123456", Util.prettyTrim("123456", 6));
		assertEquals("1...67", Util.prettyTrim("1234567", 6));
		assertEquals("1...78", Util.prettyTrim("12345678", 6));
	}

	@Test
	public void testUnderscoredToJava1() throws Exception {
		String expected = "ClassNameIdentifier";
		assertEquals(expected, Util.underscoredToJava(
				"_CLASS_NAME_IDENTIFIER_",
				true));
	}

	@Test
	public void testUnderscoredToJava2() throws Exception {
		String expected = "propNameIdentifier123";
		assertEquals(expected, Util.underscoredToJava(
				"_prop_name_Identifier_123",
				false));
	}

	@Test
	public void testUnderscoredToJava3() throws Exception {
		String expected = "lastName";
		assertEquals(expected, Util.underscoredToJava("lastName", false));
	}

	@Test
	public void testUnderscoredToJava4() throws Exception {
		String expected = "lastName";
		assertEquals(expected, Util.underscoredToJava("LastName", false));
	}

	@Test
	public void testUnderscoredToJava5() throws Exception {
		String expected = "LastName";
		assertEquals(expected, Util.underscoredToJava("LastName", true));
	}

	@Test
	public void testUnderscoredToJavaSpecialChars() throws Exception {
		assertEquals("ABCpoundXyz", Util.underscoredToJava("ABC#_XYZ", true));
	}
}
