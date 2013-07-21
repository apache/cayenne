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

package org.apache.cayenne.access.jdbc;

import java.util.Collections;

import junit.framework.TestCase;

public class SQLTemplateProcessorSelectTest extends TestCase {

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
        assertEquals("B", column.getDataRowKey());
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
