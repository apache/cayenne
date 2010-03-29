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
package org.apache.cayenne.conf;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;

import org.apache.cayenne.unit.BasicCase;

public class ClasspathResourceFinderTest extends BasicCase {

    public void testClassLoader() {

        ClasspathResourceFinder finder = new ClasspathResourceFinder();
        assertNull(finder.getClassLoader());

        ClassLoader cl1 = new URLClassLoader(new URL[0]);
        finder.setClassLoader(cl1);
        assertSame(cl1, finder.getClassLoader());
    }

    public void testGetResourceClassLoader() {

        ClasspathResourceFinder f1 = new ClasspathResourceFinder();
        ClassLoader cl1 = new URLClassLoader(new URL[0]);
        f1.setClassLoader(cl1);
        assertSame(cl1, f1.getResourceClassLoader());

        ClasspathResourceFinder f2 = new ClasspathResourceFinder();
        ClassLoader cl2 = new URLClassLoader(new URL[0]);
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(cl2);
            assertSame(cl2, f2.getResourceClassLoader());

            Thread.currentThread().setContextClassLoader(null);
            assertSame(ClasspathResourceFinder.class.getClassLoader(), f2
                    .getResourceClassLoader());
        }
        finally {
            Thread.currentThread().setContextClassLoader(old);
        }
    }
    
    public void testRootPaths() {
        ClasspathResourceFinder f1 = new ClasspathResourceFinder();
        assertEquals(1, f1.rootPaths.size());
        assertTrue(f1.rootPaths.contains(""));
        
        f1.addRootPath("/");
        assertEquals(1, f1.rootPaths.size());
        assertTrue(f1.rootPaths.contains(""));
        
        f1.addRootPath("/my/package/name/");
        assertEquals(2, f1.rootPaths.size());
        assertTrue("" + f1.rootPaths, f1.rootPaths.contains("my/package/name"));
    }
    
    public void testGetResource() {
        ClasspathResourceFinder f1 = new ClasspathResourceFinder();
        assertNotNull(f1.getResource("org/apache/cayenne/Persistent.class"));
        
        assertNull(f1.getResource("/apache/cayenne/Persistent.class"));
        f1.addRootPath("org");
        assertNotNull(f1.getResource("/apache/cayenne/Persistent.class"));
    }
    
    public void testGetResources() {
        ClasspathResourceFinder f1 = new ClasspathResourceFinder();
        Collection<URL> r1 = f1.getResources("org/apache/cayenne/Persistent.class");
        assertNotNull(r1);
        assertEquals(1, r1.size());
        
        Collection<URL> r2 = f1.getResources("/apache/cayenne/Persistent.class");
        assertNotNull(r2);
        assertEquals(0, r2.size());
        f1.addRootPath("org");
        Collection<URL> r3 = f1.getResources("/apache/cayenne/Persistent.class");
        assertNotNull(r3);
        assertEquals(1, r3.size());
    }
}
