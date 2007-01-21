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

package org.apache.cayenne.access.trans;

import org.apache.art.Artist;
import org.apache.cayenne.TranslationCase;
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.unit.CayenneCase;

public class OrderingTranslatorTest extends CayenneCase {

    protected TstQueryAssembler qa;
    protected SelectQuery q;

    protected void setUp() throws Exception {
        qa = new TstQueryAssembler(getNode(), new SelectQuery());
        q = (SelectQuery) qa.getQuery();
    }

    /**
     * Tests ascending ordering on string attribute.
     */
    public void testDoTranslation1() throws Exception {
        try {
            TranslationCase tstCase =
                new TranslationCase("Artist", null, "ta.ARTIST_NAME");
            q.setRoot(Artist.class);
            q.addOrdering("artistName", Ordering.ASC);
            String orderBySql = new OrderingTranslator(qa).doTranslation();

            assertNotNull(orderBySql);
            tstCase.assertTranslatedWell(orderBySql);
        }
        finally {
            qa.dispose();
        }
    }

    /**
     * Tests descending ordering on string attribute.
     */
    public void testDoTranslation2() throws Exception {
        try {
            TranslationCase tstCase =
                new TranslationCase("Artist", null, "ta.ARTIST_NAME DESC");
            q.setRoot(Artist.class);
            q.addOrdering("artistName", Ordering.DESC);
            String orderBySql = new OrderingTranslator(qa).doTranslation();

            assertNotNull(orderBySql);
            tstCase.assertTranslatedWell(orderBySql);
        }
        finally {
            qa.dispose();
        }
    }

    /**
     * Tests ascending caese-insensitive ordering on string attribute.
     */
    public void testDoTranslation4() throws Exception {
        try {
            TranslationCase tstCase =
                new TranslationCase("Artist", null, "UPPER(ta.ARTIST_NAME)");
            q.setRoot(Artist.class);
            q.addOrdering("artistName", Ordering.ASC, true);
            String orderBySql = new OrderingTranslator(qa).doTranslation();

            assertNotNull(orderBySql);
            assertTrue(orderBySql.indexOf("UPPER(") != -1);
            tstCase.assertTranslatedWell(orderBySql);
        }
        finally {
            qa.dispose();
        }
    }

    public void testDoTranslation5() throws Exception {
        try {
            TranslationCase tstCase =
                new TranslationCase(
                    "Artist",
                    null,
                    "UPPER(ta.ARTIST_NAME) DESC, ta.ESTIMATED_PRICE");
            q.setRoot(Artist.class);
            q.addOrdering("artistName", Ordering.DESC, true);
            q.addOrdering("paintingArray.estimatedPrice", Ordering.ASC);
            String orderBySql = new OrderingTranslator(qa).doTranslation();

            assertNotNull(orderBySql);
            //Check there is an UPPER modifier
            int indexOfUpper = orderBySql.indexOf("UPPER(");
            assertTrue(indexOfUpper != -1);

            // and ensure there is only ONE upper modifier
            assertTrue(orderBySql.indexOf("UPPER(", indexOfUpper + 1) == -1);
            tstCase.assertTranslatedWell(orderBySql);
        }
        finally {
            qa.dispose();
        }
    }

    public void testDoTranslation6() throws Exception {
        try {
            TranslationCase tstCase =
                new TranslationCase(
                    "Artist",
                    null,
                    "UPPER(ta.ARTIST_NAME), UPPER(ta.ESTIMATED_PRICE)");
            q.setRoot(Artist.class);
            q.addOrdering("artistName", Ordering.ASC, true);
            q.addOrdering("paintingArray.estimatedPrice", Ordering.ASC, true);
            String orderBySql = new OrderingTranslator(qa).doTranslation();

            assertNotNull(orderBySql);
            //Check there is at least one UPPER modifier
            int indexOfUpper = orderBySql.indexOf("UPPER(");
            assertTrue(indexOfUpper != -1);

            // and ensure there is another after it
            assertTrue(orderBySql.indexOf("UPPER(", indexOfUpper + 1) != -1);

            tstCase.assertTranslatedWell(orderBySql);
        }
        finally {
            qa.dispose();
        }
    }

    public void testDoTranslation3() throws Exception {
        try {
            TranslationCase tstCase =
                new TranslationCase(
                    "Artist",
                    null,
                    "ta.ARTIST_NAME DESC, ta.ESTIMATED_PRICE");
            q.setRoot(Artist.class);
            q.addOrdering("artistName", Ordering.DESC);
            q.addOrdering("paintingArray.estimatedPrice", Ordering.ASC);
            String orderBySql = new OrderingTranslator(qa).doTranslation();

            assertNotNull(orderBySql);
            tstCase.assertTranslatedWell(orderBySql);
        }
        finally {
            qa.dispose();
        }
    }
}
