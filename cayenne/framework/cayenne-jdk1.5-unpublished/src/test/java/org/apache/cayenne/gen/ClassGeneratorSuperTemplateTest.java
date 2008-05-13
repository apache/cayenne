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

import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;

/**
 * @deprecated since 3.0
 */
public class ClassGeneratorSuperTemplateTest extends ClassGeneratorTestBase {

    @Override
    protected ClassGenerator createGenerator() throws Exception {
        return new ClassGenerator(MapClassGenerator.SUPERCLASS_TEMPLATE, ClassGenerator.VERSION_1_1);
    }

    public void testNoImports() throws Exception {
        // setup fixture
        ObjEntity testEntity = new ObjEntity("Test");
        cgen.getClassGenerationInfo().setClassName("JavaClass");
        cgen.getClassGenerationInfo().setSuperPrefix("*");
        cgen.getClassGenerationInfo().setSuperClassName("123.567.Super");

        String generated = generatedString(testEntity);
        assertNotNull(generated);
        assertTrue(generated.length() > 0);

        assertFalse(generated.indexOf("import ") > 0);
    }

    public void testImports() throws Exception {
        // setup fixture
        ObjEntity testEntity = new ObjEntity("Test");
        ObjRelationship toMany = new ObjRelationship("tomany") {

            @Override
            public boolean isToMany() {
                return true;
            }
        };

        testEntity.addRelationship(toMany);
        cgen.getClassGenerationInfo().setClassName("JavaClass");
        cgen.getClassGenerationInfo().setSuperPrefix("*");
        cgen.getClassGenerationInfo().setSuperClassName("123.567.Super");

        String generated = generatedString(testEntity);
        assertNotNull(generated);
        assertTrue(generated.length() > 0);

        assertFalse(generated.indexOf("import java.util.List;") > 0);
    }

    public void testClassStructure() throws Exception {
        // setup fixture
        ObjEntity testEntity = new ObjEntity("Test");
        cgen.getClassGenerationInfo().setClassName("JavaClass");
        cgen.getClassGenerationInfo().setSuperPrefix("*");
        cgen.getClassGenerationInfo().setSuperClassName("123.567.Super");

        String generated = generatedString(testEntity);
        assertNotNull(generated);
        assertTrue(generated.length() > 0);

        int classNameIndex = generated.indexOf("class *JavaClass ");
        assertTrue(classNameIndex > 0);

        int superClassIndex = generated.indexOf("extends 123.567.Super");
        assertTrue(classNameIndex < superClassIndex);
    }

    public void testAttributes() throws Exception {
        // setup fixture
        ObjEntity testEntity = new ObjEntity("Test");

        ObjAttribute a1 = new ObjAttribute("a1");
        a1.setType("dummy1");
        testEntity.addAttribute(a1);

        ObjAttribute a2 = new ObjAttribute("a2");
        a2.setType("dummy2");
        testEntity.addAttribute(a2);

        cgen.getClassGenerationInfo().setClassName("C");
        cgen.getClassGenerationInfo().setSuperPrefix("*");
        cgen.getClassGenerationInfo().setSuperClassName("SC");

        String generated = generatedString(testEntity);
        assertNotNull(generated);
        assertTrue(generated.length() > 0);

        assertTrue(generated.indexOf("setA1(dummy1 a1)") > 0);
        assertTrue(generated.indexOf("dummy1 getA1()") > 0);

        assertTrue(generated.indexOf("setA2(dummy2 a2)") > 0);
        assertTrue(generated.indexOf("dummy2 getA2()") > 0);
    }

    public void testReservedName() throws Exception {
        // setup fixture
        ObjEntity testEntity = new ObjEntity("Test");
        ObjAttribute attribute = new ObjAttribute("abstract");
        attribute.setType("dummy");
        testEntity.addAttribute(attribute);
        cgen.getClassGenerationInfo().setClassName("C");
        cgen.getClassGenerationInfo().setSuperPrefix("*");
        cgen.getClassGenerationInfo().setSuperClassName("SC");

        String generated = generatedString(testEntity);
        assertNotNull(generated);
        assertTrue(generated.length() > 0);

        assertFalse(generated.indexOf("setAbstract(dummy abstract)") > 0);
        assertTrue("No 'abstract' property found: " + generated, generated
                .indexOf("setAbstract(dummy _abstract)") > 0);
    }
}
