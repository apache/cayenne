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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.query.BatchQuery;
import org.objectstyle.cayenne.query.UpdateBatchQuery;

/**
 * @author Andrei Adamchik
 */
public class LOBUpdateBatchQueryBuilder extends LOBBatchQueryBuilder {

    public LOBUpdateBatchQueryBuilder(DbAdapter adapter) {
        super(adapter);
    }

    public List getValuesForLOBUpdateParameters(BatchQuery query) {
        int len = query.getDbAttributes().size();
        UpdateBatchQuery updateBatch = (UpdateBatchQuery) query;

        List values = new ArrayList(len);
        List qualifierAttributes = updateBatch.getQualifierAttributes();
        List updatedDbAttributes = updateBatch.getUpdatedAttributes();

        int updatedLen = updatedDbAttributes.size();
        int qualifierLen = qualifierAttributes.size();
        for (int i = 0; i < updatedLen; i++) {
            DbAttribute attribute = (DbAttribute) updatedDbAttributes.get(i);
            Object value = query.getValue(i);
            if(isUpdateableColumn(value, attribute.getType())) {
            	values.add(value);
            }
        }

		for (int i = 0; i < qualifierLen; i++) {
			values.add(query.getValue(updatedLen + i));
        }

        return values;
    }

    public String createSqlString(BatchQuery batch) {
        UpdateBatchQuery updateBatch = (UpdateBatchQuery) batch;
        String table = batch.getDbEntity().getFullyQualifiedName();
        List idDbAttributes = updateBatch.getQualifierAttributes();
        List updatedDbAttributes = updateBatch.getUpdatedAttributes();
        StringBuffer query = new StringBuffer("UPDATE ");
        query.append(table).append(" SET ");

        int len = updatedDbAttributes.size();
        for (int i = 0; i < len; i++) {
            if (i > 0) {
                query.append(", ");
            }

            DbAttribute attribute = (DbAttribute) updatedDbAttributes.get(i);
            query.append(attribute.getName()).append(" = ");
            appendUpdatedParameter(query, attribute, batch.getValue(i));
        }

        query.append(" WHERE ");
        Iterator i = idDbAttributes.iterator();
        while (i.hasNext()) {
            DbAttribute attribute = (DbAttribute) i.next();
            appendDbAttribute(query, attribute);
            query.append(" = ?");
            if (i.hasNext()) {
                query.append(" AND ");
            }
        }
        return query.toString();
    }
}
