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

package org.objectstyle.cayenne.access;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.conn.DataSourceInfo;
import org.objectstyle.cayenne.conn.DriverDataSource;
import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.cayenne.dba.PkGenerator;
import org.objectstyle.cayenne.dba.TypesMapping;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DbJoin;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.DerivedDbEntity;
import org.objectstyle.cayenne.validation.SimpleValidationFailure;
import org.objectstyle.cayenne.validation.ValidationResult;

/**
 * Utility class that generates database schema based on Cayenne mapping. It is a logical
 * counterpart of DbLoader class.
 * 
 * @author Andrei Adamchik
 */
public class DbGenerator {

    private Logger logObj = Logger.getLogger(DbGenerator.class);

    /**
     * @deprecated Unused since 1.1
     */
    protected DataNode node;
    
    protected DbAdapter adapter;
    protected DataMap map;

    // stores generated SQL statements
    protected Map dropTables;
    protected Map createTables;
    protected Map createFK;
    protected List createPK;
    protected List dropPK;

    /**
     * Contains all DbEntities ordered considering their interdependencies.
     * DerivedDbEntities are filtered out of this list.
     */
    protected List dbEntitiesInInsertOrder;
    protected List dbEntitiesRequiringAutoPK;

    protected boolean shouldDropTables;
    protected boolean shouldCreateTables;
    protected boolean shouldDropPKSupport;
    protected boolean shouldCreatePKSupport;
    protected boolean shouldCreateFKConstraints;

    protected ValidationResult failures;

    /**
     * Creates and initializes new DbGenerator.
     */
    public DbGenerator(DbAdapter adapter, DataMap map) {
        this(adapter, map, Collections.EMPTY_LIST);
    }

    /**
     * Creates and initializes new DbGenerator. <code>excludedEntities</code> parameter
     * specifies which entities should be ignored during class generation.
     */
    public DbGenerator(DbAdapter adapter, DataMap map, Collection excludedEntities) {
        // sanity check
        if (adapter == null) {
            throw new IllegalArgumentException("Adapter must not be null.");
        }

        if (map == null) {
            throw new IllegalArgumentException("DataMap must not be null.");
        }

        this.map = map;
        this.adapter = adapter;

        prepareDbEntities(excludedEntities);
        resetToDefaults();
        buildStatements();
    }

    protected void resetToDefaults() {
        this.shouldDropTables = false;
        this.shouldDropPKSupport = false;
        this.shouldCreatePKSupport = true;
        this.shouldCreateTables = true;
        this.shouldCreateFKConstraints = true;
    }

    /**
     * Creates and stores internally a set of statements for database schema creation,
     * ignoring configured schema creation preferences. Statements are NOT executed in
     * this method.
     */
    protected void buildStatements() {
        dropTables = new HashMap();
        createTables = new HashMap();
        createFK = new HashMap();

        DbAdapter adapter = getAdapter();
        Iterator it = dbEntitiesInInsertOrder.iterator();
        boolean supportsFK = adapter.supportsFkConstraints();
        while (it.hasNext()) {
            DbEntity dbe = (DbEntity) it.next();

            String name = dbe.getName();

            // build "DROP TABLE"
            dropTables.put(name, adapter.dropTable(dbe));

            // build "CREATE TABLE"
            createTables.put(name, adapter.createTable(dbe));

            // build "FK"
            if (supportsFK) {
                createFK.put(name, createFkConstraintsQueries(dbe));
            }
        }

        PkGenerator pkGenerator = adapter.getPkGenerator();
        dropPK = pkGenerator.dropAutoPkStatements(dbEntitiesRequiringAutoPK);
        createPK = pkGenerator.createAutoPkStatements(dbEntitiesRequiringAutoPK);
    }

    /**
     * Returns <code>true</code> if there is nothing to be done by this generator. If
     * <code>respectConfiguredSettings</code> is <code>true</code>, checks are done
     * applying currently configured settings, otherwise check is done, assuming that all
     * possible generated objects.
     */
    public boolean isEmpty(boolean respectConfiguredSettings) {
        if (dbEntitiesInInsertOrder.isEmpty() && dbEntitiesRequiringAutoPK.isEmpty()) {
            return true;
        }

        if (!respectConfiguredSettings) {
            return false;
        }

        return !(shouldDropTables
                || shouldCreateTables
                || shouldCreateFKConstraints
                || shouldCreatePKSupport || shouldDropPKSupport);
    }

    /** Returns DbAdapter associated with this DbGenerator. */
    public DbAdapter getAdapter() {
        return adapter;
    }

    /**
     * Returns a list of all schema statements that should be executed with the current
     * configuration.
     */
    public List configuredStatements() {
        List list = new ArrayList();

        if (shouldDropTables) {
            ListIterator it = dbEntitiesInInsertOrder
                    .listIterator(dbEntitiesInInsertOrder.size());
            while (it.hasPrevious()) {
                DbEntity ent = (DbEntity) it.previous();
                list.add(dropTables.get(ent.getName()));
            }
        }

        if (shouldCreateTables) {
            Iterator it = dbEntitiesInInsertOrder.iterator();
            while (it.hasNext()) {
                DbEntity ent = (DbEntity) it.next();
                list.add(createTables.get(ent.getName()));
            }
        }

        if (shouldCreateFKConstraints && getAdapter().supportsFkConstraints()) {
            Iterator it = dbEntitiesInInsertOrder.iterator();
            while (it.hasNext()) {
                DbEntity ent = (DbEntity) it.next();
                List fks = (List) createFK.get(ent.getName());
                list.addAll(fks);
            }
        }

        if (shouldDropPKSupport) {
            list.addAll(dropPK);
        }

        if (shouldCreatePKSupport) {
            list.addAll(createPK);
        }

        return list;
    }

    /**
     * Creates a temporary DataSource out of DataSourceInfo and invokes
     * <code>public void runGenerator(DataSource ds)</code>.
     */
    public void runGenerator(DataSourceInfo dsi) throws Exception {
        this.failures = null;

        // do a pre-check. Maybe there is no need to run anything
        // and therefore no need to create a connection
        if (isEmpty(true)) {
            return;
        }

        Driver driver = (Driver) Class.forName(dsi.getJdbcDriver()).newInstance();
        DataSource dataSource = new DriverDataSource(driver, dsi.getDataSourceUrl(), dsi
                .getUserName(), dsi.getPassword());

        runGenerator(dataSource);
    }

    /**
     * Executes a set of commands to drop/create database objects. This is the main worker
     * method of DbGenerator. Command set is built based on pre-configured generator
     * settings.
     */
    public void runGenerator(DataSource ds) throws Exception {
        this.failures = null;

        Connection connection = ds.getConnection();

        try {

            // drop tables
            if (shouldDropTables) {
                ListIterator it = dbEntitiesInInsertOrder
                        .listIterator(dbEntitiesInInsertOrder.size());
                while (it.hasPrevious()) {
                    DbEntity ent = (DbEntity) it.previous();
                    safeExecute(connection, (String) dropTables.get(ent.getName()));
                }
            }

            // create tables
            List createdTables = new ArrayList();
            if (shouldCreateTables) {
                Iterator it = dbEntitiesInInsertOrder.iterator();
                while (it.hasNext()) {
                    DbEntity ent = (DbEntity) it.next();

                    // only create missing tables

                    safeExecute(connection, (String) createTables.get(ent.getName()));
                    createdTables.add(ent.getName());
                }
            }

            // create FK
            if (shouldCreateTables
                    && shouldCreateFKConstraints
                    && getAdapter().supportsFkConstraints()) {
                Iterator it = dbEntitiesInInsertOrder.iterator();
                while (it.hasNext()) {
                    DbEntity ent = (DbEntity) it.next();

                    if (createdTables.contains(ent.getName())) {
                        List fks = (List) createFK.get(ent.getName());
                        Iterator fkIt = fks.iterator();
                        while (fkIt.hasNext()) {
                            safeExecute(connection, (String) fkIt.next());
                        }
                    }
                }
            }
            
            // drop PK
            if (shouldDropPKSupport) {
                List dropAutoPKSQL = getAdapter().getPkGenerator().dropAutoPkStatements(
                        dbEntitiesRequiringAutoPK);
                Iterator it = dropAutoPKSQL.iterator();
                while (it.hasNext()) {
                    safeExecute(connection, (String) it.next());
                }
            }

            // create pk
            if (shouldCreatePKSupport) {
                List createAutoPKSQL = getAdapter()
                        .getPkGenerator()
                        .createAutoPkStatements(dbEntitiesRequiringAutoPK);
                Iterator it = createAutoPKSQL.iterator();
                while (it.hasNext()) {
                    safeExecute(connection, (String) it.next());
                }
            }
        }
        finally {
            connection.close();
        }
    }

    /**
     * Builds and executes a SQL statement, catching and storing SQL exceptions resulting
     * from invalid SQL. Only non-recoverable exceptions are rethrown.
     * 
     * @since 1.1
     */
    protected boolean safeExecute(Connection connection, String sql) throws SQLException {
        Statement statement = connection.createStatement();

        try {
            QueryLogger.logQuery(Level.INFO, sql, null);
            statement.execute(sql);
            return true;
        }
        catch (SQLException ex) {
            if (this.failures == null) {
                this.failures = new ValidationResult();
            }

            failures.addFailure(new SimpleValidationFailure(sql, ex.getMessage()));
            QueryLogger.logQueryError(Level.INFO, ex);
            return false;
        }
        finally {
            statement.close();
        }
    }

    /**
     * Executes a DDL statement, logging the execution via the QueryLogger.
     * 
     * @deprecated Since 1.1 this method is unused.
     */
    protected void executeStatement(String sql, Statement stmt) throws SQLException {
        QueryLogger.logQuery(Level.INFO, sql, null);

        try {
            stmt.execute(sql);
        }
        catch (SQLException ex) {
            SQLException newException = new SQLException("Failed statement: " + sql);
            newException.setNextException(ex);
            throw newException;
        }
    }

    /**
     * Returns an array of queries to create foreign key constraints for a particular
     * DbEntity. Throws CayenneRuntimeException, if called for adapter that does not
     * support FK constraints.
     */
    public List createFkConstraintsQueries(DbEntity dbEnt) {
        if (!getAdapter().supportsFkConstraints()) {
            throw new CayenneRuntimeException(
                    "FK constraints are not supported by adapter.");
        }

        List list = new ArrayList();
        Iterator it = dbEnt.getRelationships().iterator();
        while (it.hasNext()) {
            DbRelationship rel = (DbRelationship) it.next();

            if (rel.isToMany()) {
                continue;
            }

            // create an FK CONSTRAINT only if the relationship is to PK
            // and if this is not a dependent PK

            // create UNIQUE CONSTRAINT on FK if reverse relationship is to-one

            if (rel.isToPK() && !rel.isToDependentPK()) {

                if (getAdapter().supportsUniqueConstraints()) {

                    DbRelationship reverse = rel.getReverseRelationship();
                    if (reverse != null && !reverse.isToMany() && !reverse.isToPK()) {
                        list.add(getAdapter().createUniqueConstraint(
                                (DbEntity) rel.getSourceEntity(),
                                rel.getSourceAttributes()));
                    }
                }

                list.add(getAdapter().createFkConstraint(rel));
            }
        }
        return list;
    }

    /**
     * Returns an object representing a collection of failures that occurred on the last
     * "runGenerator" invocation, or null if there were no failures. Failures usually
     * indicate problems with generated DDL (such as "create...", "drop...", etc.) and
     * usually happen due to the DataMap being out of sync with the database.
     * 
     * @since 1.1
     */
    public ValidationResult getFailures() {
        return failures;
    }

    /**
     * Returns whether DbGenerator is configured to create primary key support for DataMap
     * entities.
     */
    public boolean shouldCreatePKSupport() {
        return shouldCreatePKSupport;
    }

    /**
     * Returns whether DbGenerator is configured to create tables for DataMap entities.
     */
    public boolean shouldCreateTables() {
        return shouldCreateTables;
    }

    public boolean shouldDropPKSupport() {
        return shouldDropPKSupport;
    }

    public boolean shouldDropTables() {
        return shouldDropTables;
    }

    public boolean shouldCreateFKConstraints() {
        return shouldCreateFKConstraints;
    }

    public void setShouldCreatePKSupport(boolean shouldCreatePKSupport) {
        this.shouldCreatePKSupport = shouldCreatePKSupport;
    }

    public void setShouldCreateTables(boolean shouldCreateTables) {
        this.shouldCreateTables = shouldCreateTables;
    }

    public void setShouldDropPKSupport(boolean shouldDropPKSupport) {
        this.shouldDropPKSupport = shouldDropPKSupport;
    }

    public void setShouldDropTables(boolean shouldDropTables) {
        this.shouldDropTables = shouldDropTables;
    }

    public void setShouldCreateFKConstraints(boolean shouldCreateFKConstraints) {
        this.shouldCreateFKConstraints = shouldCreateFKConstraints;
    }

    /**
     * Helper method that orders DbEntities to satisfy referential constraints and returns
     * an ordered list. It also filters out DerivedDbEntities.
     */
    private void prepareDbEntities(Collection excludedEntities) {
        if (excludedEntities == null) {
            excludedEntities = Collections.EMPTY_LIST;
        }

        // remove derived db entities
        List tables = new ArrayList();
        List tablesWithAutoPk = new ArrayList();
        Iterator it = map.getDbEntities().iterator();
        while (it.hasNext()) {
            DbEntity nextEntity = (DbEntity) it.next();

            // do sanity checks...

            // derived DbEntities are not included in generated SQL
            if (nextEntity instanceof DerivedDbEntity) {
                continue;
            }

            // tables with no columns are not included
            if (nextEntity.getAttributes().size() == 0) {
                logObj
                        .info("Skipping entity with no attributes: "
                                + nextEntity.getName());
                continue;
            }

            // check if this entity is explicitly excluded
            if (excludedEntities.contains(nextEntity)) {
                continue;
            }

            // tables with invalid DbAttributes are not included
            boolean invalidAttributes = false;
            Iterator nextDbAtributes = nextEntity.getAttributes().iterator();
            while (nextDbAtributes.hasNext()) {
                DbAttribute attr = (DbAttribute) nextDbAtributes.next();
                if (attr.getType() == TypesMapping.NOT_DEFINED) {
                    logObj.info("Skipping entity, attribute type is undefined: "
                            + nextEntity.getName()
                            + "."
                            + attr.getName());
                    invalidAttributes = true;
                    break;
                }
            }
            if (invalidAttributes) {
                continue;
            }

            tables.add(nextEntity);

            // check if an automatic PK generation can be potentailly supported
            // in this entity. For now simply check that the key is not propagated
            Iterator relationships = nextEntity.getRelationships().iterator();

            // create a copy of the original PK list,
            // since the list will be modified locally
            List pkAttributes = new ArrayList(nextEntity.getPrimaryKey());
            while (pkAttributes.size() > 0 && relationships.hasNext()) {
                DbRelationship nextRelationship = (DbRelationship) relationships.next();
                if (!nextRelationship.isToMasterPK()) {
                    continue;
                }

                // supposedly all source attributes of the relationship
                // to master entity must be a part of primary key,
                // so
                Iterator joins = nextRelationship.getJoins().iterator();
                while (joins.hasNext()) {
                    DbJoin join = (DbJoin) joins.next();
                    pkAttributes.remove(join.getSource());
                }
            }

            // primary key is needed only if at least one of the primary key attributes
            // is not propagated via releationship
            if (pkAttributes.size() > 0) {
                tablesWithAutoPk.add(nextEntity);
            }
        }

        // sort table list
        if (tables.size() > 1) {
            DataNode node = adapter.createDataNode("temp");
            node.addDataMap(map);
            node.getEntitySorter().sortDbEntities(tables, false);
        }

        this.dbEntitiesInInsertOrder = tables;
        this.dbEntitiesRequiringAutoPK = tablesWithAutoPk;
    }
}