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

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.property.PropertyFactory;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @since 4.0
 */
@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class ExpressionCollectionEvaluationIT extends RuntimeCase {

    @Inject
    private DataContext context;

    @Inject
    private DBHelper dbHelper;

    @Before
    public void createArtistsDataSet() throws Exception {
        TableHelper tArtist = new TableHelper(dbHelper, "ARTIST");
        tArtist.setColumns("ARTIST_ID", "ARTIST_NAME", "DATE_OF_BIRTH");
        tArtist.insert(1, "artist1", new java.sql.Date(System.currentTimeMillis()));

        TableHelper tGallery = new TableHelper(dbHelper, "GALLERY");
        tGallery.setColumns("GALLERY_ID", "GALLERY_NAME");
        tGallery.insert(1, "tate modern");

        TableHelper tPaintings = new TableHelper(dbHelper, "PAINTING");
        tPaintings.setColumns("PAINTING_ID", "PAINTING_TITLE", "ARTIST_ID", "GALLERY_ID", "ESTIMATED_PRICE");
        for (int i = 1; i <= 3; i++) {
            tPaintings.insert(i, i + "painting" + (Math.pow(10, i)), 1, 1, i * 100);
        }

        tPaintings.insert(4, "4painting", null, 1, 10000);
//        tPaintings.insert(5, "5painting", 1, 1, null);
    }

    @Test
    public void testSubstringWithCollection() {
        testExpression("substring(paintingArray.paintingTitle, 1, 1)", String.class);
    }

    @Test
    public void testTrimWithCollection() {
        testExpression("trim(paintingArray.paintingTitle)", String.class);
    }

    @Test
    public void testUpperWithCollection() {
        testExpression("upper(paintingArray.paintingTitle)", String.class);
    }

    @Test
    public void testLowerWithCollection() {
        testExpression("lower(paintingArray.paintingTitle)", String.class);
    }

    @Test
    public void testLengthWithCollection() {
        testExpression("length(paintingArray.paintingTitle)", Integer.class);
    }

    @Test
    public void testConcatWithCollection() {
        testExpression("concat(paintingArray.paintingTitle, ' ', 'xyz')", String.class);
    }

    @Test
    public void testMathWithCollection() {
        testExpression("paintingArray.estimatedPrice + 2", BigDecimal.class);
    }

    @SuppressWarnings("unchecked")
    private <T extends Comparable<T>> void testExpression(String expStr, Class<T> tClass) {
        Expression exp = ExpressionFactory.exp(expStr);
        Object res = exp.evaluate(ObjectSelect
                .query(Artist.class)
                .prefetch(Artist.PAINTING_ARRAY.disjoint())
                .selectOne(context));
        List<T> sqlResult = ObjectSelect.query(Artist.class)
                .column(PropertyFactory.createBase(exp, tClass))
                .orderBy("db:paintingArray.PAINTING_ID")
                .select(context);

        Collections.sort((List<T>)res);
        Collections.sort(sqlResult);

        assertEquals(3, sqlResult.size());
        assertEquals(res, sqlResult);
    }

}
