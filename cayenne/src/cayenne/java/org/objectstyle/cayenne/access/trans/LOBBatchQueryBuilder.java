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

package org.objectstyle.cayenne.access.trans;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Iterator;
import java.util.List;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.cayenne.dba.TypesMapping;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.query.BatchQuery;

/**
 * Superclass of query builders for the DML operations involving LOBs.
 * 
 * @author Andrei Adamchik
 */
public abstract class LOBBatchQueryBuilder extends BatchQueryBuilder {

    protected String newClobFunction;
    protected String newBlobFunction;

    public LOBBatchQueryBuilder(DbAdapter adapter) {
        super(adapter);
    }
    
    
    public abstract List getValuesForLOBUpdateParameters(BatchQuery query);

    public String createLOBSelectString(
        BatchQuery updateQuery,
        List selectedLOBAttributes,
        List qualifierAttributes) {

        StringBuffer buf = new StringBuffer();
        buf.append("SELECT ");

        Iterator it = selectedLOBAttributes.iterator();
        while (it.hasNext()) {
            buf.append(((DbAttribute) it.next()).getName());

            if (it.hasNext()) {
                buf.append(", ");
            }
        }

        buf.append(" FROM ").append(
            updateQuery.getDbEntity().getFullyQualifiedName()).append(
            " WHERE ");

        it = qualifierAttributes.iterator();
        while (it.hasNext()) {
            DbAttribute attribute = (DbAttribute) it.next();
            appendDbAttribute(buf, attribute);
            buf.append(" = ?");
            if (it.hasNext()) {
                buf.append(" AND ");
            }
        }

        buf.append(" FOR UPDATE");
        return buf.toString();
    }

    /**
      * Appends parameter placeholder for the value of the column
      * being updated. If requested, performs special handling on LOB
      * columns.
      */
    protected void appendUpdatedParameter(
        StringBuffer buf,
        DbAttribute dbAttribute,
        Object value) {

        int type = dbAttribute.getType();

        if (isUpdateableColumn(value, type)) {
            buf.append('?');
        }
        else {
            if (type == Types.CLOB) {
                buf.append(newClobFunction);
            }
            else if (type == Types.BLOB) {
                buf.append(newBlobFunction);
            }
            else {
                throw new CayenneRuntimeException(
                    "Unknown LOB column type: "
                        + type
                        + "("
                        + TypesMapping.getSqlNameByType(type)
                        + "). Query buffer: "
                        + buf);
            }
        }
    }


    /**
     * Binds BatchQuery parameters to the PreparedStatement. 
     */
    public void bindParameters(
        PreparedStatement statement,
        BatchQuery query,
        List dbAttributes)
        throws SQLException, Exception {

        int attributeCount = dbAttributes.size();

        // i - attribute position in the query
        // j - PreparedStatement parameter position (starts with "1")
        for (int i = 0, j = 1; i < attributeCount; i++) {
            Object value = query.getObject(i);
            DbAttribute attribute = (DbAttribute) dbAttributes.get(i);
            int type = attribute.getType();

            // TODO: (Andrus) This works as long as there is no LOBs in qualifier
            if (isUpdateableColumn(value, type)) {
                adapter.bindParameter(
                    statement,
                    value,
                    j,
                    type,
                    attribute.getPrecision());

                j++;
            }
        }
    }

    protected boolean isUpdateableColumn(Object value, int type) {
        return value == null || (type != Types.BLOB && type != Types.CLOB);
    }

    public String getNewBlobFunction() {
        return newBlobFunction;
    }

    public String getNewClobFunction() {
        return newClobFunction;
    }

    public void setNewBlobFunction(String string) {
        newBlobFunction = string;
    }

    public void setNewClobFunction(String string) {
        newClobFunction = string;
    }
}
