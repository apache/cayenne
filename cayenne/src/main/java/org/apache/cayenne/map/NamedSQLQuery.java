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

import org.apache.cayenne.project.ProjectNodeVisitor;
import org.apache.cayenne.query.CapsStrategy;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.query.QueryCacheStrategy;
import org.apache.cayenne.util.XMLEncoder;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

/**
 * @since 4.0
 */
public final class NamedSQLQuery extends NamedQuery {

    private String sql;
    private Map<String, Integer> prefetchesMap;
    private Map<String, String> adapterSql;
    private CapsStrategy columnNamesCapitalization;

    public NamedSQLQuery() {
        this.prefetchesMap = new HashMap<>();
        this.adapterSql = new HashMap<>();
    }

    /**
     * Returns default SQL statement for this query.
     */
    public String getSql() {
        return sql;
    }

    /**
     * Sets default SQL statement for this query.
     */
    public void setSql(String sql) {
        this.sql = sql;
    }

    /**
     * Returns map of db adapter specific SQL statements.
     */
    public Map<String, String> getAdapterSql() {
        return adapterSql;
    }

    /**
     * Sets a map db adapter specific SQL statements for this query.
     */
    public void setAdapterSql(Map<String, String> adapterSql) {
        this.adapterSql = adapterSql;
    }

    /**
     * Returns the capitalization strategy applied to the column names of the query result, or null if none is set.
     *
     * @since 5.0
     */
    public CapsStrategy getColumnNamesCapitalization() {
        return columnNamesCapitalization;
    }

    /**
     * @since 5.0
     */
    public void setColumnNamesCapitalization(CapsStrategy columnNamesCapitalization) {
        this.columnNamesCapitalization = columnNamesCapitalization;
    }

    /**
     * Returns map of prefetch paths with semantics for this query.
     * @since 4.1
     */
    public Map<String, Integer> getPrefetchesMap() { return prefetchesMap; }

    /**
     * Sets map of prefetch paths with semantics for this query.
     * @since 4.1
     */
    public void setPrefetchesMap(HashMap<String, Integer> prefetchesMap) {
        this.prefetchesMap = prefetchesMap;
    }

    /**
     * Adds single prefetch path with semantics to this query.
     * @since 4.1
     */
    public void addPrefetch(String prefetchPath, int semantics){
        this.prefetchesMap.put(prefetchPath, semantics);
    }

    /**
     * Removes single prefetch path from this query.
     * @since 4.1
     */
    public void removePrefetch(String prefetchPath) {
        this.prefetchesMap.remove(prefetchPath);
    }

    @Override
    public void encodeAsXML(XMLEncoder encoder, ProjectNodeVisitor<?> delegate) {
        encoder.start("sqlQuery").attribute("name", getName());

        String rootString = null;
        String rootType = null;

        if (root instanceof String) {
            rootType = NamedQuery.OBJ_ENTITY_ROOT;
            rootString = root.toString();
        } else if (root instanceof ObjEntity) {
            rootType = NamedQuery.OBJ_ENTITY_ROOT;
            rootString = ((ObjEntity) root).getName();
        } else if (root instanceof DataMap) {
            rootType = NamedQuery.DATA_MAP_ROOT;
            rootString = ((DataMap) root).getName();
        }

        if (rootType != null) {
            encoder.attribute("root", rootType).attribute("rootName", rootString);
        }

        encoder.attribute("cacheStrategy", cacheStrategy != QueryCacheStrategy.NO_CACHE ? cacheStrategy.name() : null)
                .attribute("dataRows", fetchingDataRows)
                .attribute("pageSize", pageSize)
                .attribute("statementFetchSize", statementFetchSize)
                .attribute("columnNameCapitalization",
                        columnNamesCapitalization != null && columnNamesCapitalization != CapsStrategy.DEFAULT
                                ? columnNamesCapitalization.name()
                                : null);

        // encode default SQL
        if (sql != null) {
            encoder.start("sql").cdata(sql, true).end();
        }

        // encode adapter SQL
        if (adapterSql != null && !adapterSql.isEmpty()) {
            // sorting entries by adapter name
            TreeSet<String> keys = new TreeSet<>(adapterSql.keySet());
            for (String key : keys) {
                String value = adapterSql.get(key);
                if (key != null && value != null) {
                    String sql = value.trim();
                    if (!sql.isEmpty()) {
                        encoder.start("sql")
                                .attribute("adapterClass", key)
                                .cdata(sql, true)
                                .end();
                    }
                }
            }
        }

        encodeCacheGroup(encoder);

        PrefetchTreeNode prefetchTree = new PrefetchTreeNode();

        for (Map.Entry<String, Integer> entry : prefetchesMap.entrySet()) {
            PrefetchTreeNode node = prefetchTree.addPath(entry.getKey());
            node.setSemantics(entry.getValue());
            node.setPhantom(false);
        }

        encoder.nested(prefetchTree, delegate);

        delegate.visitQuery(this);
        encoder.end();
    }
}
