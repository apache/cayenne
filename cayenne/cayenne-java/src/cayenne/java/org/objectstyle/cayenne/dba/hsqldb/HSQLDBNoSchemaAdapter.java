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
package org.objectstyle.cayenne.dba.hsqldb;

import org.objectstyle.cayenne.map.DbEntity;


/**
 * A flavor of HSQLDBAdapter that implements workarounds for some old driver limitations.
 * 
 * @since 1.2
 * @author Mike Kienenberger
 */
public class HSQLDBNoSchemaAdapter extends HSQLDBAdapter {
    /**
     * Generate unqualified name without schema.
     * 
     * @since 1.2
     */
    protected String getTableName(DbEntity entity)
    {
        return  entity.getName();
    }

    /**
     * Generate unqualified name.
     * 
     * @since 1.2
     */
    protected String getSchemaName(DbEntity entity) {
        return "";
    }

    /**
     * Returns a SQL string to drop a table corresponding to <code>ent</code> DbEntity.
     * 
     * @since 1.2
     */
    public String dropTable(DbEntity ent) {
        // hsqldb doesn't support schema namespaces, so remove if found
        return "DROP TABLE " + getTableName(ent);
    }

    /**
     * Uses unqualified entity names.
     * 
     * @since 1.2
     */
    public String createTable(DbEntity ent) {
        String sql = super.createTable(ent);

        // hsqldb doesn't support schema namespaces, so remove if found
        String fqnCreate = "CREATE CACHED TABLE " + super.getTableName(ent) + " (";
        if (sql != null && sql.toUpperCase().startsWith(fqnCreate)) {
            sql = "CREATE CACHED TABLE "
                    + getTableName(ent)
                    + " ("
                    + sql.substring(fqnCreate.length());
        }

        return sql;
    }
}
