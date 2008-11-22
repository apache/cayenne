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

package org.apache.cayenne.project;

import org.apache.cayenne.unit.CayenneCase;

/**
 */
public class ProjectPathTest extends CayenneCase {

    public void testConstructor() throws Exception {
        Object[] path = new Object[0];
        ProjectPath pp = new ProjectPath(path);
        assertSame(path, pp.getPath());
    }

    public void testGetObject1() throws Exception {
        Object[] path = new Object[] { new Object(), new Object()};
        ProjectPath p = new ProjectPath(path);
        assertSame(path[1], p.getObject());
    }

    public void testGetObject2() throws Exception {
        Object[] path = new Object[] { new Object()};
        ProjectPath p = new ProjectPath(path);
        assertSame(path[0], p.getObject());
    }

    public void testGetObject3() throws Exception {
        Object[] path = new Object[] {};
        ProjectPath p = new ProjectPath(path);
        assertNull(p.getObject());
    }

    public void testAppendToPath1() throws Exception {
        ProjectPath path = new ProjectPath();
        Object obj1 = new Object();
        path = path.appendToPath(obj1);

        Object[] p = path.getPath();
        assertNotNull(p);
        assertEquals(1, p.length);
        assertSame(obj1, p[0]);
    }

    public void testAppendToPath2() throws Exception {
        ProjectPath path = new ProjectPath();
        path = path.appendToPath(new Object());
        path = path.appendToPath(new Object());

        Object obj1 = new Object();
        path = path.appendToPath(obj1);

        Object[] p = path.getPath();
        assertNotNull(p);
        assertEquals(3, p.length);
        assertSame(obj1, p[2]);
    }

    public void testGetObjectParent1() throws Exception {
        Object[] path = new Object[] { new Object(), new Object()};
        assertSame(path[0], new ProjectPath(path).getObjectParent());
    }

    public void testGetObjectParent2() throws Exception {
        Object[] path = new Object[] { new Object()};
        assertNull(new ProjectPath(path).getObjectParent());
    }

    public void testFirstInstanceOf1() throws Exception {
        ProjectPath path = new ProjectPath(new Object());
        assertNull(path.firstInstanceOf(String.class));
    }

    public void testFirstInstanceOf2() throws Exception {
        String str = "sdsadsad";
        ProjectPath path = new ProjectPath(str);
        assertEquals(str, path.firstInstanceOf(String.class));
    }
}
