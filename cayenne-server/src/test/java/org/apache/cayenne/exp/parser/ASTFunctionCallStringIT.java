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

package org.apache.cayenne.exp.parser;

import java.util.Date;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.Property;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

/**
 * @since 4.0
 */
@UseServerRuntime(CayenneProjects.TESTMAP_PROJECT)
public class ASTFunctionCallStringIT extends ServerCase {

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

        ASTTrim exp = new ASTTrim(Artist.ARTIST_NAME.path());
        Property<String> trimmedName = Property.create("trimmedName", exp, String.class);

        Artist a2 = ObjectSelect.query(Artist.class).where(trimmedName.eq("name")).selectOne(context);
        assertEquals(a1, a2);
    }

    @Test
    public void testASTUpperInWhere() throws Exception {
        Artist a1 = createArtist("name");

        ASTUpper exp = new ASTUpper(Artist.ARTIST_NAME.path());
        Property<String> upperName = Property.create("upperName", exp, String.class);

        Artist a2 = ObjectSelect.query(Artist.class).where(upperName.eq("NAME")).selectOne(context);
        assertEquals(a1, a2);
    }

    @Test
    public void testASTLowerInWhere() throws Exception {
        Artist a1 = createArtist("NAME");

        ASTLower exp = new ASTLower(Artist.ARTIST_NAME.path());
        Property<String> lowerName = Property.create("lowerName", exp, String.class);

        Artist a2 = ObjectSelect.query(Artist.class).where(lowerName.eq("name")).selectOne(context);
        assertEquals(a1, a2);
    }

    @Test
    public void testASTSubstringInWhere() throws Exception {
        Artist a1 = createArtist("1234567890xyz");

        ASTSubstring exp = new ASTSubstring(Artist.ARTIST_NAME.path(), new ASTScalar((Integer)2), new ASTScalar((Integer)8));
        Property<String> substrName = Property.create("substrName", exp, String.class);

        Artist a2 = ObjectSelect.query(Artist.class).where(substrName.eq("23456789")).selectOne(context);
        assertEquals(a1, a2);
    }

    @Test
    public void testASTConcat() throws Exception {
        Artist a1 = createArtist("Pablo");

        ASTScalar scalar1 = new ASTScalar(" ");
        ASTScalar scalar2 = new ASTScalar("Picasso");

        ASTConcat exp = new ASTConcat(Artist.ARTIST_NAME.path(), scalar1, scalar2);
        Property<String> concatName = Property.create("concatName", exp, String.class);

        Artist a2 = ObjectSelect.query(Artist.class).where(concatName.eq("Pablo Picasso")).selectOne(context);
        assertEquals(a1, a2);
    }

    @Test
    public void testASTLength() throws Exception {
        Artist a1 = createArtist("123456");

        ASTLength exp = new ASTLength(Artist.ARTIST_NAME.path());
        Property<Integer> nameLength = Property.create("nameLength", exp, Integer.class);

        Artist a2 = ObjectSelect.query(Artist.class).where(nameLength.gt(5)).selectOne(context);
        assertEquals(a1, a2);

        Artist a3 = ObjectSelect.query(Artist.class).where(nameLength.lt(5)).selectOne(context);
        assertEquals(null, a3);
    }

    @Test
    public void testASTLocate() throws Exception {
        Artist a1 = createArtist("1267834567890abc");

        ASTScalar substr = new ASTScalar("678");
//        ASTScalar offset = new ASTScalar((Integer)5); // not all DBs support offset parameter, so skip it
        ASTLocate exp = new ASTLocate(substr, Artist.ARTIST_NAME.path());
        Property<Integer> nameLoc = Property.create("nameLoc", exp, Integer.class);

        Artist a2 = ObjectSelect.query(Artist.class).where(nameLoc.eq(3)).selectOne(context);
        assertEquals(a1, a2);
    }

    @Test
    public void testCombinedFunction() throws Exception {
        Artist a1 = createArtist("absdefghij  klmnopq"); // substring with length 10 from 3 is "sdefghij  "

        ASTSubstring substring = new ASTSubstring(
                Artist.ARTIST_NAME.path(),
                new ASTScalar((Integer)3),
                new ASTScalar((Integer)10));
        ASTTrim trim = new ASTTrim(substring);
        ASTUpper upper = new ASTUpper(trim);
        ASTConcat concat = new ASTConcat(upper, new ASTScalar(" "), new ASTScalar("test"));

        Property<String> name = Property.create("substrName", concat, String.class);
        Artist a2 = ObjectSelect.query(Artist.class).where(name.eq("SDEFGHIJ test")).selectOne(context);
        assertEquals(a1, a2);
    }

    @Test
    public void testASTConcatParse() {
        Expression exp = ExpressionFactory.exp("CONCAT('abc', 'def')");
        assertEquals("abcdef", exp.evaluate(new Object()));
    }

    @Test
    public void testASTSubstringParse() {
        Expression exp = ExpressionFactory.exp("SUBSTRING('123456789', 3, 2)");
        assertEquals("45", exp.evaluate(new Object()));
    }

    @Test
    public void testASTTrimParse() {
        Expression exp = ExpressionFactory.exp("TRIM(' abc ')");
        assertEquals("abc", exp.evaluate(new Object()));
    }

    @Test
    public void testASTLowerParse() {
        Expression exp = ExpressionFactory.exp("LOWER('AbC')");
        assertEquals("abc", exp.evaluate(new Object()));
    }

    @Test
    public void testASTUpperParse() {
        Expression exp = ExpressionFactory.exp("UPPER('aBc')");
        assertEquals("ABC", exp.evaluate(new Object()));
    }

    @Test
    public void testASTLocateParse() {
        Expression exp = ExpressionFactory.exp("LOCATE('Bc', 'aBc')");
        assertEquals(2, exp.evaluate(new Object()));
    }

    @Test
    public void testComplexParse() {
        Expression exp = ExpressionFactory.exp("LOCATE(UPPER('Bc'), UPPER('aBc')) = LENGTH(SUBSTRING(TRIM(LOWER(CONCAT('   abc', 'def   '))), 3, 2))");
        assertEquals(true, exp.evaluate(new Object()));
    }
}
