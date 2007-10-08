/*
 * ==================================================================== The ObjectStyle
 * Group Software License, version 1.1 ObjectStyle Group - http://objectstyle.org/
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors of the
 * software. All rights reserved. Redistribution and use in source and binary forms, with
 * or without modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer. 2. Redistributions in binary form must
 * reproduce the above copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided with the distribution.
 * 3. The end-user documentation included with the redistribution, if any, must include
 * the following acknowlegement: "This product includes software developed by independent
 * contributors and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 * Alternately, this acknowlegement may appear in the software itself, if and wherever
 * such third-party acknowlegements normally appear. 4. The names "ObjectStyle Group" and
 * "Cayenne" must not be used to endorse or promote products derived from this software
 * without prior written permission. For written permission, email "andrus at objectstyle
 * dot org". 5. Products derived from this software may not be called "ObjectStyle" or
 * "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their names without prior
 * written permission. THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE OBJECTSTYLE
 * GROUP OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * ==================================================================== This software
 * consists of voluntary contributions made by many individuals and hosted on ObjectStyle
 * Group web site. For more information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;

import junit.framework.TestCase;

public class UtilTst extends TestCase {

    private File fTmpFileInCurrentDir;
    private String fTmpFileName;
    private File fTmpFileCopy;

    protected void setUp() throws Exception {
        fTmpFileName = "." + File.separator + System.currentTimeMillis() + ".tmp";

        fTmpFileInCurrentDir = new File(fTmpFileName);

        // right some garbage to the temp file, so that it is not empty
        FileWriter fout = new FileWriter(fTmpFileInCurrentDir);
        fout.write("This is total garbage..");
        fout.close();

        fTmpFileCopy = new File(fTmpFileName + ".copy");
    }

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
        assertEquals(new UtilTst[0].getClass().getName(), Util.getJavaClass(
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
        URL fileInJar = ClassLoader.getSystemResource("testfile1.txt");
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