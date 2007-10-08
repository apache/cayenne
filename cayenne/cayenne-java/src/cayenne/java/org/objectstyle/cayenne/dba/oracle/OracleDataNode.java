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
package org.objectstyle.cayenne.dba.oracle;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.access.OperationObserver;
import org.objectstyle.cayenne.access.trans.BatchQueryBuilder;
import org.objectstyle.cayenne.access.trans.LOBBatchQueryBuilder;
import org.objectstyle.cayenne.access.trans.LOBBatchQueryWrapper;
import org.objectstyle.cayenne.query.BatchQuery;

/**
 * DataNode subclass customized for Oracle database engine.
 * 
 * @deprecated Since 1.2 DataNode customization is done entirely via DbAdapter.
 * @author Andrei Adamchik
 */
public class OracleDataNode extends DataNode {

    public OracleDataNode() {
        super();
    }

    public OracleDataNode(String name) {
        super(name);
    }

 
    /**
     * Special update method that is called from OracleAdapter if LOB columns are to be
     * updated.
     */
    public void runBatchUpdateWithLOBColumns(
            Connection con,
            BatchQuery query,
            OperationObserver delegate) throws SQLException, Exception {

        new OracleLOBBatchAction(query, getAdapter()).performAction(con, delegate);
    }

    /**
     * Selects a LOB row and writes LOB values.
     */
    protected void processLOBRow(
            Connection con,
            LOBBatchQueryBuilder queryBuilder,
            LOBBatchQueryWrapper selectQuery,
            List qualifierAttributes) throws SQLException, Exception {

        new OracleLOBBatchAction(null, getAdapter()).processLOBRow(con,
                queryBuilder,
                selectQuery,
                qualifierAttributes);
    }

    /**
     * Configures BatchQueryBuilder to trim CHAR column values, and then invokes super
     * implementation.
     */
    protected void runBatchUpdateAsBatch(
            Connection con,
            BatchQuery query,
            BatchQueryBuilder queryBuilder,
            OperationObserver delegate) throws SQLException, Exception {

        queryBuilder.setTrimFunction(OracleAdapter.TRIM_FUNCTION);
        super.runBatchUpdateAsBatch(con, query, queryBuilder, delegate);
    }

    /**
     * Configures BatchQueryBuilder to trim CHAR column values, and then invokes super
     * implementation.
     */
    protected void runBatchUpdateAsIndividualQueries(
            Connection con,
            BatchQuery query,
            BatchQueryBuilder queryBuilder,
            OperationObserver delegate) throws SQLException, Exception {

        queryBuilder.setTrimFunction(OracleAdapter.TRIM_FUNCTION);
        super.runBatchUpdateAsIndividualQueries(con, query, queryBuilder, delegate);
    }
}