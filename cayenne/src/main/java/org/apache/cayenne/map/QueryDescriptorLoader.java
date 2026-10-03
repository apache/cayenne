/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
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
import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.ql.JavaCharStream;
import org.apache.cayenne.ql.QLParser;
import org.apache.cayenne.ql.QLParserTokenManager;
import org.apache.cayenne.query.FluentSelect;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.query.PrefetchTreeNode;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.cayenne.util.Util.isBlank;

/**
 * A builder that constructs Cayenne queries from abstract configuration information
 * defined in cayenne-data-map*.dtd. This abstract builder supports values declared in the
 * DTD, allowing subclasses to define their own Query creation logic.
 *
 * @since 4.0
 */
public class QueryDescriptorLoader {

    protected String name;
    protected String queryType;
    protected String sql;
    protected Expression qualifier;
    protected DataMap dataMap;
    protected String rootType;
    protected String rootName;
    protected String resultEntity;
    protected String columnNameCapitalization;

    protected List<Ordering> orderings = new ArrayList<>();
    protected HashMap<String, Integer> prefetchesMap = new HashMap<>();
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
                ((SelectQueryDescriptor) descriptor).setPrefetchesMap(prefetchesMap);
                break;
            case QueryDescriptor.SQL_TEMPLATE:
                ((SQLTemplateDescriptor) descriptor).setSql(sql);
                ((SQLTemplateDescriptor) descriptor).setPrefetchesMap(prefetchesMap);
                ((SQLTemplateDescriptor) descriptor).setAdapterSql(adapterSql);
                descriptor.setProperty(SQLTemplateDescriptor.COLUMN_NAME_CAPITALIZATION_PROPERTY,
                        columnNameCapitalization);
                break;
            case QueryDescriptor.PROCEDURE_QUERY:
                ((ProcedureQueryDescriptor) descriptor).setResultEntityName(resultEntity);
                descriptor.setProperty(ProcedureQueryDescriptor.COLUMN_NAME_CAPITALIZATION_PROPERTY,
                        columnNameCapitalization);
                break;
            default:
                // no additional properties
        }

        return descriptor;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setQueryType(String queryType) {
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
                || QueryDescriptor.DATA_MAP_ROOT.equals(rootType)
                || rootName == null) {
            root = dataMap;
        }
        else if (QueryDescriptor.OBJ_ENTITY_ROOT.equals(rootType)) {
            root = dataMap.getObjEntity(rootName);
        }
        else if (QueryDescriptor.DB_ENTITY_ROOT.equals(rootType)) {
            root = dataMap.getDbEntity(rootName);
        }
        else if (QueryDescriptor.PROCEDURE_ROOT.equals(rootType)) {
            root = dataMap.getProcedure(rootName);
        }
        else if (QueryDescriptor.JAVA_CLASS_ROOT.equals(rootType)) {
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
     * Sets the capitalization of the result column names of a SQLTemplate or a ProcedureQuery.
     *
     * @since 5.0
     */
    public void setColumnNameCapitalization(String columnNameCapitalization) {
        this.columnNameCapitalization = columnNameCapitalization;
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
                adapterSql = new HashMap<>();
            }

            adapterSql.put(adapterClass, sql);
        }
    }

    /**
     * Sets the select query from its String form, in the syntax of {@link ObjectSelect#parse(String, Object...)}.
     * The String defines the root entity, qualifier, orderings, prefetches, limit, offset and "distinct" of the query.
     * "$name" parameters in the String are preserved as named parameters, to be bound when the query is executed.
     *
     * @since 5.0
     */
    public void setSelect(String select) {
        if (select == null || isBlank(select)) {
            return;
        }

        // TODO: the parsed query can be a ColumnSelect, have a "having" clause or be rooted in a DbEntity, none of
        //  which is representable in SelectQueryDescriptor, or editable in the Modeler. Both need to be reworked
        //  to hold a full query instead of its parts, and MappedSelect should stop assuming an ObjectSelect
        FluentSelect<?, ?> parsed = parseSelect(select);
        if (!(parsed instanceof ObjectSelect<?> query)) {
            throw new ConfigurationException("Query '%s' selects columns, which is not supported in a mapped query: %s",
                    name, select);
        }
        if (query.getHaving() != null) {
            throw new ConfigurationException("Query '%s' has a 'having' clause, which is not supported in a mapped " +
                    "query: %s", name, select);
        }
        if (query.getEntityName() == null) {
            throw new ConfigurationException("Query '%s' must be rooted in an ObjEntity: %s", name, select);
        }

        this.rootType = QueryDescriptor.OBJ_ENTITY_ROOT;
        this.rootName = query.getEntityName();
        this.qualifier = query.getWhere();
        this.orderings = query.getOrderings() != null ? new ArrayList<>(query.getOrderings()) : new ArrayList<>();

        PrefetchTreeNode prefetches = query.getPrefetches();
        if (prefetches != null) {
            for (PrefetchTreeNode node : prefetches.nonPhantomNodes()) {
                // the root of the tree is not a prefetch
                if (node.getParent() != null) {
                    addPrefetch(node.getPath().value(), node.getSemantics());
                }
            }
        }

        if (query.getLimit() > 0) {
            addProperty(QueryDescriptor.FETCH_LIMIT_PROPERTY, String.valueOf(query.getLimit()));
        }
        if (query.getOffset() > 0) {
            addProperty(QueryDescriptor.FETCH_OFFSET_PROPERTY, String.valueOf(query.getOffset()));
        }
        if (query.isDistinct()) {
            addProperty(SelectQueryDescriptor.DISTINCT_PROPERTY, "true");
        }
    }

    /**
     * Parses a query String without binding its "$name" parameters, unlike
     * {@link ObjectSelect#parse(String, Object...)} that requires the values of all the parameters upfront.
     */
    private FluentSelect<?, ?> parseSelect(String select) {
        JavaCharStream stream = new JavaCharStream(new StringReader(select), 1, 1, select.length() + 1);
        QLParser parser = new QLParser(new QLParserTokenManager(stream));
        try {
            return parser.query();
        } catch (Throwable th) {
            throw new ConfigurationException("Query '%s' can't be parsed: %s", th, name, th.getMessage());
        }
    }

    public void addProperty(String name, String value) {
        if (properties == null) {
            properties = new HashMap<>();
        }

        properties.put(name, value);
    }

    public void addPrefetch(String path, int semantics) {
        if (path == null || isBlank(path)) {
            // throw??
            return;
        }

        if (prefetchesMap == null) {
            prefetchesMap = new HashMap<>();
        }

        prefetchesMap.put(path.trim(), semantics);
    }
}
