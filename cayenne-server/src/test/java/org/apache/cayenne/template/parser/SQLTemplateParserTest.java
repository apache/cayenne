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

package org.apache.cayenne.template.parser;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.template.Context;
import org.apache.cayenne.template.DefaultTemplateContextFactory;
import org.apache.cayenne.template.TemplateContextFactory;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @since 4.1
 */
public class SQLTemplateParserTest {

    private TemplateContextFactory contextFactory;

    @Before
    public void setUp() {
        contextFactory = new DefaultTemplateContextFactory();
    }

    @Test
    public void testUnchangedParse() throws Exception {
        Context context = contextFactory.createContext(Collections.emptyMap());
        String template = "SELECT * FROM a";

        String sql = parseString(template, context);
        assertEquals(template, sql);
    }

    @Test
    public void testParameterParse() throws Exception {
        Context context = contextFactory.createContext(Collections.singletonMap("a", true));
        String template = "SELECT $a FROM a";

        String sql = parseString(template, context);
        assertEquals("SELECT true FROM a", sql);
    }

    @Test
    public void testIfElseParse() throws Exception {
        Context context = contextFactory.createContext(Collections.singletonMap("a", true));
        String template = "SELECT #if($a) * #else 1 #end FROM a";

        String sql = parseString(template, context);
        assertEquals("SELECT  *  FROM a", sql);

        context = contextFactory.createContext(Collections.singletonMap("a", false));
        template = "SELECT #if($a) * #else 1 #end FROM a";

        sql = parseString(template, context);
        assertEquals("SELECT  1  FROM a", sql);
    }

    @Test
    public void testBindParse() throws Exception {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("a", "var");
        parameters.put("b", "bbb");

        Context context = contextFactory.createContext(parameters);
        String template = "SELECT #if($a) #bind($a, 'INT' ,2) #else #bind($b, 'CHAR' ,2) #end FROM a";

        String sql = parseString(template, context);
        assertEquals("SELECT  ?  FROM a", sql);
        assertEquals(1, context.getParameterBindings().length);
        assertEquals("var", context.getParameterBindings()[0].getValue());
    }


    @Test
    public void testComplexParse() throws Exception {
        class Helper {
            public String cayenneExp(Object obj, String exp) {
                return "aaaa";
            }
        }
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("a", "var");
        parameters.put("myHelper", new Helper());

        String template = "SELECT * \n" +
                "FROM ME\n" +
                "#if($a) \n" +
                "WHERE \n" +
                "COLUMN1 #bind($myHelper.cayenneExp($a, 'db:ID_COLUMN1'), 'INT')\n" +
                "     \tAND \n" +
                "COLUMN2 #bind($myHelper.cayenneExp($a, 'db:ID_COLUMN2'), 'VARCHAR')\n" +
                "#end\n";
        Context context = contextFactory.createContext(parameters);

        String sql = parseString(template, context);
        assertEquals("SELECT * \n" +
                "FROM ME\n" +
                " \n" +
                "WHERE \n" +
                "COLUMN1 ?\n" +
                "     \tAND \n" +
                "COLUMN2 ?\n\n", sql);
        assertEquals(2, context.getParameterBindings().length);
        assertEquals("aaaa", context.getParameterBindings()[0].getValue());
    }

    @Test
    public void testComplexParse2() throws Exception {
        String tpl = "SELECT " +
                "#result('t0.BIGDECIMAL_FIELD' 'java.math.BigDecimal' 'ec0_0' 'ec0_0' 2), " +
                "#result('t0.ID' 'java.lang.Integer' 'ec0_1' 'ec0_1' 4) " +
                "FROM BIGDECIMAL_ENTITY t0 WHERE {fn ABS( t0.BIGDECIMAL_FIELD)} < #bind($id0 'DECIMAL')";

        Context context = contextFactory.createContext(Collections.singletonMap("id0", 123));
        String sql = parseString(tpl, context);

        assertEquals("SELECT " +
                "t0.BIGDECIMAL_FIELD AS ec0_0, " +
                "t0.ID AS ec0_1 " +
                "FROM BIGDECIMAL_ENTITY t0 WHERE {fn ABS( t0.BIGDECIMAL_FIELD)} < ?", sql);
        assertEquals(1, context.getParameterBindings().length);
        assertEquals(2, context.getColumnDescriptors().length);
    }

    @Test
    public void testComplexParse3() throws Exception {
        String tpl = "SELECT " +
                "#result('COUNT(*)' 'java.lang.Long' 'sc0'), " +
                "#result('t0.ARTIST_NAME' 'java.lang.String' 'ec1_0' 'ec1_0' 1), " +
                "#result('t0.DATE_OF_BIRTH' 'java.util.Date' 'ec1_1' 'ec1_1' 91), " +
                "#result('t0.ARTIST_ID' 'java.lang.Long' 'ec1_2' 'ec1_2' -5), " +
                "#result('SUM(t1.ESTIMATED_PRICE)' 'java.math.BigDecimal' 'sc2') " +
                "FROM ARTIST t0 " +
                "LEFT OUTER JOIN PAINTING t1 ON (t0.ARTIST_ID = t1.ARTIST_ID) " +
                "GROUP BY t0.ARTIST_NAME, t0.DATE_OF_BIRTH, t0.ARTIST_ID ORDER BY t0.ARTIST_NAME";
        Context context = contextFactory.createContext(Collections.emptyMap());
        String sql = parseString(tpl, context);
        assertEquals(5, context.getColumnDescriptors().length);
        assertEquals("SELECT COUNT(*) AS sc0, t0.ARTIST_NAME AS ec1_0, " +
                "t0.DATE_OF_BIRTH AS ec1_1, t0.ARTIST_ID AS ec1_2, SUM(t1.ESTIMATED_PRICE) AS sc2 " +
                "FROM ARTIST t0 " +
                "LEFT OUTER JOIN PAINTING t1 ON (t0.ARTIST_ID = t1.ARTIST_ID) " +
                "GROUP BY t0.ARTIST_NAME, t0.DATE_OF_BIRTH, t0.ARTIST_ID ORDER BY t0.ARTIST_NAME", sql);
    }

    @Test
    public void testHelperObject() throws Exception {
        String tpl = "($helper.cayenneExp($a, 'field'))";
        Context context = contextFactory.createContext(Collections.singletonMap("a", new TestBean(5)));

        String sql = parseString(tpl, context);
        assertEquals("(5)", sql);
    }

    @Test
    public void testMethodCallArray() throws Exception {
        String tpl = "$a.arrayMethod(['1' '2' '3'])";
        Context context = contextFactory.createContext(Collections.singletonMap("a", new TestBean(5)));

        String sql = parseString(tpl, context);
        assertEquals("array_3", sql);
    }

    @Test
    public void testMethodCallArray2() throws Exception {
        String tpl = "$a.arrayMethod(['1', '2', '3'])";
        Context context = contextFactory.createContext(Collections.singletonMap("a", new TestBean(5)));

        String sql = parseString(tpl, context);
        assertEquals("array_3", sql);
    }

    @Test
    public void testMethodCallInt() throws Exception {
        String tpl = "$a.intMethod(42)";
        Context context = contextFactory.createContext(Collections.singletonMap("a", new TestBean(5)));

        String sql = parseString(tpl, context);
        assertEquals("int_42", sql);
    }

    @Test
    public void testMethodCallString() throws Exception {
        String tpl = "$a.stringMethod(\"abc\")";
        Context context = contextFactory.createContext(Collections.singletonMap("a", new TestBean(5)));

        String sql = parseString(tpl, context);
        assertEquals("string_abc", sql);
    }

    @Test
    public void testMethodCallFloat() throws Exception {
        String tpl = "$a.floatMethod(3.14)";
        Context context = contextFactory.createContext(Collections.singletonMap("a", new TestBean(5)));

        String sql = parseString(tpl, context);
        assertEquals("float_3.14", sql);
    }

    @Test
    @Ignore("Method overload not properly supported, this test can return m2_true")
    public void testMethodCallSelectByArgType1() throws Exception {
        String tpl = "$a.method(123)";
        Context context = contextFactory.createContext(Collections.singletonMap("a", new TestBean(5)));

        String sql = parseString(tpl, context);
        assertEquals("m1_123", sql);
    }

    @Test
    public void testMethodCallSelectByArgType2() throws Exception {
        String tpl = "$a.method(true)";
        Context context = contextFactory.createContext(Collections.singletonMap("a", new TestBean(5)));

        String sql = parseString(tpl, context);
        assertEquals("m2_true", sql);
    }

    @Test
    public void testPropertyAccess() throws Exception {
        String tpl = "$a.field()";
        Context context = contextFactory.createContext(Collections.singletonMap("a", new TestBean(5)));

        String sql = parseString(tpl, context);
        assertEquals("5", sql);
    }

    @Test
    public void testNestedBrackets() throws Exception {
        String tpl = "(#bind('A' 'b'))";
        String sql = parseString(tpl, contextFactory.createContext(Collections.emptyMap()));
        assertEquals("(?)", sql);
    }

    @Test
    public void testQuotes() throws Exception {
        String template = "\"$a\"";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("a", "val");
        Context context = contextFactory.createContext(parameters);
        String sql = parseString(template, context);
        assertEquals("\"val\"", sql);

        context = contextFactory.createContext(parameters);
        template = "'$a'";
        sql = parseString(template, context);
        assertEquals("'val'", sql);
    }

    @Test
    public void testComma() throws Exception {
        String template = "$a,$a";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("a", "val");
        Context context = contextFactory.createContext(parameters);
        String sql = parseString(template, context);
        assertEquals("val,val", sql);
    }

    private String parseString(String tpl, Context context) throws ParseException {
        new SQLTemplateParser(new ByteArrayInputStream(tpl.getBytes())).template().evaluate(context);
        return context.buildTemplate();
    }

    static public class TestBean {
        private int field;
        TestBean(int field) {
            this.field = field;
        }

        public int getField() {
            return field;
        }

        public String arrayMethod(Object[] array) {
            return "array_" + array.length;
        }

        public String stringMethod(String string) {
            return "string_" + string;
        }

        public String intMethod(int i) {
            return "int_" + i;
        }

        public String floatMethod(float f) {
            return "float_" + f;
        }

        public String method(int i) {
            return "m1_" + i;
        }

        public String method(boolean b) {
            return "m2_" + b;
        }
    }

}