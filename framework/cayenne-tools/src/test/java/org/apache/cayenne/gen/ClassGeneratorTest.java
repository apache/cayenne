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

package org.apache.cayenne.gen;

import junit.framework.TestCase;

import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;

/**
 * ClassGenerationInfo tests that are not template-specific.
 * 
 */
public class ClassGeneratorTest extends TestCase {

    protected ClassGenerationInfo cgen;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        cgen = new ClassGenerationInfo();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        cgen = null;
    }

    public void testFormatVariableName() {
        assertEquals("abc", cgen.formatVariableName("abc"));

        assertEquals("_abstract", cgen.formatVariableName("abstract"));
        assertEquals("_finally", cgen.formatVariableName("finally"));
    }

    public void testClassName() throws Exception {
        String className = "aaa";
        cgen.setClassName(className);
        assertEquals(className, cgen.getClassName());
    }

    public void testSuperPrefix() throws Exception {
        String prefix = "pr_";
        cgen.setSuperPrefix(prefix);
        assertEquals(prefix, cgen.getSuperPrefix());
    }

    public void testPackageName() throws Exception {
        assertFalse(cgen.isUsingPackage());
        String pkgName = "aaa.org";
        cgen.setPackageName(pkgName);
        assertEquals(pkgName, cgen.getPackageName());
        assertTrue(cgen.isUsingPackage());
    }

    public void testSuperClassName() throws Exception {
        cgen.setSuperClassName("super!!!");
        assertEquals("super!!!", cgen.getSuperClassName());
    }

    public void testContainingListProperties() throws Exception {
        cgen.entity = new ObjEntity("test");

        assertFalse(cgen.isContainingListProperties());

        ObjRelationship toOne = new ObjRelationship("toone");
        cgen.entity.addRelationship(toOne);
        assertFalse(toOne.isToMany());
        assertFalse(cgen.isContainingListProperties());

        ObjRelationship toMany = new ObjRelationship("tomany") {

            @Override
            public boolean isToMany() {
                return true;
            }
        };

        cgen.entity.addRelationship(toMany);
        assertTrue(toMany.isToMany());
        assertTrue(cgen.isContainingListProperties());
    }
}
