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
package org.objectstyle.cayenne.gen;

import junit.framework.TestCase;

/**
 * @author Mike Kienenberger
 */
public class ImportUtilsTst extends TestCase {

    protected ImportUtils importUtils = null;

    protected void setUp() throws Exception {
        super.setUp();
        importUtils = new ImportUtils();
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
                + ">", -1 == generatedStatements.indexOf(expectedImportStatement));
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
                + ">", -1 == generatedStatements.indexOf(expectedImportStatement));
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

    public void testJavaLangTypeFormatJavaTypeWithoutAddTypeGeneratesCorrectFQNs()
            throws Exception {
        final String baseType = "myType";
        final String packageType = "java.lang";
        final String fullyQualifiedType = packageType + "." + baseType;

        assertEquals(baseType, importUtils.formatJavaType(fullyQualifiedType));
    }
}
