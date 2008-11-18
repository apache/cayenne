/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.access;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityInheritanceTree;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.Query;

/**
 * Defines API for translation Cayenne queries to JDBC PreparedStatements.
 * 
 * @deprecated since 3.0, as a part of translator inheritance hierarchy reorganization.
 *             This class is no longer in use as a common superclass.
 */
public abstract class QueryTranslator {

    protected Query query;
    protected Connection connection;
    protected DbAdapter adapter;
    protected EntityResolver entityResolver;

    /**
     * Creates and binds a PreparedStatement to execute query SQL via JDBC.
     */
    public abstract PreparedStatement createStatement() throws Exception;

    /**
     * Returns query object being processed.
     */
    public Query getQuery() {
        return query;
    }

    public void setQuery(Query query) {
        this.query = query;
    }

    /**
     * Returns Connection object used by this translator.
     * 
     * @since 1.2
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * @since 1.2
     */
    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public DbAdapter getAdapter() {
        return adapter;
    }

    public void setAdapter(DbAdapter adapter) {
        this.adapter = adapter;
    }

    /**
     * Returns an EntityInheritanceTree for the root entity.
     * 
     * @since 1.1
     */
    public EntityInheritanceTree getRootInheritanceTree() {
        return getEntityResolver().lookupInheritanceTree(getRootEntity());
    }

    public ObjEntity getRootEntity() {
        return query.getMetaData(getEntityResolver()).getObjEntity();
    }

    public DbEntity getRootDbEntity() {
        return query.getMetaData(getEntityResolver()).getDbEntity();
    }

    /**
     * @since 1.2
     */
    public EntityResolver getEntityResolver() {
        return entityResolver;
    }

    /**
     * @since 1.2
     */
    public void setEntityResolver(EntityResolver entityResolver) {
        this.entityResolver = entityResolver;
    }
}
