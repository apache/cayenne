/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1 ObjectStyle Group -
 * http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors of
 * the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any, must
 * include the following acknowlegement: "This product includes software
 * developed by independent contributors and hosted on ObjectStyle Group web
 * site (http://objectstyle.org/)." Alternately, this acknowlegement may appear
 * in the software itself, if and wherever such third-party acknowlegements
 * normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse or
 * promote products derived from this software without prior written permission.
 * For written permission, email "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle" or
 * "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their names without
 * prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * OBJECTSTYLE GROUP OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many individuals
 * and hosted on ObjectStyle Group web site. For more information on the
 * ObjectStyle Group, please see <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.gen;

import org.objectstyle.cayenne.map.ObjAttribute;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;

/**
 * @author Andrei Adamchik
 */
public class ClassGeneratorSingleTemplateTst extends ClassGeneratorTestBase {

    protected ClassGenerator createGenerator() throws Exception {
        return new ClassGenerator(MapClassGenerator.SINGLE_CLASS_TEMPLATE, MapClassGenerator.DEFAULT_VERSION);
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

        int classNameIndex = generated.indexOf("class JavaClass ");
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