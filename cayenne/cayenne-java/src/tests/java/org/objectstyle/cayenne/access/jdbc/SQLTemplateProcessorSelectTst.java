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
package org.objectstyle.cayenne.access.jdbc;

import java.util.Collections;

import org.objectstyle.cayenne.unit.BasicTestCase;

/**
 * @author Andrei Adamchik
 */
public class SQLTemplateProcessorSelectTst extends BasicTestCase {

    public void testProcessTemplateUnchanged() throws Exception {
        String sqlTemplate = "SELECT * FROM ME";

        SQLStatement compiled = new SQLTemplateProcessor().processTemplate(
                sqlTemplate,
                Collections.EMPTY_MAP);

        assertEquals(sqlTemplate, compiled.getSql());
        assertEquals(0, compiled.getBindings().length);
        assertEquals(0, compiled.getResultColumns().length);
    }

    public void testProcessSelectTemplate1() throws Exception {
        String sqlTemplate = "SELECT #result('A') FROM ME";

        SQLStatement compiled = new SQLTemplateProcessor().processTemplate(
                sqlTemplate,
                Collections.EMPTY_MAP);

        assertEquals("SELECT A FROM ME", compiled.getSql());
        assertEquals(0, compiled.getBindings().length);
        assertEquals(1, compiled.getResultColumns().length);
        assertEquals("A", compiled.getResultColumns()[0].getName());
        assertNull(compiled.getResultColumns()[0].getJavaClass());
    }

    public void testProcessSelectTemplate2() throws Exception {
        String sqlTemplate = "SELECT #result('A' 'String') FROM ME";

        SQLStatement compiled = new SQLTemplateProcessor().processTemplate(
                sqlTemplate,
                Collections.EMPTY_MAP);

        assertEquals("SELECT A FROM ME", compiled.getSql());
        assertEquals(0, compiled.getBindings().length);

        assertEquals(1, compiled.getResultColumns().length);
        assertEquals("A", compiled.getResultColumns()[0].getName());
        assertEquals("java.lang.String", compiled.getResultColumns()[0].getJavaClass());
    }

    public void testProcessSelectTemplate3() throws Exception {
        String sqlTemplate = "SELECT #result('A' 'String' 'B') FROM ME";

        SQLStatement compiled = new SQLTemplateProcessor().processTemplate(
                sqlTemplate,
                Collections.EMPTY_MAP);

        assertEquals("SELECT A AS B FROM ME", compiled.getSql());
        assertEquals(0, compiled.getBindings().length);

        assertEquals(1, compiled.getResultColumns().length);
        ColumnDescriptor column = compiled.getResultColumns()[0];
        assertEquals("A", column.getName());
        assertEquals("B", column.getLabel());
        assertEquals("java.lang.String", column.getJavaClass());
    }

    public void testProcessSelectTemplate4() throws Exception {
        String sqlTemplate = "SELECT #result('A'), #result('B'), #result('C') FROM ME";

        SQLStatement compiled = new SQLTemplateProcessor().processTemplate(
                sqlTemplate,
                Collections.EMPTY_MAP);

        assertEquals("SELECT A, B, C FROM ME", compiled.getSql());
        assertEquals(0, compiled.getBindings().length);

        assertEquals(3, compiled.getResultColumns().length);
        assertEquals("A", compiled.getResultColumns()[0].getName());
        assertEquals("B", compiled.getResultColumns()[1].getName());
        assertEquals("C", compiled.getResultColumns()[2].getName());
    }
}
