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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.access.DbGenerator;
import org.objectstyle.cayenne.access.QueryLogger;
import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.cayenne.dba.firebird.FirebirdAdapter;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DerivedDbEntity;

/**
 * Defines a set of algorithms useful for a generic AccessStack.
 * 
 * @author Andrei Adamchik
 */
public abstract class AbstractAccessStack {
    private static Logger logObj = Logger.getLogger(AbstractAccessStack.class);
    
    // hardcoded dependent entities that should be excluded
    // if LOBs are not supported
    private static final String[] EXTRA_EXCLUDED_FOR_NO_LOB = new String[] {
        "CLOB_DETAIL"
    };

    protected CayenneTestResources resources;

    public AccessStackAdapter getAdapter(DataNode node) {
        return resources.getAccessStackAdapter(node.getAdapter());
    }

    /**
     * Helper method that orders DbEntities to satisfy referential
     * constraints and returns an ordered list.
     */
    protected List dbEntitiesInInsertOrder(DataNode node, DataMap map) {
        List entities = new ArrayList(map.getDbEntities());

        // filter varios unsupported tests...

        // LOBs
        boolean excludeLOB = !getAdapter(node).supportsLobs();
        boolean excludeBinPK = !getAdapter(node).supportsBinaryPK();
        if (excludeLOB || excludeBinPK) {
            Iterator it = entities.iterator();
            List filtered = new ArrayList();
            while (it.hasNext()) {
                DbEntity ent = (DbEntity) it.next();
                
                if(ent instanceof DerivedDbEntity) {
                    continue;
                }

                // check for LOB attributes
                if (excludeLOB) {
                    if (Arrays.binarySearch(EXTRA_EXCLUDED_FOR_NO_LOB, ent.getName()) >= 0) {
                        continue;
                    }

                    boolean hasLob = false;
                    Iterator attrs = ent.getAttributes().iterator();
                    while (attrs.hasNext()) {
                        DbAttribute attr = (DbAttribute) attrs.next();
                        if (attr.getType() == Types.BLOB || attr.getType() == Types.CLOB) {
                            hasLob = true;
                            break;
                        }
                    }

                    if (hasLob) {
                        continue;
                    }
                }

                // check for BIN PK
                if (excludeBinPK) {
                    boolean skip = false;
                    Iterator attrs = ent.getAttributes().iterator();
                    while (attrs.hasNext()) {
                        // check for BIN PK or FK to BIN Pk
                        DbAttribute attr = (DbAttribute) attrs.next();
                        if (attr.getType() == Types.BINARY
                            || attr.getType() == Types.VARBINARY
                            || attr.getType() == Types.LONGVARBINARY) {

                            if (attr.isPrimaryKey() || attr.isForeignKey()) {
                                skip = true;
                                break;
                            }
                        }
                    }

                    if (skip) {
                        continue;
                    }
                }

                filtered.add(ent);
            }

            entities = filtered;
        }

        node.getEntitySorter().sortDbEntities(entities, false);
        return entities;
    }

    protected void deleteTestData(DataNode node, DataMap map) throws Exception {
        // TODO: move this to delegate
        boolean isFirebird = node.getAdapter() instanceof FirebirdAdapter;

        Connection conn = node.getDataSource().getConnection();
        List list = this.dbEntitiesInInsertOrder(node, map);
        try {
            if (conn.getAutoCommit()) {
                conn.setAutoCommit(false);
            }

            Statement stmt = conn.createStatement();

            ListIterator it = list.listIterator(list.size());
            while (it.hasPrevious()) {
                DbEntity ent = (DbEntity) it.previous();
                if (ent instanceof DerivedDbEntity) {
                    continue;
                }

                // this may not work on tables with reflexive relationships
                // at least on Firebird it doesn't... 

                if (isFirebird && "ARTGROUP".equalsIgnoreCase(ent.getName())) {
                    int deleted = 0;
                    String deleteChildren =
                        "DELETE FROM "
                            + ent.getName()
                            + " WHERE GROUP_ID NOT IN (SELECT DISTINCT PARENT_GROUP_ID FROM "
                            + ent.getName()
                            + ")";
                    do {
                        deleted = stmt.executeUpdate(deleteChildren);
                    }
                    while (deleted > 0);
                }

                String deleteSql = "DELETE FROM " + ent.getName();
                stmt.executeUpdate(deleteSql);
            }
            conn.commit();
            stmt.close();
        }
        finally {
            conn.close();
        }
    }

    protected void dropSchema(DataNode node, DataMap map) throws Exception {
        Connection conn = node.getDataSource().getConnection();
        List list = dbEntitiesInInsertOrder(node, map);

        try {
            DatabaseMetaData md = conn.getMetaData();
            ResultSet tables = md.getTables(null, null, "%", null);
            List allTables = new ArrayList();

            while (tables.next()) {
                // 'toUpperCase' is needed since most databases
                // are case insensitive, and some will convert names to lower case (PostgreSQL)
                String name = tables.getString("TABLE_NAME");
                if (name != null)
                    allTables.add(name.toUpperCase());
            }
            tables.close();

            getAdapter(node).willDropTables(conn, map, allTables);

            // drop all tables in the map
            Statement stmt = conn.createStatement();

            ListIterator it = list.listIterator(list.size());
            while (it.hasPrevious()) {
                DbEntity ent = (DbEntity) it.previous();
                if (!allTables.contains(ent.getName())) {
                    continue;
                }

                try {
                    String dropSql = node.getAdapter().dropTable(ent);
                    logObj.info(dropSql);
                    stmt.execute(dropSql);
                }
                catch (SQLException sqe) {
                    logObj.warn(
                        "Can't drop table " + ent.getName() + ", ignoring...",
                        sqe);
                }
            }

            getAdapter(node).droppedTables(conn, map);
        }
        finally {
            conn.close();
        }

    }

    protected void dropPKSupport(DataNode node, DataMap map) throws Exception {
        List filteredEntities = dbEntitiesInInsertOrder(node, map);
        node.getAdapter().getPkGenerator().dropAutoPk(node, filteredEntities);
    }

    protected void createPKSupport(DataNode node, DataMap map) throws Exception {
        List filteredEntities = dbEntitiesInInsertOrder(node, map);
        node.getAdapter().getPkGenerator().createAutoPk(node, filteredEntities);
    }

    protected void createSchema(DataNode node, DataMap map) throws Exception {
        Connection conn = node.getDataSource().getConnection();

        try {
            getAdapter(node).willCreateTables(conn, map);
            Statement stmt = conn.createStatement();
            Iterator it = tableCreateQueries(node, map);
            while (it.hasNext()) {
                String query = (String) it.next();
                QueryLogger.logQuery(
                    QueryLogger.DEFAULT_LOG_LEVEL,
                    query,
                    Collections.EMPTY_LIST);
                stmt.execute(query);
            }
            getAdapter(node).createdTables(conn, map);
        }
        finally {
            conn.close();
        }
    }

    /** 
     * Returns iterator of preprocessed table create queries.
     */
    protected Iterator tableCreateQueries(DataNode node, DataMap map) throws Exception {
        DbAdapter adapter = node.getAdapter();
        DbGenerator gen = new DbGenerator(adapter, map);
        List orderedEnts = dbEntitiesInInsertOrder(node, map);
        List queries = new ArrayList();

        // table definitions
        Iterator it = orderedEnts.iterator();
        while (it.hasNext()) {
            DbEntity ent = (DbEntity) it.next();
            if (ent instanceof DerivedDbEntity) {
                continue;
            }

            queries.add(adapter.createTable(ent));
        }

        // FK constraints
        if (adapter.supportsFkConstraints()) {
            it = orderedEnts.iterator();
            while (it.hasNext()) {
                DbEntity ent = (DbEntity) it.next();
                if (ent instanceof DerivedDbEntity) {
                    continue;
                }

                List qs = gen.createFkConstraintsQueries(ent);
                queries.addAll(qs);
            }
        }

        return queries.iterator();
    }
}
