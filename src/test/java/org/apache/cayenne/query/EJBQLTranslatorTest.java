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
package org.apache.cayenne.query;

import org.apache.cayenne.ejbql.EJBQLCompiledExpression;
import org.apache.cayenne.ejbql.EJBQLParser;
import org.apache.cayenne.ejbql.EJBQLParserFactory;
import org.apache.cayenne.unit.CayenneCase;

public class EJBQLTranslatorTest extends CayenneCase {

    public void testSelectFrom() {
        EJBQLParser parser = EJBQLParserFactory.getParser();
        EJBQLCompiledExpression select = parser.compile(
                "select a from Artist a",
                getDomain().getEntityResolver());

        EJBQLTranslator tr = new EJBQLTranslator(select);
        String sql = tr.translate();

        // System.out.println("Expression: " + select.getExpression());
        // System.out.println("SQL: " + sql);

        // column order is unpredictable, just need to ensure that they are all there
        assertTrue(sql, sql.startsWith("SELECT "));
        assertTrue(sql, sql.indexOf("t0.ARTIST_ID") > 0);
        assertTrue(sql, sql.indexOf("t0.ARTIST_NAME") > 0);
        assertTrue(sql, sql.indexOf("t0.DATE_OF_BIRTH") > 0);
        assertTrue(sql, sql.endsWith(" FROM ARTIST t0"));
    }

    public void testSelectDistinct() {
        EJBQLParser parser = EJBQLParserFactory.getParser();
        EJBQLCompiledExpression select = parser.compile(
                "select distinct a from Artist a",
                getDomain().getEntityResolver());

        EJBQLTranslator tr = new EJBQLTranslator(select);
        String sql = tr.translate();

        // System.out.println("Expression: " + select.getExpression());
        // System.out.println("SQL: " + sql);

        assertTrue(sql, sql.startsWith("SELECT DISTINCT "));
        assertTrue(sql, sql.endsWith(" FROM ARTIST t0"));
    }

    public void testSelectFromWhereEqual() {
        EJBQLParser parser = EJBQLParserFactory.getParser();
        EJBQLCompiledExpression select = parser.compile(
                "select a from Artist a where a.artistName = 'Dali'",
                getDomain().getEntityResolver());

        EJBQLTranslator tr = new EJBQLTranslator(select);
        String sql = tr.translate();

        // System.out.println("Expression: " + select.getExpression());
        // System.out.println("SQL: " + sql);

        assertTrue(sql, sql.startsWith("SELECT "));
        // assertTrue(sql, sql.endsWith(" FROM ARTIST t0 WHERE t0.ARTIST_NAME = ?"));
    }

    public void testSelectFromWhereOrEqual() {
        EJBQLParser parser = EJBQLParserFactory.getParser();
        EJBQLCompiledExpression select = parser
                .compile(
                        "select a from Artist a where a.artistName = 'Dali' or a.artistName = 'Malevich'",
                        getDomain().getEntityResolver());

        EJBQLTranslator tr = new EJBQLTranslator(select);
        String sql = tr.translate();

        // System.out.println("Expression: " + select.getExpression());
        // System.out.println("SQL: " + sql);

        EJBQLCompiledExpression select1 = parser.compile(
                "select a from Artist a where a.artistName = 'Dali' "
                        + "or a.artistName = 'Malevich' "
                        + "or a.artistName = 'Picasso'",
                getDomain().getEntityResolver());

        EJBQLTranslator tr1 = new EJBQLTranslator(select);
        String sql1 = tr1.translate();

        System.out.println("Expression: " + select1.getExpression());
        System.out.println("SQL: " + sql1);

        assertTrue(sql, sql.startsWith("SELECT "));
        assertTrue(sql, sql.indexOf(" FROM ARTIST t0 WHERE ") > 0);

        assertTrue(sql1, sql1.startsWith("SELECT "));
        assertTrue(sql1, sql1.indexOf(" FROM ARTIST t0 WHERE ") > 0);
    }
}
