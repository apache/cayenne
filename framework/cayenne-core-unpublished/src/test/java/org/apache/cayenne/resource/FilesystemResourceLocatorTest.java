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
package org.apache.cayenne.resource;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import junit.framework.TestCase;

import org.apache.cayenne.test.file.FileUtil;

public class FilesystemResourceLocatorTest extends TestCase {

    public void testArrayConstructor() {
        FilesystemResourceLocator l1 = new FilesystemResourceLocator();
        assertEquals(1, l1.roots.length);
        assertEquals(System.getProperty("user.dir"), l1.roots[0].getPath());

        File base = FileUtil.baseTestDirectory();
        File f1 = new File(base, "f1");
        File f2 = new File(new File(base, "f2"), "f3");

        FilesystemResourceLocator l2 = new FilesystemResourceLocator(f1, f2);
        assertEquals(2, l2.roots.length);
        assertEquals(base, l2.roots[0]);
        assertEquals(new File(base, "f2"), l2.roots[1]);
    }

    public void testCollectionConstructor() {
        FilesystemResourceLocator l1 = new FilesystemResourceLocator(Collections
                .<File> emptyList());
        assertEquals(1, l1.roots.length);
        assertEquals(System.getProperty("user.dir"), l1.roots[0].getPath());

        File base = FileUtil.baseTestDirectory();
        File f1 = new File(base, "f1");
        File f2 = new File(new File(base, "f2"), "f3");

        FilesystemResourceLocator l2 = new FilesystemResourceLocator(Arrays
                .asList(f1, f2));
        assertEquals(2, l2.roots.length);
        assertEquals(base, l2.roots[0]);
        assertEquals(new File(base, "f2"), l2.roots[1]);
    }

    public void testFindResources() throws Exception {

        File base = new File(FileUtil.baseTestDirectory(), getClass().getName());
        File root1 = new File(base, "r1");
        File root2 = new File(base, "r2");

        root1.mkdirs();
        root2.mkdirs();

        FilesystemResourceLocator locator = new FilesystemResourceLocator(root1, root2);
        Collection<Resource> resources1 = locator.findResources("x.txt");
        assertNotNull(resources1);
        assertEquals(0, resources1.size());

        File f1 = new File(root1, "x.txt");
        touch(f1);

        Collection<Resource> resources2 = locator.findResources("x.txt");
        assertNotNull(resources2);
        assertEquals(1, resources2.size());
        assertEquals(f1.toURL(), resources2.iterator().next().getURL());

        File f2 = new File(root2, "x.txt");
        touch(f2);

        Collection<Resource> resources3 = locator.findResources("x.txt");
        assertNotNull(resources3);
        assertEquals(2, resources3.size());
        
        Resource[] resources3a = resources3.toArray(new Resource[2]);
        assertEquals(f1.toURL(), resources3a[0].getURL());
        assertEquals(f2.toURL(), resources3a[1].getURL());
    }

    private void touch(File f) throws Exception {
        FileOutputStream out = new FileOutputStream(f);
        out.write('a');
        out.close();
    }
}
