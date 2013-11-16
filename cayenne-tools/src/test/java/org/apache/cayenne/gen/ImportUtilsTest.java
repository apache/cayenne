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

public class ImportUtilsTest extends TestCase {

    protected ImportUtils importUtils = null;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        importUtils = new ImportUtils();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        importUtils = null;
    }

    public void testSetPackageGeneratesPackageStatement() throws Exception {
        final String packageName = "org.myPackage";
        final String expectedPackageStatement = "package " + packageName + ";";

        importUtils.setPackage(packageName);

        String generatedStatements = importUtils.generate();
        assertTrue("<"
                + generatedStatements
                + "> does not start with <"
                + expectedPackageStatement
                + ">", generatedStatements.startsWith(expectedPackageStatement));
        assertEquals("package statement appears multiple times.", generatedStatements
                .lastIndexOf(expectedPackageStatement), generatedStatements
                .lastIndexOf(expectedPackageStatement));
    }

    public void testAddTypeGeneratesImportStatement() throws Exception {
        final String type = "org.myPackage.myType";
        final String expectedImportStatement = "import " + type + ";";

        importUtils.addType(type);

        String generatedStatements = importUtils.generate();
        assertFalse("<"
                + generatedStatements
                + "> does not contain <"
                + expectedImportStatement
                + ">", !generatedStatements.contains(expectedImportStatement));
        assertEquals("import statement appears multiple times.", generatedStatements
                .lastIndexOf(expectedImportStatement), generatedStatements
                .lastIndexOf(expectedImportStatement));
    }

    public void testAddReservedTypeGeneratesNoImportStatement() throws Exception {
        final String type = "org.myPackage.myType";

        importUtils.addReservedType(type);

        String generatedStatements = importUtils.generate();
        assertEquals(
                "<" + generatedStatements + "> contains <" + type + ">",
                -1,
                generatedStatements.indexOf(type));
    }

    public void testAddTypeAfterReservedTypeGeneratesNoImportStatement() throws Exception {
        final String baseType = "myType";
        final String reservedType = "org.myPackage." + baseType;
        final String nonReservedType = "org.myPackage2." + baseType;

        importUtils.addReservedType(reservedType);
        importUtils.addType(nonReservedType);

        String generatedStatements = importUtils.generate();
        assertEquals(
                "<" + generatedStatements + "> contains <" + reservedType + ">",
                -1,
                generatedStatements.indexOf(reservedType));
        assertEquals(
                "<" + generatedStatements + "> contains <" + nonReservedType + ">",
                -1,
                generatedStatements.indexOf(nonReservedType));
    }

    public void testAddTypeAfterPackageReservedTypeGeneratesNoImportStatement()
            throws Exception {
        final String baseType = "myType";
        final String packageType = "org.myPackage";
        final String reservedType = packageType + "." + baseType;
        final String nonReservedType = "org.myPackage2." + baseType;

        importUtils.setPackage(packageType);
        importUtils.addReservedType(reservedType);
        importUtils.addType(nonReservedType);

        String generatedStatements = importUtils.generate();

        assertEquals(
                "<" + generatedStatements + "> contains <" + reservedType + ">",
                -1,
                generatedStatements.indexOf(reservedType));
        assertEquals(
                "<" + generatedStatements + "> contains <" + nonReservedType + ">",
                -1,
                generatedStatements.indexOf(nonReservedType));
    }

    public void testAddTypeAfterTypeGeneratesNoImportStatement() throws Exception {
        final String baseType = "myType";
        final String firstType = "org.myPackage." + baseType;
        final String secondType = "org.myPackage2." + baseType;

        final String expectedImportStatement = "import " + firstType + ";";

        importUtils.addType(firstType);
        importUtils.addType(secondType);

        String generatedStatements = importUtils.generate();

        assertFalse("<"
                + generatedStatements
                + "> does not contain <"
                + expectedImportStatement
                + ">", !generatedStatements.contains(expectedImportStatement));
        assertEquals("import statement appears multiple times.", generatedStatements
                .lastIndexOf(expectedImportStatement), generatedStatements
                .lastIndexOf(expectedImportStatement));

        assertEquals(
                "<" + generatedStatements + "> contains <" + secondType + ">",
                -1,
                generatedStatements.indexOf(secondType));
    }

    public void testAddSimilarTypeTwiceBeforeFormatJavaTypeGeneratesCorrectFQNs()
            throws Exception {
        final String baseType = "myType";
        final String firstType = "org.myPackage." + baseType;
        final String secondType = "org.myPackage2." + baseType;

        importUtils.addType(firstType);
        importUtils.addType(secondType);

        assertEquals(baseType, importUtils.formatJavaType(firstType));
        assertEquals(secondType, importUtils.formatJavaType(secondType));
    }

    public void testAddTypeBeforeFormatJavaTypeGeneratesCorrectFQNs() throws Exception {
        final String baseType = "myType";
        final String fullyQualifiedType = "org.myPackage." + baseType;

        importUtils.addType(fullyQualifiedType);

        assertEquals(baseType, importUtils.formatJavaType(fullyQualifiedType));
    }

    public void testAddReservedTypeBeforeFormatJavaTypeGeneratesCorrectFQNs()
            throws Exception {
        final String baseType = "myType";
        final String fullyQualifiedType = "org.myPackage." + baseType;

        importUtils.addReservedType(fullyQualifiedType);

        assertEquals(fullyQualifiedType, importUtils.formatJavaType(fullyQualifiedType));
    }

    public void testFormatJavaTypeWithPrimitives() throws Exception {
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

    public void testFormatJavaTypeWithoutAddTypeGeneratesCorrectFQNs() throws Exception {
        final String baseType = "myType";
        final String fullyQualifiedType = "org.myPackage." + baseType;

        assertEquals(fullyQualifiedType, importUtils.formatJavaType(fullyQualifiedType));
    }

    public void testPackageFormatJavaTypeWithoutAddTypeGeneratesCorrectFQNs()
            throws Exception {
        final String baseType = "myType";
        final String packageType = "org.myPackage";
        final String fullyQualifiedType = packageType + "." + baseType;

        importUtils.setPackage(packageType);

        assertEquals(baseType, importUtils.formatJavaType(fullyQualifiedType));
    }

    public void testFormatJavaType() {
        assertEquals("x.X", importUtils.formatJavaType("x.X"));
        assertEquals("X", importUtils.formatJavaType("java.lang.X"));
        assertEquals("java.lang.x.X", importUtils.formatJavaType("java.lang.x.X"));
    }

    public void testJavaLangTypeFormatJavaTypeWithoutAddTypeGeneratesCorrectFQNs()
            throws Exception {
        final String baseType = "myType";
        final String packageType = "java.lang";
        final String fullyQualifiedType = packageType + "." + baseType;

        assertEquals(baseType, importUtils.formatJavaType(fullyQualifiedType));
    }
}
