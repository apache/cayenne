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
package org.objectstyle.cayenne.access.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.access.OperationObserver;
import org.objectstyle.cayenne.access.QueryLogger;
import org.objectstyle.cayenne.access.QueryTranslator;
import org.objectstyle.cayenne.access.trans.DeleteTranslator;
import org.objectstyle.cayenne.access.trans.UpdateTranslator;
import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.cayenne.map.EntityResolver;
import org.objectstyle.cayenne.query.DeleteQuery;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.UpdateQuery;

/**
 * @since 1.2
 * @author Andrei Adamchik
 */
public class UpdateAction extends BaseSQLAction {

    protected Query query;

    public UpdateAction(Query query, DbAdapter adapter, EntityResolver entityResolver) {
        super(adapter, entityResolver);
        this.query = query;
    }

    protected QueryTranslator createTranslator(Connection connection) {
        QueryTranslator translator = checkDeprecatedQueries(query);

        if (translator == null) {
            if (query instanceof UpdateQuery) {
                translator = new UpdateTranslator();
            }
            else if (query instanceof DeleteQuery) {
                translator = new DeleteTranslator();
            }
            else {
                throw new CayenneRuntimeException("Can't make a translator for query "
                        + query);
            }
        }

        translator.setAdapter(getAdapter());
        translator.setQuery(query);
        translator.setEntityResolver(getEntityResolver());
        translator.setConnection(connection);

        return translator;
    }

    /**
     * @deprecated remove once deprcated queries processed here are removed.
     */
    final QueryTranslator checkDeprecatedQueries(Query query) {
        if (query instanceof org.objectstyle.cayenne.query.InsertQuery) {
            return new org.objectstyle.cayenne.access.trans.InsertTranslator();
        }

        return null;
    }

    public void performAction(Connection connection, OperationObserver observer)
            throws SQLException, Exception {

        QueryTranslator translator = createTranslator(connection);

        PreparedStatement statement = translator.createStatement();

        try {
            // execute update
            int count = statement.executeUpdate();
            QueryLogger.logUpdateCount(count);

            // send results back to consumer
            observer.nextCount(query, count);
        }
        finally {
            statement.close();
        }
    }
}