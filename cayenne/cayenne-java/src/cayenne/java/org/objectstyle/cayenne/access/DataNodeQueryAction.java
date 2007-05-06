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

package org.objectstyle.cayenne.access;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import org.apache.log4j.Level;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.SQLAction;

/**
 * A helper that executes a sequence of queries, providing correct mapping of the results
 * to the original query. Note that this class is not thread-safe as it stores current
 * query execution state.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
class DataNodeQueryAction implements OperationObserver {

    OperationObserver observer;
    DataNode node;

    private Query currentQuery;

    public DataNodeQueryAction(DataNode node, OperationObserver observer) {
        this.observer = observer;
        this.node = node;
    }

    public void runQuery(Connection connection, Query query) throws SQLException,
            Exception {

        // remember root query ... it will be used to map the results, even if SQLAction
        // uses query substitute...
        this.currentQuery = query;

        SQLAction action = node.getAdapter().getAction(query, node);
        action.performAction(connection, this);
    }

    public void nextBatchCount(Query query, int[] resultCount) {
        observer.nextBatchCount(currentQuery, resultCount);
    }

    public void nextCount(Query query, int resultCount) {
        observer.nextCount(currentQuery, resultCount);
    }

    public void nextDataRows(Query query, List dataRows) {
        observer.nextDataRows(currentQuery, dataRows);
    }

    public void nextDataRows(Query q, ResultIterator it) {
        observer.nextDataRows(currentQuery, it);
    }

    public void nextGeneratedDataRows(Query query, ResultIterator keysIterator) {
        observer.nextGeneratedDataRows(currentQuery, keysIterator);
    }

    public void nextGlobalException(Exception ex) {
        observer.nextGlobalException(ex);
    }

    public void nextQueryException(Query query, Exception ex) {
        observer.nextQueryException(currentQuery, ex);
    }

    /**
     * @deprecated Unused since 1.2
     */
    public Level getLoggingLevel() {
        return observer.getLoggingLevel();
    }

    public boolean isIteratedResult() {
        return observer.isIteratedResult();
    }
}
