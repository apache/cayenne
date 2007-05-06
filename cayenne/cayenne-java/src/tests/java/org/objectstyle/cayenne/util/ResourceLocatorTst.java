/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.util;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;

import junit.framework.TestCase;

public class ResourceLocatorTst extends TestCase {

    private File fTmpFileInCurrentDir;
    private String fTmpFileName;

    protected void setUp() throws java.lang.Exception {
        fTmpFileName = System.currentTimeMillis() + ".tmp";
        fTmpFileInCurrentDir = new File("." + File.separator + fTmpFileName);

        // right some garbage to the temp file, so that it is not empty
        FileWriter fout = new FileWriter(fTmpFileInCurrentDir);
        fout.write("This is total garbage..");
        fout.close();
    }

    protected void tearDown() throws java.lang.Exception {
        if (!fTmpFileInCurrentDir.delete())
            throw new Exception("Error deleting temporary file: " + fTmpFileInCurrentDir);
    }

    public void testFindResourceInCurrentDirectory() throws java.lang.Exception {
        InputStream in = ResourceLocator.findResourceInFileSystem(fTmpFileName);
        try {
            assertNotNull(in);
        }
        finally {
            in.close();
        }
    }

    public void testClassBaseUrl() throws java.lang.Exception {
        String me = ResourceLocator.classBaseUrl(this.getClass());
        assertNotNull(me);
        assertTrue("Expected 'jar:' or 'file:' URL, got " + me, me.startsWith("jar:")
                || me.startsWith("file:"));
    }

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
