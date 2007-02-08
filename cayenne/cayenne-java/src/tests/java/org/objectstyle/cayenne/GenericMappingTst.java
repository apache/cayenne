/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
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
package org.objectstyle.cayenne;

import java.util.List;

import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.exp.ExpressionFactory;
import org.objectstyle.cayenne.query.SQLTemplate;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.unit.AccessStack;
import org.objectstyle.cayenne.unit.CayenneTestCase;
import org.objectstyle.cayenne.unit.CayenneTestResources;

public class GenericMappingTst extends CayenneTestCase {

    protected AccessStack buildAccessStack() {
        return CayenneTestResources.getResources().getAccessStack("GenericStack");
    }

    protected void setUp() throws Exception {
        super.setUp();
        deleteTestData();
    }

    public void testInsertSingle() {
        DataContext context = createDataContext();

        DataObject g1 = context.createAndRegisterNewObject("Generic1");
        g1.writeProperty("name", "G1 Name");

        context.commitChanges();
    }

    public void testInsertRelated() {
        DataContext context = createDataContext();

        DataObject g1 = context.createAndRegisterNewObject("Generic1");
        g1.writeProperty("name", "G1 Name");

        DataObject g2 = context.createAndRegisterNewObject("Generic2");
        g2.writeProperty("name", "G2 Name");
        g2.setToOneTarget("toGeneric1", g1, true);

        context.commitChanges();
    }

    public void testSelect() {
        DataContext context = createDataContext();

        context.performNonSelectingQuery(new SQLTemplate(
                "Generic1",
                "INSERT INTO GENERIC1 (ID, NAME) VALUES (1, 'AAAA')"));
        context.performNonSelectingQuery(new SQLTemplate(
                "Generic1",
                "INSERT INTO GENERIC1 (ID, NAME) VALUES (2, 'BBBB')"));
        context.performNonSelectingQuery(new SQLTemplate(
                "Generic1",
                "INSERT INTO GENERIC2 (GENERIC1_ID, ID, NAME) VALUES (1, 1, 'CCCCC')"));

        Expression qual = ExpressionFactory.matchExp("name", "AAAA");
        SelectQuery q = new SelectQuery("Generic1", qual);

        List result = context.performQuery(q);
        assertEquals(1, result.size());
    }

    public void testUpdateRelated() {
        DataContext context = createDataContext();

        DataObject g1 = context.createAndRegisterNewObject("Generic1");
        g1.writeProperty("name", "G1 Name");

        DataObject g2 = context.createAndRegisterNewObject("Generic2");
        g2.writeProperty("name", "G2 Name");
        g2.setToOneTarget("toGeneric1", g1, true);

        context.commitChanges();

        List r1 = (List) g1.readProperty("generic2s");
        assertTrue(r1.contains(g2));

        DataObject g11 = context.createAndRegisterNewObject("Generic1");
        g11.writeProperty("name", "G11 Name");
        g2.setToOneTarget("toGeneric1", g11, true);

        context.commitChanges();

        List r11 = (List) g11.readProperty("generic2s");
        assertTrue(r11.contains(g2));

        List r1_1 = (List) g1.readProperty("generic2s");
        assertFalse(r1_1.contains(g2));
    }
}
