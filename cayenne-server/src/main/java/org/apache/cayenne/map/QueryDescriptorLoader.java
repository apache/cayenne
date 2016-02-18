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

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.query.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @since 4.0
 */
public class QueryDescriptorLoader {

    protected String name;
    protected String queryType;
    protected String sql;
    protected String ejbql;
    protected Expression qualifier;
    protected DataMap dataMap;
    protected String rootType;
    protected String rootName;
    protected String resultEntity;

    protected List<Ordering> orderings = new ArrayList<>();
    protected List<String> prefetches = new ArrayList<>();
    protected Map<String, String> adapterSql = new HashMap<>();
    protected Map<String, String> properties = new HashMap<>();

    /**
     * Builds a Query object based on internal configuration information.
     */
    public QueryDescriptor buildQueryDescriptor() {
        QueryDescriptor descriptor = QueryDescriptor.descriptor(queryType);

        descriptor.setName(name);
        descriptor.setDataMap(dataMap);
        descriptor.setRoot(getRoot());
        descriptor.setProperties(properties);

        switch (queryType) {
            case QueryDescriptor.SELECT_QUERY:
                ((SelectQueryDescriptor) descriptor).setQualifier(qualifier);
                ((SelectQueryDescriptor) descriptor).setOrderings(orderings);
                ((SelectQueryDescriptor) descriptor).setPrefetches(prefetches);
                break;
            case QueryDescriptor.SQL_TEMPLATE:
                ((SQLTemplateDescriptor) descriptor).setSql(sql);
                ((SQLTemplateDescriptor) descriptor).setAdapterSql(adapterSql);
                break;
            case QueryDescriptor.EJBQL_QUERY:
                ((EJBQLQueryDescriptor) descriptor).setEjbql(ejbql);
                break;
            case QueryDescriptor.PROCEDURE_QUERY:
                ((ProcedureQueryDescriptor) descriptor).setResultEntityName(resultEntity);
                break;
            default:
                // no additional properties
        }

        return descriptor;
    }

    void setName(String name) {
        this.name = name;
    }

    void setQueryType(String queryType) {
        this.queryType = queryType;
    }

    /**
     * Determines query root based on configuration info, falls back to a DataMap root if
     * the data is invalid.
     *
     * @throws CayenneRuntimeException if a valid root can't be established.
     */
    protected Object getRoot() {

        Object root = null;

        if (rootType == null
                || MapLoader.DATA_MAP_ROOT.equals(rootType)
                || rootName == null) {
            root = dataMap;
        }
        else if (MapLoader.OBJ_ENTITY_ROOT.equals(rootType)) {
            root = dataMap.getObjEntity(rootName);
        }
        else if (MapLoader.DB_ENTITY_ROOT.equals(rootType)) {
            root = dataMap.getDbEntity(rootName);
        }
        else if (MapLoader.PROCEDURE_ROOT.equals(rootType)) {
            root = dataMap.getProcedure(rootName);
        }
        else if (MapLoader.JAVA_CLASS_ROOT.equals(rootType)) {
            // setting root to ObjEntity, since creating a Class requires
            // the knowledge of the ClassLoader
            root = dataMap.getObjEntityForJavaClass(rootName);
        }

        return (root != null) ? root : dataMap;
    }

    void setResultEntity(String resultEntity) {
        this.resultEntity = resultEntity;
    }

    /**
     * Sets the information pertaining to the root of the query.
     */
    void setRoot(DataMap dataMap, String rootType, String rootName) {
        this.dataMap = dataMap;
        this.rootType = rootType;
        this.rootName = rootName;
    }

    void setEjbql(String ejbql) {
        this.ejbql = ejbql;
    }

    /**
     * Adds raw sql. If adapterClass parameter is not null, sets the SQL string to be
     * adapter-specific. Otherwise it is used as a default SQL string.
     */
    void addSql(String sql, String adapterClass) {
        if (adapterClass == null) {
            this.sql = sql;
        }
        else {
            if (adapterSql == null) {
                adapterSql = new HashMap<>();
            }

            adapterSql.put(adapterClass, sql);
        }
    }

    void setQualifier(String qualifier) {
        if (qualifier == null || qualifier.trim().length() == 0) {
            this.qualifier = null;
        }
        else {
            this.qualifier = Expression.fromString(qualifier.trim());
        }
    }

    void addProperty(String name, String value) {
        if (properties == null) {
            properties = new HashMap<>();
        }

        properties.put(name, value);
    }

    void addOrdering(String path, String descending, String ignoreCase) {
        if (orderings == null) {
            orderings = new ArrayList<Ordering>();
        }

        if (path != null && path.trim().length() == 0) {
            path = null;
        }
        boolean isDescending = "true".equalsIgnoreCase(descending);
        boolean isIgnoringCase = "true".equalsIgnoreCase(ignoreCase);

        SortOrder order;

        if (isDescending) {
            order = isIgnoringCase
                    ? SortOrder.DESCENDING_INSENSITIVE
                    : SortOrder.DESCENDING;
        }
        else {
            order = isIgnoringCase
                    ? SortOrder.ASCENDING_INSENSITIVE
                    : SortOrder.ASCENDING;
        }

        orderings.add(new Ordering(path, order));
    }

    void addPrefetch(String path) {
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
