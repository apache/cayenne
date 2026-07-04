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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.sql.SQLException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UtilTest {

	private File fTmpFileInCurrentDir;
	private String fTmpFileName;
	private File fTmpFileCopy;

	@BeforeEach
	public void setUp() throws Exception {
		fTmpFileName = "." + File.separator + System.currentTimeMillis() + ".tmp";

		fTmpFileInCurrentDir = new File(fTmpFileName);

		// right some garbage to the temp file, so that it is not empty
		try (FileWriter fout = new FileWriter(fTmpFileInCurrentDir)) {
			fout.write("This is total garbage..");
		}

		fTmpFileCopy = new File(fTmpFileName + ".copy");
	}

	@AfterEach
	public void tearDown() throws Exception {
		if (!fTmpFileInCurrentDir.delete())
			throw new Exception("Error deleting temporary file: " + fTmpFileInCurrentDir);

		if (fTmpFileCopy.exists() && !fTmpFileCopy.delete())
			throw new Exception("Error deleting temporary file: " + fTmpFileCopy);

	}

	@Test
	@SuppressWarnings("deprecation")
	public void getJavaClass() throws Exception {
		assertEquals(byte.class.getName(), Util.getJavaClass("byte").getName());
		assertEquals(byte[].class.getName(), Util.getJavaClass("byte[]").getName());
		assertEquals(String[].class.getName(), Util.getJavaClass("java.lang.String[]").getName());
		assertEquals(UtilTest[].class.getName(), Util.getJavaClass(getClass().getName() + "[]").getName());
	}

	@Test
	@SuppressWarnings("deprecation")
	public void toMap() {
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
		assertThrows(IllegalArgumentException.class, () -> Util.toMap(keys, values2));
	}

	@Test
	public void stripLineBreaks() {

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
	public void cloneViaSerialization() throws Exception {
		// need a special subclass of Object to make "clone" method public
		MockSerializable o1 = new MockSerializable();
		Object o2 = Util.cloneViaSerialization(o1);
		assertEquals(o1, o2);
		assertTrue(o1 != o2);
	}

	@Test
	public void packagePath1() throws Exception {
		String expectedPath = "org/apache/cayenne/util";
		assertEquals(expectedPath, Util.getPackagePath(UtilTest.class.getName()));
	}

	@Test
	public void packagePath2() throws Exception {
		// inner class
		class TmpTest extends Object {
		}

		String expectedPath = "org/apache/cayenne/util";
		assertEquals(expectedPath, Util.getPackagePath(TmpTest.class.getName()));
	}

	@Test
	public void packagePath3() throws Exception {
		assertEquals("", Util.getPackagePath("ClassWithNoPackage"));
	}

	@Test
	public void isEmptyString1() throws Exception {
		assertTrue(Util.isEmptyString(""));
	}

	@Test
	public void isEmptyString2() throws Exception {
		assertFalse(Util.isEmptyString("  "));
	}

	@Test
	public void isEmptyString3() throws Exception {
		assertTrue(Util.isEmptyString(null));
	}

	@Test
	public void backslashFix() throws Exception {
		String strBefore = "abcd\\12345\\";
		String strAfter = "abcd/12345/";
		assertEquals(strAfter, Util.substBackslashes(strBefore));
	}

	@Test
	@SuppressWarnings("deprecation")
	public void nullSafeEquals() throws Exception {
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
	public void extractFileExtension1() throws Exception {
		String fullName = "n.ext";
		assertEquals("ext", Util.extractFileExtension(fullName));
	}

	@Test
	public void extractFileExtension2() throws Exception {
		String fullName = "n";
		assertNull(Util.extractFileExtension(fullName));
	}

	@Test
	public void extractFileExtension3() throws Exception {
		String fullName = ".ext";
		assertNull(Util.extractFileExtension(fullName));
	}

	@Test
	public void stripFileExtension1() throws Exception {
		String fullName = "n.ext";
		assertEquals("n", Util.stripFileExtension(fullName));
	}

	@Test
	public void stripFileExtension2() throws Exception {
		String fullName = "n";
		assertEquals("n", Util.stripFileExtension(fullName));
	}

	@Test
	public void stripFileExtension3() throws Exception {
		String fullName = ".ext";
		assertEquals(".ext", Util.stripFileExtension(fullName));
	}

	@Test
	public void encodeXmlAttribute1() throws Exception {
		String unencoded = "normalstring";
		assertEquals(unencoded, Util.encodeXmlAttribute(unencoded));
	}

	@Test
	public void encodeXmlAttribute2() throws Exception {
		String unencoded = "<a>";
		assertEquals("&lt;a&gt;", Util.encodeXmlAttribute(unencoded));
	}

	@Test
	public void encodeXmlAttribute3() throws Exception {
		String unencoded = "a&b";
		assertEquals("a&amp;b", Util.encodeXmlAttribute(unencoded));
	}

	@Test
	public void unwindException1() throws Exception {
		Throwable e = new Throwable();
		assertSame(e, Util.unwindException(e));
	}

	@Test
	public void unwindException2() throws Exception {
		CayenneRuntimeException e = new CayenneRuntimeException();
		assertSame(e, Util.unwindException(e));
	}

	@Test
	public void unwindException3() throws Exception {
		Throwable root = new Throwable();
		CayenneRuntimeException e = new CayenneRuntimeException(root);
		assertSame(root, Util.unwindException(e));
	}

	@Test
	public void prettyTrim1() throws Exception {
		// size is too short, must throw
		assertThrows(IllegalArgumentException.class, () -> Util.prettyTrim("abc", 4));
	}

	@Test
	public void prettyTrim2() throws Exception {
		assertEquals("123", Util.prettyTrim("123", 6));
		assertEquals("123456", Util.prettyTrim("123456", 6));
		assertEquals("1...67", Util.prettyTrim("1234567", 6));
		assertEquals("1...78", Util.prettyTrim("12345678", 6));
	}

	@Test
	public void underscoredToJava1() throws Exception {
		String expected = "ClassNameIdentifier";
		assertEquals(expected, Util.underscoredToJava(
				"_CLASS_NAME_IDENTIFIER_",
				true));
	}

	@Test
	public void underscoredToJava2() throws Exception {
		String expected = "propNameIdentifier123";
		assertEquals(expected, Util.underscoredToJava(
				"_prop_name_Identifier_123",
				false));
	}

	@Test
	public void underscoredToJava3() throws Exception {
		String expected = "lastName";
		assertEquals(expected, Util.underscoredToJava("lastName", false));
	}

	@Test
	public void underscoredToJava4() throws Exception {
		String expected = "lastName";
		assertEquals(expected, Util.underscoredToJava("LastName", false));
	}

	@Test
	public void underscoredToJava5() throws Exception {
		String expected = "LastName";
		assertEquals(expected, Util.underscoredToJava("LastName", true));
	}

	@Test
	public void underscoredToJavaSpecialChars() throws Exception {
		assertEquals("ABCpoundXyz", Util.underscoredToJava("ABC#_XYZ", true));
	}

	@Test
	public void unwindException() {
		SQLException sql = new SQLException("bad sql");
		CayenneRuntimeException wrapper = new CayenneRuntimeException(sql);

		// the plain form unwinds all the way to the non-Cayenne root
		assertSame(sql, Util.unwindException(wrapper));
	}

	@Test
	public void unwindExceptionUpTo_stopsAtWrapper() {
		SQLException sql = new SQLException("bad sql");
		CayenneRuntimeException wrapper = new CayenneRuntimeException(sql);

		// does not unwind past the CayenneRuntimeException into its lower-level SQLException cause
		assertSame(wrapper, Util.unwindException(wrapper, CayenneRuntimeException.class));
	}

	@Test
	public void unwindExceptionUpTo_returnsInnermostMatch() {
		SQLException sql = new SQLException("bad sql");
		CayenneRuntimeException inner = new CayenneRuntimeException("inner", sql);
		CayenneRuntimeException outer = new CayenneRuntimeException("outer", inner);

		// generic outer wrapper is stripped, but unwinding stops at the innermost matching exception
		assertSame(inner, Util.unwindException(outer, CayenneRuntimeException.class));
	}

	@Test
	public void unwindExceptionUpTo_noMatchUnwindsToRoot() {
		IllegalArgumentException root = new IllegalArgumentException("root");
		RuntimeException wrapper = new RuntimeException(root);

		// no exception of the requested type in the chain, so behaves like the plain unwind
		assertSame(root, Util.unwindException(wrapper, SQLException.class));
	}
}
