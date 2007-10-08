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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.query.BatchQuery;

/**
 * Translator of InsertBatchQueries.
 * 
 * @author Andriy Shapochka
 * @author Andrei Adamchik
 */
public class InsertBatchQueryBuilder extends BatchQueryBuilder {

    public InsertBatchQueryBuilder(DbAdapter adapter) {
        super.setAdapter(adapter);
    }

    /**
     * Binds parameters for the current batch iteration to the PreparedStatement. Performs
     * filtering of attributes based on column generation rules.
     * 
     * @since 1.2
     */
    public void bindParameters(PreparedStatement statement, BatchQuery query)
            throws SQLException, Exception {

        List dbAttributes = query.getDbAttributes();
        int attributeCount = dbAttributes.size();

        // must use an independent counter "j" for prepared statement index
        for (int i = 0, j = 0; i < attributeCount; i++) {
            DbAttribute attribute = (DbAttribute) dbAttributes.get(i);
            if (includeInBatch(attribute)) {
                j++;
                Object value = query.getValue(i);
                adapter.bindParameter(statement, value, j, attribute.getType(), attribute
                        .getPrecision());
            }
        }
    }

    /**
     * Returns a list of values for the current batch iteration. Performs filtering of
     * attributes based on column generation rules. Used primarily for logging.
     * 
     * @since 1.2
     */
    public List getParameterValues(BatchQuery query) {
        List attributes = query.getDbAttributes();
        int len = attributes.size();
        List values = new ArrayList(len);
        for (int i = 0; i < len; i++) {
            DbAttribute attribute = (DbAttribute) attributes.get(i);
            if (includeInBatch(attribute)) {
                values.add(query.getValue(i));
            }
        }
        return values;
    }

    public String createSqlString(BatchQuery batch) {
        String table = batch.getDbEntity().getFullyQualifiedName();
        List dbAttributes = batch.getDbAttributes();

        StringBuffer query = new StringBuffer("INSERT INTO ");
        query.append(table).append(" (");

        int columnCount = 0;
        Iterator it = dbAttributes.iterator();

        while (it.hasNext()) {
            DbAttribute attribute = (DbAttribute) it.next();

            // attribute inclusion rule - one of the rules below must be true:
            // (1) attribute not generated
            // (2) attribute is generated and PK and adapter does not support generated
            // keys

            if (includeInBatch(attribute)) {

                if (columnCount > 0) {
                    query.append(", ");
                }
                query.append(attribute.getName());
                columnCount++;
            }
        }

        query.append(") VALUES (");

        for (int i = 0; i < columnCount; i++) {
            if (i > 0) {
                query.append(", ");
            }

            query.append('?');
        }
        query.append(')');
        return query.toString();
    }

    /**
     * Returns true if an attribute should be included in the batch.
     * 
     * @since 1.2
     */
    protected boolean includeInBatch(DbAttribute attribute) {
        // attribute inclusion rule - one of the rules below must be true:
        // (1) attribute not generated
        // (2) attribute is generated and PK and adapter does not support generated
        // keys

        return !attribute.isGenerated()
                || (attribute.isPrimaryKey() && !adapter.supportsGeneratedKeys());
    }
}