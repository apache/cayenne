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
package org.apache.cayenne.map;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.query.Query;

/**
 * A builder that constructs Cayenne queries from abstract configuration information
 * defined in cayenne-data-map*.dtd. This abstract builder supports values declared in the
 * DTD, allowing subclasses to define their own Query creation logic.
 * 
 * @since 1.1
 * @deprecated since 3.0 replaced by a non-public class.
 */
public abstract class QueryBuilder {

    public static final String OBJ_ENTITY_ROOT = "obj-entity";
    public static final String DB_ENTITY_ROOT = "db-entity";
    public static final String PROCEDURE_ROOT = "procedure";
    public static final String DATA_MAP_ROOT = "data-map";
    public static final String JAVA_CLASS_ROOT = "java-class";

    protected String name;
    protected Map<String, String> properties;
    protected String sql;
    protected Map<String, String> adapterSql;
    protected Expression qualifier;
    protected List<Ordering> orderings;
    protected List<String> prefetches;
    protected DataMap dataMap;
    protected String rootType;
    protected String rootName;
    protected String resultEntity;

    /**
     * Builds a Query object based on internal configuration information.
     */
    public abstract Query getQuery();

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Determines query root based on configuration info, falls back to a DataMap root if
     * the data is invalid.
     * 
     * @throws CayenneRuntimeException if a valid root can't be established.
     */
    protected Object getRoot() {

        Object root = null;

        if (rootType == null || DATA_MAP_ROOT.equals(rootType) || rootName == null) {
            root = dataMap;
        }
        else if (OBJ_ENTITY_ROOT.equals(rootType)) {
            root = dataMap.getObjEntity(rootName);
        }
        else if (DB_ENTITY_ROOT.equals(rootType)) {
            root = dataMap.getDbEntity(rootName);
        }
        else if (PROCEDURE_ROOT.equals(rootType)) {
            root = dataMap.getProcedure(rootName);
        }
        else if (JAVA_CLASS_ROOT.equals(rootType)) {
            // setting root to ObjEntity, since creating a Class requires
            // the knowledge of the ClassLoader
            root = dataMap.getObjEntityForJavaClass(rootName);
        }

        return (root != null) ? root : dataMap;
    }

    public void setResultEntity(String resultEntity) {
        this.resultEntity = resultEntity;
    }

    /**
     * Sets the information pertaining to the root of the query.
     */
    public void setRoot(DataMap dataMap, String rootType, String rootName) {
        this.dataMap = dataMap;
        this.rootType = rootType;
        this.rootName = rootName;
    }

    /**
     * Adds raw sql. If adapterClass parameter is not null, sets the SQL string to be
     * adapter-specific. Otherwise it is used as a default SQL string.
     */
    public void addSql(String sql, String adapterClass) {
        if (adapterClass == null) {
            this.sql = sql;
        }
        else {
            if (adapterSql == null) {
                adapterSql = new HashMap<String, String>();
            }

            adapterSql.put(adapterClass, sql);
        }
    }

    public void setQualifier(String qualifier) {
        if (qualifier == null || qualifier.trim().length() == 0) {
            this.qualifier = null;
        }
        else {
            this.qualifier = Expression.fromString(qualifier.trim());
        }
    }

    public void addProperty(String name, String value) {
        if (properties == null) {
            properties = new HashMap<String, String>();
        }

        properties.put(name, value);
    }

    public void addOrdering(String path, String descending, String ignoreCase) {
        if (orderings == null) {
            orderings = new ArrayList<Ordering>();
        }

        if (path != null && path.trim().length() == 0) {
            path = null;
        }
        boolean isDescending = "true".equalsIgnoreCase(descending);
        boolean isIgnoringCase = "true".equalsIgnoreCase(ignoreCase);
        orderings.add(new Ordering(path, !isDescending, isIgnoringCase));
    }

    public void addPrefetch(String path) {
        if (path == null || (path != null && path.trim().length() == 0)) {
            // throw??
            return;
        }

        if (prefetches == null) {
            prefetches = new ArrayList<String>();
        }
        prefetches.add(path.trim());
    }
}
