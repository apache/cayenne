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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;

import junit.framework.TestCase;

public class UtilTest extends TestCase {

    private File fTmpFileInCurrentDir;
    private String fTmpFileName;
    private File fTmpFileCopy;

    @Override
    protected void setUp() throws Exception {
        fTmpFileName = "." + File.separator + System.currentTimeMillis() + ".tmp";

        fTmpFileInCurrentDir = new File(fTmpFileName);

        // right some garbage to the temp file, so that it is not empty
        FileWriter fout = new FileWriter(fTmpFileInCurrentDir);
        fout.write("This is total garbage..");
        fout.close();

        fTmpFileCopy = new File(fTmpFileName + ".copy");
    }

    @Override
    protected void tearDown() throws java.lang.Exception {
        if (!fTmpFileInCurrentDir.delete())
            throw new Exception("Error deleting temporary file: " + fTmpFileInCurrentDir);

        if (fTmpFileCopy.exists() && !fTmpFileCopy.delete())
            throw new Exception("Error deleting temporary file: " + fTmpFileCopy);

    }

    public void testGetJavaClass() throws Exception {
        assertEquals(byte.class.getName(), Util.getJavaClass("byte").getName());
        assertEquals(byte[].class.getName(), Util.getJavaClass("byte[]").getName());
        assertEquals(String[].class.getName(), Util
                .getJavaClass("java.lang.String[]")
                .getName());
        assertEquals(new UtilTest[0].getClass().getName(), Util.getJavaClass(
                getClass().getName() + "[]").getName());
    }

    public void testToMap() {
        Object[] keys = new Object[] {
                "a", "b"
        };
        Object[] values = new Object[] {
                "1", "2"
        };

        Map map = Util.toMap(keys, values);
        assertEquals(2, map.size());
        assertEquals("1", map.get("a"));
        assertEquals("2", map.get("b"));

        // check that map is mutable
        map.put("c", "3");

        // check that two null maps work
        Map emptyMap = Util.toMap(null, new Object[0]);
        assertTrue(emptyMap.isEmpty());
        emptyMap.put("key1", "value1");

        // check arrays with different sizes
        Object[] values2 = new Object[] {
            "1"
        };
        try {
            Util.toMap(keys, values2);
            fail("must have thrown");
        }
        catch (IllegalArgumentException e) {
            // expected
        }
    }

    public void testStripLineBreaks() {
        // Windows
        assertEquals("aAbAc", Util.stripLineBreaks("a\r\nb\r\nc", "A"));

        // Mac
        assertEquals("aBbBc", Util.stripLineBreaks("a\rb\rc", "B"));

        // UNIX
        assertEquals("aCbCc", Util.stripLineBreaks("a\nb\nc", "C"));
    }

    public void testCopyFile() throws java.lang.Exception {
        assertFalse("Temp file "
                + fTmpFileCopy
                + " is on the way, please delete it manually.", fTmpFileCopy.exists());
        assertTrue(Util.copy(fTmpFileInCurrentDir, fTmpFileCopy));
        assertTrue(fTmpFileCopy.exists());
        assertEquals(fTmpFileCopy.length(), fTmpFileInCurrentDir.length());
    }

    public void testCopyFileUrl() throws java.lang.Exception {
        assertFalse("Temp file "
                + fTmpFileCopy
                + " is on the way, please delete it manually.", fTmpFileCopy.exists());
        assertTrue(Util.copy(fTmpFileInCurrentDir.toURL(), fTmpFileCopy));
        assertTrue(fTmpFileCopy.exists());
        assertEquals(fTmpFileCopy.length(), fTmpFileInCurrentDir.length());
    }

    public void testCopyJarUrl() throws Exception {
        URL fileInJar = getClass().getClassLoader().getResource("testfile1.txt");
        assertNotNull(fileInJar);

        // skipping test if file not in jar
        if (!fileInJar.toExternalForm().startsWith("jar:")) {
            return;
        }

        assertTrue(Util.copy(fileInJar, fTmpFileCopy));
        assertTrue(fTmpFileCopy.exists());

        // check file size in a jar
        InputStream in = null;
        try {
            in = fileInJar.openConnection().getInputStream();
            int len = 0;
            while (in.read() >= 0) {
                len++;
            }
            assertEquals(len, fTmpFileCopy.length());
        }
        catch (IOException ioex) {
            fail();
        }
        finally {
            if (in != null)
                in.close();
        }

    }

    public void testDeleteFile() throws java.lang.Exception {
        // delete file
        assertFalse("Temp file "
                + fTmpFileCopy
                + " is on the way, please delete it manually.", fTmpFileCopy.exists());
        Util.copy(fTmpFileInCurrentDir, fTmpFileCopy);
        assertTrue(Util.delete(fTmpFileCopy.getPath(), false));

        // delete empty dir with no recursion
        String tmpDirName = "tmpdir_" + System.currentTimeMillis();
        File tmpDir = new File(tmpDirName);
        assertTrue(tmpDir.mkdir());
        assertTrue(Util.delete(tmpDirName, false));
        assertFalse(tmpDir.exists());

        // delete dir with files with recurions
        assertTrue(tmpDir.mkdir());
        assertTrue(new File(tmpDir, "aaa").createNewFile());
        assertTrue(Util.delete(tmpDirName, true));
        assertFalse(tmpDir.exists());

        // fail delete dir with files with no recurions
        assertTrue(tmpDir.mkdir());
        assertTrue(new File(tmpDir, "aaa").createNewFile());
        assertFalse(Util.delete(tmpDirName, false));
        assertTrue(tmpDir.exists());
        assertTrue(Util.delete(tmpDirName, true));
        assertFalse(tmpDir.exists());
    }

    public void testCloneViaSerialization() throws java.lang.Exception {
        // need a special subclass of Object to make "clone" method public
        MockSerializable o1 = new MockSerializable();
        Object o2 = Util.cloneViaSerialization(o1);
        assertEquals(o1, o2);
        assertTrue(o1 != o2);
    }
}
