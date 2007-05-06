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
package org.objectstyle.cayenne.dba.frontbase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.access.QueryResult;
import org.objectstyle.cayenne.dba.JdbcPkGenerator;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.query.SQLTemplate;

/**
 * @since 1.2
 * @author Andrus Adamchik
 */
public class FrontBasePkGenerator extends JdbcPkGenerator {

    public FrontBasePkGenerator() {
        super();
    }

    /**
     * Retruns zero as PK caching is not supported by FrontBaseAdapter.
     */
    public int getPkCacheSize() {
        return 0;
    }

    public void createAutoPk(DataNode node, List dbEntities) throws Exception {
        // For each entity (re)set the unique counter
        Iterator it = dbEntities.iterator();
        while (it.hasNext()) {
            DbEntity ent = (DbEntity) it.next();
            runUpdate(node, pkCreateString(ent.getName()));
        }
    }

    public List createAutoPkStatements(List dbEntities) {
        List list = new ArrayList();

        Iterator it = dbEntities.iterator();
        while (it.hasNext()) {
            DbEntity ent = (DbEntity) it.next();
            list.add(pkCreateString(ent.getName()));
        }

        return list;
    }

    public void dropAutoPk(DataNode node, List dbEntities) throws Exception {
    }

    protected String pkTableCreateString() {
        return "";
    }

    protected String pkDeleteString(List dbEntities) {
        return "-- The 'Drop Primary Key Support' option is unavailable";
    }

    protected String pkCreateString(String entName) {
        StringBuffer buf = new StringBuffer();
        buf.append("SET UNIQUE = 1000000 FOR \"" + entName + "\"");
        return buf.toString();
    }

    protected String pkSelectString(String entName) {
        StringBuffer buf = new StringBuffer();
        buf.append("SELECT UNIQUE FROM \"" + entName + "\"");
        return buf.toString();
    }

    protected String pkUpdateString(String entName) {
        return "";
    }

    protected String dropAutoPkString() {
        return "";
    }

    protected int pkFromDatabase(DataNode node, DbEntity entity) throws Exception {
        String template = "SELECT #result('UNIQUE' 'int') FROM " + entity.getName();

        SQLTemplate query = new SQLTemplate(entity, template);
        QueryResult observer = new QueryResult();
        node.performQueries(Collections.singleton(query), observer);

        List results = observer.getFirstRows(query);
        if (results.size() != 1) {
            throw new CayenneRuntimeException("Error fetching PK. Expected one row, got "
                    + results.size());
        }

        Map row = (Map) results.get(0);
        Number pk = (Number) row.get("UNIQUE");
        return pk.intValue();
    }
}