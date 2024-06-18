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

package org.apache.cayenne.exp.parser;

import java.util.Date;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.OracleUnitDbAdapter;
import org.apache.cayenne.unit.UnitDbAdapter;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;
import static org.junit.Assume.assumeFalse;

/**
 * @since 4.0
 */
@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class ASTFunctionCallStringIT extends RuntimeCase {

    @Inject
    private UnitDbAdapter unitDbAdapter;

    @Inject
    private ObjectContext context;

    private Artist createArtist(String name) throws Exception {
        Artist a1 = context.newObject(Artist.class);
        a1.setArtistName(name);
        a1.setDateOfBirth(new Date());
        context.commitChanges();
        return a1;
    }

    @Test
    public void testASTTrimInWhere() throws Exception {
        Artist a1 = createArtist("  name  ");
        Artist a2 = ObjectSelect.query(Artist.class)
                .where(Artist.ARTIST_NAME.trim().eq("name")).selectOne(context);
        assertEquals(a1, a2);
    }

    @Test
    public void testASTUpperInWhere() throws Exception {
        // TODO: This will fail for Oracle, so skip for now.
        //       It is necessary to provide connection with "fixedString=true" property somehow.
        //       Also see CAY-1470.
        assumeFalse(unitDbAdapter instanceof OracleUnitDbAdapter);
        Artist a1 = createArtist("name");
        Artist a2 = ObjectSelect.query(Artist.class)
                .where(Artist.ARTIST_NAME.upper().eq("NAME")).selectOne(context);
        assertEquals(a1, a2);
    }

    @Test
    public void testASTLowerInWhere() throws Exception {
        // TODO: This will fail for Oracle, so skip for now.
        //       It is necessary to provide connection with "fixedString=true" property somehow.
        //       Also see CAY-1470.
        assumeFalse(unitDbAdapter instanceof OracleUnitDbAdapter);
        Artist a1 = createArtist("NAME");
        Artist a2 = ObjectSelect.query(Artist.class)
                .where(Artist.ARTIST_NAME.lower().eq("name")).selectOne(context);
        assertEquals(a1, a2);
    }

    @Test
    public void testASTSubstringInWhere() throws Exception {
        Artist a1 = createArtist("1234567890xyz");
        Artist a2 = ObjectSelect.query(Artist.class)
                .where(Artist.ARTIST_NAME.substring(2, 8).eq("23456789")).selectOne(context);
        assertEquals(a1, a2);
    }

    @Test
    public void testASTConcat() throws Exception {
        Artist a1 = createArtist("Pablo");
        Artist a2 = ObjectSelect.query(Artist.class)
                .where(Artist.ARTIST_NAME.trim().concat(" ", "Picasso").eq("Pablo Picasso")).selectOne(context);
        assertEquals(a1, a2);
    }

    @Test
    public void testASTLength() throws Exception {
        Artist a1 = createArtist("123456");

        Artist a2 = ObjectSelect.query(Artist.class).where(Artist.ARTIST_NAME.length().gt(5)).selectOne(context);
        assertEquals(a1, a2);

        Artist a3 = ObjectSelect.query(Artist.class).where(Artist.ARTIST_NAME.length().lt(5)).selectOne(context);
        assertNull(a3);
    }

    @Test
    public void testASTLocate() throws Exception {
        Artist a1 = createArtist("1267834567890abc");
        Artist a2 = ObjectSelect.query(Artist.class)
                .where(Artist.ARTIST_NAME.locate("678").eq(3)).selectOne(context);
        assertEquals(a1, a2);
    }

    @Test
    public void testCombinedFunction() throws Exception {
        Artist a1 = createArtist("absdefghij  klmnopq"); // substring with length 10 from 3 is "sdefghij  "
        Artist a2 = ObjectSelect.query(Artist.class)
                .where(Artist.ARTIST_NAME.substring(3, 10).trim().upper().concat(" ", "test").eq("SDEFGHIJ test"))
                .selectOne(context);
        assertEquals(a1, a2);
    }

    @Test
    public void testASTConcatParse() {
        Expression exp = ExpressionFactory.exp("concat('abc', 'def')");
        assertEquals("abcdef", exp.evaluate(new Object()));
    }

    @Test
    public void testASTSubstringParse() {
        Expression exp = ExpressionFactory.exp("substring('123456789', 3, 2)");
        assertEquals("34", exp.evaluate(new Object()));
    }

    @Test
    public void testASTTrimParse() {
        Expression exp = ExpressionFactory.exp("trim(' abc ')");
        assertEquals("abc", exp.evaluate(new Object()));
    }

    @Test
    public void testASTLowerParse() {
        Expression exp = ExpressionFactory.exp("lower('AbC')");
        assertEquals("abc", exp.evaluate(new Object()));
    }

    @Test
    public void testASTUpperParse() {
        Expression exp = ExpressionFactory.exp("upper('aBc')");
        assertEquals("ABC", exp.evaluate(new Object()));
    }

    @Test
    public void testASTLocateParse() {
        Expression exp = ExpressionFactory.exp("locate('Bc', 'aBc')");
        assertEquals(2, exp.evaluate(new Object()));
    }

    @Test
    public void testComplexParse() {
        Expression exp = ExpressionFactory.exp("locate(upper('Bc'), upper('aBc')) = length(substring(trim(lower(concat('   abc', 'def   '))), 3, 2))");
        assertEquals(true, exp.evaluate(new Object()));
    }
}
