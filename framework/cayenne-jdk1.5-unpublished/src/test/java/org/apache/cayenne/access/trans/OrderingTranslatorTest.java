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

    /**
     * Tests ascending ordering on string attribute.
     */
    public void testDoTranslation1() throws Exception {
        SelectQuery q = new SelectQuery(Artist.class);
        q.addOrdering("artistName", Ordering.ASC);

        TstQueryAssembler qa = new TstQueryAssembler(getNode(), q);

        try {
            TranslationCase tstCase = new TranslationCase(
                    "Artist",
                    null,
                    "ta.ARTIST_NAME");

            StringBuilder out = new StringBuilder();
            new OrderingTranslator(qa).appendPart(out);

            assertTrue(out.length() > 0);
            tstCase.assertTranslatedWell(out.toString());
        }
        finally {
            qa.dispose();
        }
    }

    /**
     * Tests descending ordering on string attribute.
     */
    public void testDoTranslation2() throws Exception {
        SelectQuery q = new SelectQuery(Artist.class);
        q.addOrdering("artistName", Ordering.DESC);

        TstQueryAssembler qa = new TstQueryAssembler(getNode(), q);

        try {
            TranslationCase tstCase = new TranslationCase(
                    "Artist",
                    null,
                    "ta.ARTIST_NAME DESC");

            StringBuilder out = new StringBuilder();
            new OrderingTranslator(qa).appendPart(out);

            assertTrue(out.length() > 0);
            tstCase.assertTranslatedWell(out.toString());
        }
        finally {
            qa.dispose();
        }
    }

    /**
     * Tests ascending caese-insensitive ordering on string attribute.
     */
    public void testDoTranslation4() throws Exception {
        SelectQuery q = new SelectQuery(Artist.class);
        q.addOrdering("artistName", Ordering.ASC, true);

        TstQueryAssembler qa = new TstQueryAssembler(getNode(), q);

        try {
            TranslationCase tstCase = new TranslationCase(
                    "Artist",
                    null,
                    "UPPER(ta.ARTIST_NAME)");

            StringBuilder out = new StringBuilder();
            new OrderingTranslator(qa).appendPart(out);

            assertTrue(out.length() > 0);
            String orderBySql = out.toString();
            assertTrue(orderBySql.contains("UPPER("));
            tstCase.assertTranslatedWell(orderBySql);
        }
        finally {
            qa.dispose();
        }
    }

    public void testDoTranslation5() throws Exception {
        SelectQuery q = new SelectQuery(Artist.class);
        q.addOrdering("artistName", Ordering.DESC, true);
        q.addOrdering("paintingArray.estimatedPrice", Ordering.ASC);

        TstQueryAssembler qa = new TstQueryAssembler(getNode(), q);

        try {
            TranslationCase tstCase = new TranslationCase(
                    "Artist",
                    null,
                    "UPPER(ta.ARTIST_NAME) DESC, ta.ESTIMATED_PRICE");

            StringBuilder out = new StringBuilder();
            new OrderingTranslator(qa).appendPart(out);

            assertTrue(out.length() > 0);
            String orderBySql = out.toString();

            // Check there is an UPPER modifier
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
        SelectQuery q = new SelectQuery(Artist.class);
        q.addOrdering("artistName", Ordering.ASC, true);
        q.addOrdering("paintingArray.estimatedPrice", Ordering.ASC, true);

        TstQueryAssembler qa = new TstQueryAssembler(getNode(), q);

        try {
            TranslationCase tstCase = new TranslationCase(
                    "Artist",
                    null,
                    "UPPER(ta.ARTIST_NAME), UPPER(ta.ESTIMATED_PRICE)");

            StringBuilder out = new StringBuilder();
            new OrderingTranslator(qa).appendPart(out);

            assertTrue(out.length() > 0);
            String orderBySql = out.toString();

            // Check there is at least one UPPER modifier
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
        SelectQuery q = new SelectQuery(Artist.class);

        q.addOrdering("artistName", Ordering.DESC);
        q.addOrdering("paintingArray.estimatedPrice", Ordering.ASC);

        TstQueryAssembler qa = new TstQueryAssembler(getNode(), q);

        try {
            TranslationCase tstCase = new TranslationCase(
                    "Artist",
                    null,
                    "ta.ARTIST_NAME DESC, ta.ESTIMATED_PRICE");

            StringBuilder out = new StringBuilder();
            new OrderingTranslator(qa).appendPart(out);

            assertTrue(out.length() > 0);
            String orderBySql = out.toString();
            tstCase.assertTranslatedWell(orderBySql);
        }
        finally {
            qa.dispose();
        }
    }
}
