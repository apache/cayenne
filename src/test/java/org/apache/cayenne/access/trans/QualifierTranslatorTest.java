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

import org.apache.art.Gallery;
import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.TstBinaryExpSuite;
import org.apache.cayenne.exp.TstExpressionCase;
import org.apache.cayenne.exp.TstExpressionSuite;
import org.apache.cayenne.exp.TstTernaryExpSuite;
import org.apache.cayenne.exp.TstUnaryExpSuite;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.MockQuery;
import org.apache.cayenne.query.QualifiedQuery;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.unit.CayenneCase;

public class QualifierTranslatorTest extends CayenneCase {

    protected TstQueryAssembler qa;

    protected void setUp() throws java.lang.Exception {
        qa = new TstQueryAssembler(getNode(), new SelectQuery());
    }

    public void testNonQualifiedQuery() throws Exception {
        qa.dispose();
        qa = new TstQueryAssembler(getNode(), new MockQuery());

        try {
            new QualifierTranslator(qa).doTranslation();
            fail();
        }
        catch (ClassCastException ccex) {
            // exception expected
        }
        finally {
            qa.dispose();
        }
    }

    public void testNullQualifier() throws Exception {
        try {
            assertNull(new QualifierTranslator(qa).doTranslation());
        }
        finally {
            qa.dispose();
        }
    }

    public void testUnary() throws Exception {
        doExpressionTest(new TstUnaryExpSuite());
    }

    public void testBinary() throws Exception {
        doExpressionTest(new TstBinaryExpSuite());
    }

    public void testTernary() throws Exception {
        doExpressionTest(new TstTernaryExpSuite());
    }

    public void testExtras() throws Exception {
        ObjectId oid1 = new ObjectId("Gallery", "GALLERY_ID", 1);
        ObjectId oid2 = new ObjectId("Gallery", "GALLERY_ID", 2);
        Gallery g1 = new Gallery();
        Gallery g2 = new Gallery();
        g1.setObjectId(oid1);
        g2.setObjectId(oid2);

        Expression e1 = ExpressionFactory.matchExp("toGallery", g1);
        Expression e2 = e1.orExp(ExpressionFactory.matchExp("toGallery", g2));

        TstExpressionCase extraCase = new TstExpressionCase(
                "Exhibit",
                e2,
                "(ta.GALLERY_ID = ?) OR (ta.GALLERY_ID = ?)",
                4,
                4);

        TstExpressionSuite suite = new TstExpressionSuite() {
        };
        suite.addCase(extraCase);
        doExpressionTest(suite);
    }

    private void doExpressionTest(TstExpressionSuite suite) throws Exception {

        try {
            TstExpressionCase[] cases = suite.cases();

            int len = cases.length;
            for (int i = 0; i < len; i++) {
                try {
                    QualifiedQuery q = (QualifiedQuery) qa.getQuery();
                    q.setQualifier(cases[i].getCayenneExp());
                    ObjEntity ent = getObjEntity(cases[i].getRootEntity());
                    assertNotNull(ent);
                    q.setRoot(ent);
                    String translated = new QualifierTranslator(qa).doTranslation();
                    cases[i].assertTranslatedWell(translated);
                }
                catch (Exception ex) {
                    throw new CayenneRuntimeException("Failed case: [" + i + "]: " + cases[i], ex);
                }
            }
        }
        finally {
            qa.dispose();
        }
    }
}
