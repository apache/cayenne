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

package org.objectstyle.cayenne.access;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.objectstyle.art.Artist;
import org.objectstyle.cayenne.access.util.DefaultOperationObserver;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.Procedure;
import org.objectstyle.cayenne.query.BatchQuery;
import org.objectstyle.cayenne.query.DeleteBatchQuery;
import org.objectstyle.cayenne.query.DeleteQuery;
import org.objectstyle.cayenne.query.InsertBatchQuery;
import org.objectstyle.cayenne.query.InsertQuery;
import org.objectstyle.cayenne.query.ProcedureQuery;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.query.UpdateBatchQuery;
import org.objectstyle.cayenne.query.UpdateQuery;
import org.objectstyle.cayenne.unit.CayenneTestCase;

/**
 * Tests query routing in DataNode.performQueries()
 * 
 * @author Andrei Adamchik
 */
public class DataNodeSwitchingTst extends CayenneTestCase {
    private NodeSwitchingMockup node;

    protected void setUp() throws Exception {
        super.setUp();

        node = new NodeSwitchingMockup();
        node.setDataSource(getNode().getDataSource());
    }

    public void testSelectQuery() throws Exception {
        assertQuery(new SelectQuery(Artist.class), "runSelect");
    }

    /**
     * @deprecated Since 1.1 SqlSelectQuery is deprecated, so is the test.
     */
    public void testSQLSelectQuery() throws Exception {
        assertQuery(
            new org.objectstyle.cayenne.query.SqlSelectQuery(Artist.class),
            "runSelect");
    }

    public void testUpdateQuery() throws Exception {
        assertQuery(new UpdateQuery(Artist.class), "runUpdate");
    }

    public void testDeleteQuery() throws Exception {
        assertQuery(new DeleteQuery(Artist.class), "runUpdate");
    }

    public void testInsertQuery() throws Exception {
        assertQuery(new InsertQuery(Artist.class), "runUpdate");
    }

    public void testBatchUpdateQuery() throws Exception {
        assertQuery(
            new UpdateBatchQuery(
                new DbEntity(),
                Collections.EMPTY_LIST,
                Collections.EMPTY_LIST,
                null,
                1),
            "runBatchUpdate");
    }

    public void testBatchDeleteQuery() throws Exception {
        assertQuery(new DeleteBatchQuery(new DbEntity(), 1), "runBatchUpdate");
    }

    public void testBatchInsertQuery() throws Exception {
        assertQuery(new InsertBatchQuery(new DbEntity(), 1), "runBatchUpdate");
    }

    public void testProcedureQuery() throws Exception {
        assertQuery(new ProcedureQuery(new Procedure()), "runStoredProcedure");
    }

    protected void assertQuery(Query q, String expectedMethod) throws Exception {
        node.performQueries(Collections.singletonList(q), new DefaultOperationObserver());
        List calls = node.getMethodsCalled();
        assertEquals(expectedMethod, 1, calls.size());
        assertEquals(expectedMethod, calls.get(0));
    }

    private class NodeSwitchingMockup extends DataNode {
        protected List methodsCalled = new ArrayList();

        protected void runBatchUpdate(
            Connection con,
            BatchQuery query,
            OperationObserver delegate)
            throws SQLException, Exception {
            methodsCalled.add("runBatchUpdate");
        }

        protected void runSelect(Connection con, Query query, OperationObserver delegate)
            throws SQLException, Exception {
            methodsCalled.add("runSelect");
        }

        protected void runStoredProcedure(
            Connection con,
            Query query,
            OperationObserver delegate)
            throws SQLException, Exception {
            methodsCalled.add("runStoredProcedure");
        }

        protected void runUpdate(Connection con, Query query, OperationObserver delegate)
            throws SQLException, Exception {
            methodsCalled.add("runUpdate");
        }

        public List getMethodsCalled() {
            return methodsCalled;
        }
    }
}
