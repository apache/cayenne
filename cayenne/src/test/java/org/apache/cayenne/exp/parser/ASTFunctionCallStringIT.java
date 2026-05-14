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

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.dba.OracleUnitDbAdapter;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

public class ASTFunctionCallStringIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    private Artist createArtist(String name) throws Exception {
        Artist a1 = env.context().newObject(Artist.class);
        a1.setArtistName(name);
        a1.setDateOfBirth(new Date());
        env.context().commitChanges();
        return a1;
    }

    @Test
    public void aSTTrimInWhere() throws Exception {
        Artist a1 = createArtist("  name  ");
        Artist a2 = ObjectSelect.query(Artist.class)
                .where(Artist.ARTIST_NAME.trim().eq("name")).selectOne(env.context());
        assertEquals(a1, a2);
    }

    @Test
    public void aSTUpperInWhere() throws Exception {
        // TODO: This will fail for Oracle, so skip for now.
        //       It is necessary to provide connection with "fixedString=true" property somehow.
        //       Also see CAY-1470.
        assumeFalse(env.unitDbAdapter() instanceof OracleUnitDbAdapter);
        Artist a1 = createArtist("name");
        Artist a2 = ObjectSelect.query(Artist.class)
                .where(Artist.ARTIST_NAME.upper().eq("NAME")).selectOne(env.context());
        assertEquals(a1, a2);
    }

    @Test
    public void aSTLowerInWhere() throws Exception {
        // TODO: This will fail for Oracle, so skip for now.
        //       It is necessary to provide connection with "fixedString=true" property somehow.
        //       Also see CAY-1470.
        assumeFalse(env.unitDbAdapter() instanceof OracleUnitDbAdapter);
        Artist a1 = createArtist("NAME");
        Artist a2 = ObjectSelect.query(Artist.class)
                .where(Artist.ARTIST_NAME.lower().eq("name")).selectOne(env.context());
        assertEquals(a1, a2);
    }

    @Test
    public void aSTSubstringInWhere() throws Exception {
        Artist a1 = createArtist("1234567890xyz");
        Artist a2 = ObjectSelect.query(Artist.class)
                .where(Artist.ARTIST_NAME.substring(2, 8).eq("23456789")).selectOne(env.context());
        assertEquals(a1, a2);
    }

    @Test
    public void aSTConcat() throws Exception {
        Artist a1 = createArtist("Pablo");
        Artist a2 = ObjectSelect.query(Artist.class)
                .where(Artist.ARTIST_NAME.trim().concat(" ", "Picasso").eq("Pablo Picasso")).selectOne(env.context());
        assertEquals(a1, a2);
    }

    @Test
    public void aSTLength() throws Exception {
        Artist a1 = createArtist("123456");

        Artist a2 = ObjectSelect.query(Artist.class).where(Artist.ARTIST_NAME.length().gt(5)).selectOne(env.context());
        assertEquals(a1, a2);

        Artist a3 = ObjectSelect.query(Artist.class).where(Artist.ARTIST_NAME.length().lt(5)).selectOne(env.context());
        assertNull(a3);
    }

    @Test
    public void aSTLocate() throws Exception {
        Artist a1 = createArtist("1267834567890abc");
        Artist a2 = ObjectSelect.query(Artist.class)
                .where(Artist.ARTIST_NAME.locate("678").eq(3)).selectOne(env.context());
        assertEquals(a1, a2);
    }

    @Test
    public void combinedFunction() throws Exception {
        Artist a1 = createArtist("absdefghij  klmnopq"); // substring with length 10 from 3 is "sdefghij  "
        Artist a2 = ObjectSelect.query(Artist.class)
                .where(Artist.ARTIST_NAME.substring(3, 10).trim().upper().concat(" ", "test").eq("SDEFGHIJ test"))
                .selectOne(env.context());
        assertEquals(a1, a2);
    }

    @Test
    public void aSTConcatParse() {
        Expression exp = ExpressionFactory.exp("concat('abc', 'def')");
        assertEquals("abcdef", exp.evaluate(new Object()));
    }

    @Test
    public void aSTSubstringParse() {
        Expression exp = ExpressionFactory.exp("substring('123456789', 3, 2)");
        assertEquals("34", exp.evaluate(new Object()));
    }

    @Test
    public void aSTTrimParse() {
        Expression exp = ExpressionFactory.exp("trim(' abc ')");
        assertEquals("abc", exp.evaluate(new Object()));
    }

    @Test
    public void aSTLowerParse() {
        Expression exp = ExpressionFactory.exp("lower('AbC')");
        assertEquals("abc", exp.evaluate(new Object()));
    }

    @Test
    public void aSTUpperParse() {
        Expression exp = ExpressionFactory.exp("upper('aBc')");
        assertEquals("ABC", exp.evaluate(new Object()));
    }

    @Test
    public void aSTLocateParse() {
        Expression exp = ExpressionFactory.exp("locate('Bc', 'aBc')");
        assertEquals(2, exp.evaluate(new Object()));
    }

    @Test
    public void complexParse() {
        Expression exp = ExpressionFactory.exp("locate(upper('Bc'), upper('aBc')) = length(substring(trim(lower(concat('   abc', 'def   '))), 3, 2))");
        assertEquals(true, exp.evaluate(new Object()));
    }
}
