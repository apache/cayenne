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

package org.objectstyle.cayenne.access.trans;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.query.BatchQuery;
import org.objectstyle.cayenne.query.DeleteBatchQuery;

/**
 * Translator for delete BatchQueries. Creates parametrized DELETE SQL statements.
 * 
 * @author Andriy Shapochka, Andrei Adamchik, Mike Kienenberger
 */

public class DeleteBatchQueryBuilder extends BatchQueryBuilder {

    public DeleteBatchQueryBuilder(DbAdapter adapter) {
        super(adapter);
    }

    public String createSqlString(BatchQuery batch) {
        DeleteBatchQuery deleteBatch = (DeleteBatchQuery) batch;
        String table = batch.getDbEntity().getFullyQualifiedName();
        List qualifierAttributes = deleteBatch.getQualifierAttributes();

        StringBuffer query = new StringBuffer("DELETE FROM ");
        query.append(table).append(" WHERE ");

        Iterator i = qualifierAttributes.iterator();
        while (i.hasNext()) {
            DbAttribute attribute = (DbAttribute) i.next();
            appendDbAttribute(query, attribute);
            query.append(deleteBatch.isNull(attribute) ? " IS NULL" : " = ?");

            if (i.hasNext()) {
                query.append(" AND ");
            }
        }

        return query.toString();
    }

    /**
     * Binds BatchQuery parameters to the PreparedStatement.
     */
    public void bindParameters(PreparedStatement statement, BatchQuery query)
            throws SQLException, Exception {

        DeleteBatchQuery deleteBatch = (DeleteBatchQuery) query;
        List qualifierAttributes = deleteBatch.getQualifierAttributes();

        int parameterIndex = 1;

        for (int i = 0; i < qualifierAttributes.size(); i++) {
            Object value = query.getValue(i);
            DbAttribute attribute = (DbAttribute) qualifierAttributes.get(i);

            // skip null attributes... they are translated as "IS NULL"
            if (deleteBatch.isNull(attribute)) {
                continue;
            }

            adapter.bindParameter(
                    statement,
                    value,
                    parameterIndex++,
                    attribute.getType(),
                    attribute.getPrecision());
        }
    }
}