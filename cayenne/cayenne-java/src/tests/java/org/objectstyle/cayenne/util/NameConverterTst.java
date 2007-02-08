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

import junit.framework.TestCase;

public class NameConverterTst extends TestCase {

    public void testUnderscoredToJava1() throws Exception {
        String expected = "ClassNameIdentifier";
        assertEquals(
            expected,
            NameConverter.underscoredToJava("_CLASS_NAME_IDENTIFIER_", true));
    }

    public void testUnderscoredToJava2() throws Exception {
        String expected = "propNameIdentifier123";
        assertEquals(
            expected,
            NameConverter.underscoredToJava("_prop_name_Identifier_123", false));
    }

    public void testUnderscoredToJava3() throws Exception {
        String expected = "lastName";
        assertEquals(expected, NameConverter.underscoredToJava("lastName", false));
    }

    public void testUnderscoredToJava4() throws Exception {
        String expected = "lastName";
        assertEquals(expected, NameConverter.underscoredToJava("LastName", false));
    }

    public void testUnderscoredToJava5() throws Exception {
        String expected = "LastName";
        assertEquals(expected, NameConverter.underscoredToJava("LastName", true));
    }

    public void testJavaToUnderscored1() throws Exception {
        String expected = "LAST_NAME";
        assertEquals(expected, NameConverter.javaToUnderscored("LastName"));
    }

    public void testJavaToUnderscored2() throws Exception {
        String expected = "A_CLASS";
        assertEquals(expected, NameConverter.javaToUnderscored("aClass"));
    }

    public void testJavaToUnderscored3() throws Exception {
        String expected = "VAR_A";
        assertEquals(expected, NameConverter.javaToUnderscored("varA"));
    }

    public void testJavaToUnderscored4() throws Exception {
        String expected = "LAST_NAME";
        assertEquals(expected, NameConverter.javaToUnderscored("LAST_NAME"));
    }

    public void testJavaToUnderscored5() throws Exception {
        String expected = "ABC_A";
        assertEquals(expected, NameConverter.javaToUnderscored("abc_A"));
    }

    public void testJavaToUnderscored6() throws Exception {
        String expected = "A123";
        assertEquals(expected, NameConverter.javaToUnderscored("a123"));
    }

    public void testJavaToUnderscored7() throws Exception {
        String expected = "AB_CDEF";
        assertEquals(expected, NameConverter.javaToUnderscored("abCDEF"));
    }

    public void testJavaToUnderscored8() throws Exception {
        String expected = "AB_CE";
        assertEquals(expected, NameConverter.javaToUnderscored("abCe"));
    }
}
