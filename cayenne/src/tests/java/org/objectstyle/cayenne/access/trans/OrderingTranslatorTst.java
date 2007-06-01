/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
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
package org.objectstyle.cayenne.access.trans;

import org.objectstyle.art.Artist;
import org.objectstyle.cayenne.TranslationTestCase;
import org.objectstyle.cayenne.query.Ordering;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.unit.CayenneTestCase;

public class OrderingTranslatorTst extends CayenneTestCase {

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
            TranslationTestCase tstCase =
                new TranslationTestCase("Artist", null, "ta.ARTIST_NAME");
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
            TranslationTestCase tstCase =
                new TranslationTestCase("Artist", null, "ta.ARTIST_NAME DESC");
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
            TranslationTestCase tstCase =
                new TranslationTestCase("Artist", null, "UPPER(ta.ARTIST_NAME)");
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
            TranslationTestCase tstCase =
                new TranslationTestCase(
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
            TranslationTestCase tstCase =
                new TranslationTestCase(
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
            TranslationTestCase tstCase =
                new TranslationTestCase(
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