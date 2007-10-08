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

import java.sql.Types;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.objectstyle.cayenne.CayenneDataObject;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.unit.BasicTestCase;

/**
 * @author Andrei Adamchik
 */
public class SQLTemplateProcessorTst extends BasicTestCase {

    public void testProcessTemplateUnchanged1() throws Exception {
        String sqlTemplate = "SELECT * FROM ME";

        SQLStatement compiled = new SQLTemplateProcessor().processTemplate(
                sqlTemplate,
                Collections.EMPTY_MAP);

        assertEquals(sqlTemplate, compiled.getSql());
        assertEquals(0, compiled.getBindings().length);
    }

    public void testProcessTemplateUnchanged2() throws Exception {
        String sqlTemplate = "SELECT a.b as XYZ FROM $SYSTEM_TABLE";

        SQLStatement compiled = new SQLTemplateProcessor().processTemplate(
                sqlTemplate,
                Collections.EMPTY_MAP);

        assertEquals(sqlTemplate, compiled.getSql());
        assertEquals(0, compiled.getBindings().length);
    }

    public void testProcessTemplateSimpleDynamicContent() throws Exception {
        String sqlTemplate = "SELECT * FROM ME WHERE $a";

        Map map = Collections.singletonMap("a", "VALUE_OF_A");
        SQLStatement compiled = new SQLTemplateProcessor().processTemplate(
                sqlTemplate,
                map);

        assertEquals("SELECT * FROM ME WHERE VALUE_OF_A", compiled.getSql());

        // bindings are not populated, since no "bind" macro is used.
        assertEquals(0, compiled.getBindings().length);
    }

    public void testProcessTemplateBind() throws Exception {
        String sqlTemplate = "SELECT * FROM ME WHERE "
                + "COLUMN1 = #bind($a 'VARCHAR') AND COLUMN2 = #bind($b 'INTEGER')";
        Map map = Collections.singletonMap("a", "VALUE_OF_A");
        SQLStatement compiled = new SQLTemplateProcessor().processTemplate(
                sqlTemplate,
                map);

        assertEquals("SELECT * FROM ME WHERE COLUMN1 = ? AND COLUMN2 = ?", compiled
                .getSql());
        assertEquals(2, compiled.getBindings().length);
        assertBindingValue("VALUE_OF_A", compiled.getBindings()[0]);
        assertBindingValue(null, compiled.getBindings()[1]);
    }

    public void testProcessTemplateBindGuessVarchar() throws Exception {
        String sqlTemplate = "SELECT * FROM ME WHERE COLUMN1 = #bind($a)";
        Map map = Collections.singletonMap("a", "VALUE_OF_A");

        SQLStatement compiled = new SQLTemplateProcessor().processTemplate(
                sqlTemplate,
                map);

        assertEquals(1, compiled.getBindings().length);
        assertBindingType(Types.VARCHAR, compiled.getBindings()[0]);
    }

    public void testProcessTemplateBindGuessInteger() throws Exception {
        String sqlTemplate = "SELECT * FROM ME WHERE COLUMN1 = #bind($a)";
        Map map = Collections.singletonMap("a", new Integer(4));

        SQLStatement compiled = new SQLTemplateProcessor().processTemplate(
                sqlTemplate,
                map);

        assertEquals(1, compiled.getBindings().length);
        assertBindingType(Types.INTEGER, compiled.getBindings()[0]);
    }

    public void testProcessTemplateBindEqual() throws Exception {
        String sqlTemplate = "SELECT * FROM ME WHERE COLUMN #bindEqual($a 'VARCHAR')";

        SQLStatement compiled = new SQLTemplateProcessor().processTemplate(
                sqlTemplate,
                Collections.EMPTY_MAP);

        assertEquals("SELECT * FROM ME WHERE COLUMN IS NULL", compiled.getSql());
        assertEquals(0, compiled.getBindings().length);

        Map map = Collections.singletonMap("a", "VALUE_OF_A");

        compiled = new SQLTemplateProcessor().processTemplate(sqlTemplate, map);

        assertEquals("SELECT * FROM ME WHERE COLUMN = ?", compiled.getSql());
        assertEquals(1, compiled.getBindings().length);
        assertBindingValue("VALUE_OF_A", compiled.getBindings()[0]);
    }

    public void testProcessTemplateBindNotEqual() throws Exception {
        String sqlTemplate = "SELECT * FROM ME WHERE COLUMN #bindNotEqual($a 'VARCHAR')";

        SQLStatement compiled = new SQLTemplateProcessor().processTemplate(
                sqlTemplate,
                Collections.EMPTY_MAP);

        assertEquals("SELECT * FROM ME WHERE COLUMN IS NOT NULL", compiled.getSql());
        assertEquals(0, compiled.getBindings().length);

        Map map = Collections.singletonMap("a", "VALUE_OF_A");

        compiled = new SQLTemplateProcessor().processTemplate(sqlTemplate, map);

        assertEquals("SELECT * FROM ME WHERE COLUMN <> ?", compiled.getSql());
        assertEquals(1, compiled.getBindings().length);
        assertBindingValue("VALUE_OF_A", compiled.getBindings()[0]);
    }

    public void testProcessTemplateID() throws Exception {
        String sqlTemplate = "SELECT * FROM ME WHERE COLUMN1 = #bind($helper.cayenneExp($a, 'db:ID_COLUMN'))";

        DataObject dataObject = new CayenneDataObject();
        dataObject.setObjectId(new ObjectId("T", "ID_COLUMN", 5));

        Map map = Collections.singletonMap("a", dataObject);

        SQLStatement compiled = new SQLTemplateProcessor().processTemplate(
                sqlTemplate,
                map);

        assertEquals("SELECT * FROM ME WHERE COLUMN1 = ?", compiled.getSql());
        assertEquals(1, compiled.getBindings().length);
        assertBindingValue(new Integer(5), compiled.getBindings()[0]);
    }

    public void testProcessTemplateNotEqualID() throws Exception {
        String sqlTemplate = "SELECT * FROM ME WHERE "
                + "COLUMN1 #bindNotEqual($helper.cayenneExp($a, 'db:ID_COLUMN1')) "
                + "AND COLUMN2 #bindNotEqual($helper.cayenneExp($a, 'db:ID_COLUMN2'))";

        Map idMap = new HashMap();
        idMap.put("ID_COLUMN1", new Integer(3));
        idMap.put("ID_COLUMN2", "aaa");
        ObjectId id = new ObjectId("T", idMap);
        DataObject dataObject = new CayenneDataObject();
        dataObject.setObjectId(id);

        Map map = Collections.singletonMap("a", dataObject);

        SQLStatement compiled = new SQLTemplateProcessor().processTemplate(
                sqlTemplate,
                map);

        assertEquals("SELECT * FROM ME WHERE COLUMN1 <> ? AND COLUMN2 <> ?", compiled
                .getSql());
        assertEquals(2, compiled.getBindings().length);
        assertBindingValue(new Integer(3), compiled.getBindings()[0]);
        assertBindingValue("aaa", compiled.getBindings()[1]);
    }

    public void testProcessTemplateConditions() throws Exception {
        String sqlTemplate = "SELECT * FROM ME #if($a) WHERE COLUMN1 > #bind($a)#end";

        Map map = Collections.singletonMap("a", "VALUE_OF_A");

        SQLStatement compiled = new SQLTemplateProcessor().processTemplate(
                sqlTemplate,
                map);

        assertEquals("SELECT * FROM ME  WHERE COLUMN1 > ?", compiled.getSql());
        assertEquals(1, compiled.getBindings().length);
        assertBindingValue("VALUE_OF_A", compiled.getBindings()[0]);

        compiled = new SQLTemplateProcessor().processTemplate(
                sqlTemplate,
                Collections.EMPTY_MAP);

        assertEquals("SELECT * FROM ME ", compiled.getSql());
        assertEquals(0, compiled.getBindings().length);
    }

    public void testProcessTemplateBindCollection() throws Exception {
        String sqlTemplate = "SELECT * FROM ME WHERE COLUMN IN (#bind($list 'VARCHAR'))";

        Map map = new HashMap();
        map.put("list", Arrays.asList(new Object[] {
                "a", "b", "c"
        }));
        SQLStatement compiled = new SQLTemplateProcessor().processTemplate(
                sqlTemplate,
                map);

        assertEquals("SELECT * FROM ME WHERE COLUMN IN (?,?,?)", compiled.getSql());
        assertEquals(3, compiled.getBindings().length);

        compiled = new SQLTemplateProcessor().processTemplate(sqlTemplate, map);
        assertBindingValue("a", compiled.getBindings()[0]);
        assertBindingValue("b", compiled.getBindings()[1]);
        assertBindingValue("c", compiled.getBindings()[2]);
    }

    protected void assertBindingValue(Object expectedValue, Object binding) {
        assertTrue("Not a binding!", binding instanceof ParameterBinding);
        assertEquals(expectedValue, ((ParameterBinding) binding).getValue());
    }

    protected void assertBindingType(int expectedType, Object binding) {
        assertTrue("Not a binding!", binding instanceof ParameterBinding);
        assertEquals(expectedType, ((ParameterBinding) binding).getJdbcType());
    }

    protected void assertBindingPrecision(int expectedPrecision, Object binding) {
        assertTrue("Not a binding!", binding instanceof ParameterBinding);
        assertEquals(expectedPrecision, ((ParameterBinding) binding).getPrecision());
    }
}
