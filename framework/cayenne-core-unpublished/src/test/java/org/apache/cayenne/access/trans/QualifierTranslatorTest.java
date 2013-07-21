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

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.TstBinaryExpSuite;
import org.apache.cayenne.exp.TstExpressionCase;
import org.apache.cayenne.exp.TstExpressionSuite;
import org.apache.cayenne.exp.TstTernaryExpSuite;
import org.apache.cayenne.exp.TstUnaryExpSuite;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.MockQuery;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.testdo.testmap.Gallery;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class QualifierTranslatorTest extends ServerCase {

    @Inject
    private DataNode node;

    public void testNonQualifiedQuery() throws Exception {
        TstQueryAssembler qa = new TstQueryAssembler(node, new MockQuery());

        try {
            new QualifierTranslator(qa).appendPart(new StringBuilder());
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
        TstQueryAssembler qa = new TstQueryAssembler(node, new SelectQuery());

        StringBuilder out = new StringBuilder();
        try {
            new QualifierTranslator(qa).appendPart(out);
        }
        finally {
            qa.dispose();
        }

        assertEquals(0, out.length());
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

        TstQueryAssembler qa = new TstQueryAssembler(node, new MockQuery());

        try {
            TstExpressionCase[] cases = suite.cases();

            int len = cases.length;
            for (int i = 0; i < len; i++) {
                try {

                    ObjEntity entity = node.getEntityResolver().getObjEntity(
                            cases[i].getRootEntity());
                    assertNotNull(entity);
                    SelectQuery q = new SelectQuery(entity);
                    q.setQualifier(cases[i].getCayenneExp());
                    qa.setQuery(q);

                    StringBuilder out = new StringBuilder();
                    new QualifierTranslator(qa).appendPart(out);
                    cases[i].assertTranslatedWell(out.toString());
                }
                catch (Exception ex) {
                    throw new CayenneRuntimeException("Failed case: ["
                            + i
                            + "]: "
                            + cases[i], ex);
                }
            }
        }
        finally {
            qa.dispose();
        }
    }
}
