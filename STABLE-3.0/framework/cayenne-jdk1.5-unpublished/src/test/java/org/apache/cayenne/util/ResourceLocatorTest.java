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
import java.io.InputStream;

import junit.framework.TestCase;

public class ResourceLocatorTest extends TestCase {

    private File fTmpFileInCurrentDir;
    private String fTmpFileName;

    @Override
    protected void setUp() throws java.lang.Exception {
        fTmpFileName = System.currentTimeMillis() + ".tmp";
        fTmpFileInCurrentDir = new File("." + File.separator + fTmpFileName);

        // right some garbage to the temp file, so that it is not empty
        FileWriter fout = new FileWriter(fTmpFileInCurrentDir);
        fout.write("This is total garbage..");
        fout.close();
    }

    @Override
    protected void tearDown() throws java.lang.Exception {
        if (!fTmpFileInCurrentDir.delete())
            throw new Exception("Error deleting temporary file: " + fTmpFileInCurrentDir);
    }

    /**
     * @deprecated since 3.0 unused
     */
    public void testFindResourceInCurrentDirectory() throws java.lang.Exception {
        InputStream in = ResourceLocator.findResourceInFileSystem(fTmpFileName);
        try {
            assertNotNull(in);
        }
        finally {
            in.close();
        }
    }

    /**
     * @deprecated since 3.0
     */
    public void testClassBaseUrl() throws java.lang.Exception {
        String me = ResourceLocator.classBaseUrl(this.getClass());
        assertNotNull(me);
        assertTrue("Expected 'jar:' or 'file:' URL, got " + me, me.startsWith("jar:")
                || me.startsWith("file:"));
    }

    /**
     * @deprecated since 3.0 unused
     */
    public void testFindResourceInClasspath() throws java.lang.Exception {
        InputStream in = ResourceLocator.findResourceInClasspath("testfile1.txt");
        try {
            assertNotNull(in);
        }
        finally {
            in.close();
        }
    }

    public void testFindResourceWithCustomClassPath() throws java.lang.Exception {
        ResourceLocator l = new ResourceLocator();
        l.setSkipAbsolutePath(true);
        l.setSkipCurrentDirectory(true);
        l.setSkipHomeDirectory(true);
        assertNotNull(l.findResource("testfile1.txt"));
    }

}
