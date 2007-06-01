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
package org.objectstyle.cayenne.access.jdbc;

import java.sql.Connection;
import java.sql.Date;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectstyle.art.Artist;
import org.objectstyle.cayenne.access.DataContextTestBase;
import org.objectstyle.cayenne.query.SQLTemplate;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.unit.CayenneTestCase;
import org.objectstyle.cayenne.unit.util.MockOperationObserver;

/**
 * @author Andrei Adamchik
 */
public class SQLTemplateExecutionPlanTst extends CayenneTestCase {
    protected void setUp() throws Exception {
        super.setUp();
        deleteTestData();
    }

    public void testExecuteUpdate() throws Exception {
        String templateString =
            "INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME, DATE_OF_BIRTH) "
                + "VALUES (#bind($id), #bind($name), #bind($dob 'DATE'))";
        SQLTemplate template = new SQLTemplate(Object.class, templateString, false);

        Map bindings = new HashMap();
        bindings.put("id", new Integer(1));
        bindings.put("name", "a1");
        bindings.put("dob", new Date(System.currentTimeMillis()));
        template.setParameters(bindings);

        SQLTemplateExecutionPlan plan =
            new SQLTemplateExecutionPlan(getAccessStackAdapter().getAdapter());
        assertSame(getAccessStackAdapter().getAdapter(), plan.getAdapter());

        Connection c = getConnection();
        try {
            MockOperationObserver observer = new MockOperationObserver();
            plan.execute(c, template, observer);

            int[] batches = observer.countsForQuery(template);
            assertNotNull(batches);
            assertEquals(1, batches.length);
            assertEquals(1, batches[0]);
        }
        finally {
            c.close();
        }

        MockOperationObserver observer = new MockOperationObserver();
        SelectQuery query = new SelectQuery(Artist.class);
        getDomain().performQueries(Collections.singletonList(query), observer);

        List data = observer.rowsForQuery(query);
        assertEquals(1, data.size());
        Map row = (Map) data.get(0);
        assertEquals(bindings.get("id"), row.get("ARTIST_ID"));
        assertEquals(bindings.get("name"), row.get("ARTIST_NAME"));
        // to compare dates we need to create the binding correctly
        // assertEquals(bindings.get("dob"), row.get("DATE_OF_BIRTH"));
    }

    public void testExecuteUpdateNoParameters() throws Exception {
        getAccessStack().createTestData(DataContextTestBase.class, "testArtists");

        SQLTemplate template = new SQLTemplate(Object.class, "delete from ARTIST", false);

        SQLTemplateExecutionPlan plan =
            new SQLTemplateExecutionPlan(getAccessStackAdapter().getAdapter());
        assertSame(getAccessStackAdapter().getAdapter(), plan.getAdapter());

        Connection c = getConnection();
        try {
            MockOperationObserver observer = new MockOperationObserver();
            plan.execute(c, template, observer);

            int[] batches = observer.countsForQuery(template);
            assertNotNull(batches);
            assertEquals(1, batches.length);
            assertEquals(DataContextTestBase.artistCount, batches[0]);
        }
        finally {
            c.close();
        }
    }

    public void testExecuteUpdateBatch() throws Exception {
        String templateString =
            "INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME, DATE_OF_BIRTH) "
                + "VALUES (#bind($id), #bind($name), #bind($dob 'DATE'))";
        SQLTemplate template = new SQLTemplate(Object.class, templateString, false);

        Map bindings1 = new HashMap();
        bindings1.put("id", new Integer(1));
        bindings1.put("name", "a1");
        bindings1.put("dob", new Date(System.currentTimeMillis()));

        Map bindings2 = new HashMap();
        bindings2.put("id", new Integer(33));
        bindings2.put("name", "a$$$$$");
        bindings2.put("dob", new Date(System.currentTimeMillis()));
        template.setParameters(new Map[] { bindings1, bindings2 });

        SQLTemplateExecutionPlan plan =
            new SQLTemplateExecutionPlan(getAccessStackAdapter().getAdapter());
        assertSame(getAccessStackAdapter().getAdapter(), plan.getAdapter());

        Connection c = getConnection();
        try {
            MockOperationObserver observer = new MockOperationObserver();
            plan.execute(c, template, observer);

            int[] batches = observer.countsForQuery(template);
            assertNotNull(batches);
            assertEquals(2, batches.length);
            assertEquals(1, batches[0]);
            assertEquals(1, batches[1]);
        }
        finally {
            c.close();
        }

        MockOperationObserver observer = new MockOperationObserver();
        SelectQuery query = new SelectQuery(Artist.class);
        query.addOrdering("db:ARTIST_ID", true);
        getDomain().performQueries(Collections.singletonList(query), observer);

        List data = observer.rowsForQuery(query);
        assertEquals(2, data.size());
        Map row1 = (Map) data.get(0);
        assertEquals(bindings1.get("id"), row1.get("ARTIST_ID"));
        assertEquals(bindings1.get("name"), row1.get("ARTIST_NAME"));
        // to compare dates we need to create the binding correctly
        // assertEquals(bindings1.get("dob"), row.get("DATE_OF_BIRTH"));

        Map row2 = (Map) data.get(1);
        assertEquals(bindings2.get("id"), row2.get("ARTIST_ID"));
        assertEquals(bindings2.get("name"), row2.get("ARTIST_NAME"));
        // to compare dates we need to create the binding correctly
        // assertEquals(bindings2.get("dob"), row2.get("DATE_OF_BIRTH"));
    }
}
