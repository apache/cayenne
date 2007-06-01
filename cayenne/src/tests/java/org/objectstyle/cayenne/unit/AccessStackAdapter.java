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

package org.objectstyle.cayenne.unit;

import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.Procedure;
import org.objectstyle.cayenne.util.Util;

/**
 * Defines API and a common superclass for testing various database features.
 * Different databases support different feature sets that need to be tested
 * differently. Many things implemented in subclasses may become future
 * candidates for inclusin in the corresponding adapter code.
 * 
 * @author Andrei Adamchik
 */
public class AccessStackAdapter {
    private static Logger logObj = Logger.getLogger(AccessStackAdapter.class);

    protected DbAdapter adapter;

    public AccessStackAdapter(DbAdapter adapter) {
        if(adapter == null) {
            throw new CayenneRuntimeException("Null adapter.");
        }
        this.adapter = adapter;
    }

    public DbAdapter getAdapter() {
        return adapter;
    }

    /**
     * Drops all table constraints.
     */
    public void willDropTables(Connection conn, DataMap map, Collection tablesToDrop)
        throws Exception {

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
                        " DROP CONSTRAINT ").append(
                        constraint);
                    executeDDL(conn, drop.toString());
                }
            }
        }
    }

    public void droppedTables(Connection con, DataMap map) throws Exception {

    }

    /**
     * Callback method that allows Delegate to customize
     * test procedure.
     */
    public void tweakProcedure(Procedure proc) {
    }

    public void willCreateTables(Connection con, DataMap map) throws Exception {
    }

    public void createdTables(Connection con, DataMap map) throws Exception {

    }

    public boolean supportsStoredProcedures() {
        return false;
    }

    /**
     * Returns true if the target database has support for large objects (BLOB,
     * CLOB).
     */
    public boolean supportsLobs() {
        return false;
    }

    public boolean supportsBinaryPK() {
        return true;
    }

    public boolean supportsHaving() {
        return true;
    }

    public boolean supportsCaseSensitiveLike() {
        return true;
    }
    
    public boolean supportsCaseInsensitiveOrder() {
        return true;
    }

    protected void executeDDL(Connection con, String ddl) throws Exception {
        logObj.info(ddl);
        Statement st = con.createStatement();

        try {
            st.execute(ddl);
        }
        finally {
            st.close();
        }
    }

    protected void executeDDL(Connection con, File sourceFile) throws Exception {
        // not sure if all JDBC adapters will like multiline statements
        // separated with '\n'. Oracle & Sybase seem OK, though
        // joining with space is probably safer, though produces ugly code
        String ddl = Util.stringFromFile(sourceFile, "\n");
        executeDDL(con, ddl);
    }

    /**
     * Returns a file under test resources DDL directory for the specified
     * database.
     */
    protected File ddlFile(String database, String name) {
        return new File(
            new File(
                new File(
                    CayenneTestResources.getResources().getTestResourcesDir(),
                    "ddl"),
                database),
            name);
    }

    public boolean handlesNullVsEmptyLOBs() {
        return supportsLobs();
    }

    /**
     * Returns a map of database constraints with DbEntity names used as keys,
     * and Collections of constraint names as values.
     */
    protected Map getConstraints(Connection conn, DataMap map, Collection includeTables)
        throws SQLException {
        Map constraintMap = new HashMap();

        DatabaseMetaData metadata = conn.getMetaData();
        Iterator it = includeTables.iterator();
        while (it.hasNext()) {
            String name = (String) it.next();
            DbEntity entity = map.getDbEntity(name);
            if (entity == null) {
                continue;
            }

            // Get all constraints for the table
            ResultSet rs =
                metadata.getExportedKeys(
                    entity.getCatalog(),
                    entity.getSchema(),
                    entity.getName());
            try {
                while (rs.next()) {
                    String fk = rs.getString("FK_NAME");
                    String fkTable = rs.getString("FKTABLE_NAME");

                    if (fk != null && fkTable != null) {
                        Collection constraints = (Collection) constraintMap.get(fkTable);
                        if (constraints == null) {
                            // use a set to avoid duplicate constraints
                            constraints = new HashSet();
                            constraintMap.put(fkTable, constraints);
                        }

                        constraints.add(fk);
                    }
                }
            }
            finally {
                rs.close();
            }
        }

        return constraintMap;
    }
}
