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
package org.objectstyle.cayenne.unit;

import java.sql.Connection;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DbEntity;

/**
 * @author Andrei Adamchik
 */
public class MySQLStackAdapter extends AccessStackAdapter {

    static final Collection NO_CONSTRAINTS_TABLES = Arrays.asList(new Object[] {
            "REFLEXIVE_AND_TO_ONE", "ARTGROUP"
    });

    public MySQLStackAdapter(DbAdapter adapter) {
        super(adapter);
    }

    public boolean supportsLobs() {
        return true;
    }

    public boolean supportsCaseSensitiveLike() {
        return false;
    }

    public void willDropTables(Connection conn, DataMap map, Collection tablesToDrop)
            throws Exception {
        // special DROP CONSTRAINT syntax for MySQL
        if (adapter.supportsFkConstraints()) {
            Map constraintsMap = getConstraints(conn, map, tablesToDrop);

            Iterator it = constraintsMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();

                Collection constraints = (Collection) entry.getValue();
                if (constraints == null || constraints.isEmpty()) {
                    continue;
                }

                Object tableName = entry.getKey();
                Iterator cit = constraints.iterator();
                while (cit.hasNext()) {
                    Object constraint = cit.next();
                    StringBuffer drop = new StringBuffer();
                    drop.append("ALTER TABLE ").append(tableName).append(
                            " DROP FOREIGN KEY ").append(constraint);
                    executeDDL(conn, drop.toString());
                }
            }
        }
    }

    public boolean supportsFKConstraints(DbEntity entity) {
        // MySQL supports that, but there are problems deleting objects from such
        // tables...
        return adapter.supportsFkConstraints()
                && !NO_CONSTRAINTS_TABLES.contains(entity.getName());
    }

}
