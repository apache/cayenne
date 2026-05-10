/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.gen;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ImportUtilsTest {

    protected ImportUtils importUtils = null;

    @BeforeEach
    public void setUp() throws Exception {
        importUtils = new ImportUtils();
    }

    @AfterEach
    public void tearDown() throws Exception {
        importUtils = null;
    }

    @Test
    public void setPackageGeneratesPackageStatement() throws Exception {
        final String packageName = "org.myPackage";
        final String expectedPackageStatement = "package " + packageName + ";";

        importUtils.setPackage(packageName);

        String generatedStatements = importUtils.generate();
        assertTrue(generatedStatements.startsWith(expectedPackageStatement),
                "<" + generatedStatements + "> does not start with <" + expectedPackageStatement + ">");
        assertEquals(generatedStatements.lastIndexOf(expectedPackageStatement),
                generatedStatements.lastIndexOf(expectedPackageStatement),
                "package statement appears multiple times.");
    }

    @Test
    public void addTypeGeneratesImportStatement() throws Exception {
        final String type = "org.myPackage.myType";
        final String expectedImportStatement = "import " + type + ";";

        importUtils.addType(type);

        String generatedStatements = importUtils.generate();
        assertFalse(!generatedStatements.contains(expectedImportStatement),
                "<" + generatedStatements + "> does not contain <" + expectedImportStatement + ">");
        assertEquals(generatedStatements.lastIndexOf(expectedImportStatement),
                generatedStatements.lastIndexOf(expectedImportStatement),
                "import statement appears multiple times.");
    }

    @Test
    public void addReservedTypeGeneratesNoImportStatement() throws Exception {
        final String type = "org.myPackage.myType";

        importUtils.addReservedType(type);

        String generatedStatements = importUtils.generate();
        assertEquals(-1, generatedStatements.indexOf(type),
                "<" + generatedStatements + "> contains <" + type + ">");
    }

    @Test
    public void addTypeAfterReservedTypeGeneratesNoImportStatement() throws Exception {
        final String baseType = "myType";
        final String reservedType = "org.myPackage." + baseType;
        final String nonReservedType = "org.myPackage2." + baseType;

        importUtils.addReservedType(reservedType);
        importUtils.addType(nonReservedType);

        String generatedStatements = importUtils.generate();
        assertEquals(-1, generatedStatements.indexOf(reservedType),
                "<" + generatedStatements + "> contains <" + reservedType + ">");
        assertEquals(-1, generatedStatements.indexOf(nonReservedType),
                "<" + generatedStatements + "> contains <" + nonReservedType + ">");
    }

    @Test
    public void addTypeAfterPackageReservedTypeGeneratesNoImportStatement()
            throws Exception {
        final String baseType = "myType";
        final String packageType = "org.myPackage";
        final String reservedType = packageType + "." + baseType;
        final String nonReservedType = "org.myPackage2." + baseType;

        importUtils.setPackage(packageType);
        importUtils.addReservedType(reservedType);
        importUtils.addType(nonReservedType);

        String generatedStatements = importUtils.generate();

        assertEquals(-1, generatedStatements.indexOf(reservedType),
                "<" + generatedStatements + "> contains <" + reservedType + ">");
        assertEquals(-1, generatedStatements.indexOf(nonReservedType),
                "<" + generatedStatements + "> contains <" + nonReservedType + ">");
    }

    @Test
    public void addTypeAfterTypeGeneratesNoImportStatement() throws Exception {
        final String baseType = "myType";
        final String firstType = "org.myPackage." + baseType;
        final String secondType = "org.myPackage2." + baseType;

        final String expectedImportStatement = "import " + firstType + ";";

        importUtils.addType(firstType);
        importUtils.addType(secondType);

        String generatedStatements = importUtils.generate();

        assertFalse(!generatedStatements.contains(expectedImportStatement),
                "<" + generatedStatements + "> does not contain <" + expectedImportStatement + ">");
        assertEquals(generatedStatements.lastIndexOf(expectedImportStatement),
                generatedStatements.lastIndexOf(expectedImportStatement),
                "import statement appears multiple times.");

        assertEquals(-1, generatedStatements.indexOf(secondType),
                "<" + generatedStatements + "> contains <" + secondType + ">");
    }

    @Test
    public void addSimilarTypeTwiceBeforeFormatJavaTypeGeneratesCorrectFQNs()
            throws Exception {
        final String baseType = "myType";
        final String firstType = "org.myPackage." + baseType;
        final String secondType = "org.myPackage2." + baseType;

        importUtils.addType(firstType);
        importUtils.addType(secondType);

        assertEquals(baseType, importUtils.formatJavaType(firstType));
        assertEquals(secondType, importUtils.formatJavaType(secondType));
    }

    @Test
    public void addTypeBeforeFormatJavaTypeGeneratesCorrectFQNs() throws Exception {
        final String baseType = "myType";
        final String fullyQualifiedType = "org.myPackage." + baseType;

        importUtils.addType(fullyQualifiedType);

        assertEquals(baseType, importUtils.formatJavaType(fullyQualifiedType));
    }

    @Test
    public void addReservedTypeBeforeFormatJavaTypeGeneratesCorrectFQNs()
            throws Exception {
        final String baseType = "myType";
        final String fullyQualifiedType = "org.myPackage." + baseType;

        importUtils.addReservedType(fullyQualifiedType);

        assertEquals(fullyQualifiedType, importUtils.formatJavaType(fullyQualifiedType));
    }

    @Test
    public void formatJavaTypeWithPrimitives() throws Exception {
        assertEquals("int", importUtils.formatJavaType("int", true));
        assertEquals("Integer", importUtils.formatJavaType("int", false));

        assertEquals("char", importUtils.formatJavaType("char", true));
        assertEquals("Character", importUtils
                .formatJavaType("java.lang.Character", false));

        assertEquals("double", importUtils.formatJavaType("java.lang.Double", true));
        assertEquals("Double", importUtils.formatJavaType("java.lang.Double", false));

        assertEquals("a.b.C", importUtils.formatJavaType("a.b.C", true));
        assertEquals("a.b.C", importUtils.formatJavaType("a.b.C", false));
    }

    @Test
    public void formatJavaTypeWithoutAddTypeGeneratesCorrectFQNs() throws Exception {
        final String baseType = "myType";
        final String fullyQualifiedType = "org.myPackage." + baseType;

        assertEquals(fullyQualifiedType, importUtils.formatJavaType(fullyQualifiedType));
    }

    @Test
    public void packageFormatJavaTypeWithoutAddTypeGeneratesCorrectFQNs()
            throws Exception {
        final String baseType = "myType";
        final String packageType = "org.myPackage";
        final String fullyQualifiedType = packageType + "." + baseType;

        importUtils.setPackage(packageType);

        assertEquals(baseType, importUtils.formatJavaType(fullyQualifiedType));
    }

    @Test
    public void formatJavaType() {
        assertEquals("x.X", importUtils.formatJavaType("x.X"));
        assertEquals("X", importUtils.formatJavaType("java.lang.X"));
        assertEquals("java.lang.x.X", importUtils.formatJavaType("java.lang.x.X"));
    }

    @Test
    public void javaLangTypeFormatJavaTypeWithoutAddTypeGeneratesCorrectFQNs()
            throws Exception {
        final String baseType = "myType";
        final String packageType = "java.lang";
        final String fullyQualifiedType = packageType + "." + baseType;

        assertEquals(baseType, importUtils.formatJavaType(fullyQualifiedType));
    }
}
