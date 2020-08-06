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

import org.apache.cayenne.configuration.ConfigurationNodeVisitor;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.util.XMLEncoder;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

/**
 * @since 4.0
 */
public class SQLTemplateDescriptor extends QueryDescriptor {

    protected String sql;
    protected Map<String, Integer> prefetchesMap = new HashMap<>();

    protected Map<String, String> adapterSql = new HashMap<>();

    public SQLTemplateDescriptor() {
        super(SQL_TEMPLATE);
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
    public SQLTemplate buildQuery() {
        SQLTemplate template = new SQLTemplate();

        if (root != null) {
            template.setRoot(root);
        }

        if (prefetchesMap != null) {
            for (Map.Entry<String, Integer> entry : prefetchesMap.entrySet()) {
                template.addPrefetch(PrefetchTreeNode.withPath(entry.getKey(), entry.getValue()));
            }
        }

        template.initWithProperties(this.getProperties());

        // init SQL
        template.setDefaultTemplate(this.getSql());

        Map<String, String> adapterSql = this.getAdapterSql();

        if (adapterSql != null) {
            for (Map.Entry<String, String> entry : adapterSql.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (key != null && value != null) {
                    template.setTemplate(key, value);
                }
            }
        }

        return template;
    }

    @Override
    public void encodeAsXML(XMLEncoder encoder, ConfigurationNodeVisitor delegate) {
        encoder.start("query")
                .attribute("name", getName())
                .attribute("type", type);

        String rootString = null;
        String rootType = null;

        if (root instanceof String) {
            rootType = QueryDescriptor.OBJ_ENTITY_ROOT;
            rootString = root.toString();
        } else if (root instanceof ObjEntity) {
            rootType = QueryDescriptor.OBJ_ENTITY_ROOT;
            rootString = ((ObjEntity) root).getName();
        } else if (root instanceof DbEntity) {
            rootType = QueryDescriptor.DB_ENTITY_ROOT;
            rootString = ((DbEntity) root).getName();
        } else if (root instanceof Procedure) {
            rootType = QueryDescriptor.PROCEDURE_ROOT;
            rootString = ((Procedure) root).getName();
        } else if (root instanceof Class<?>) {
            rootType = QueryDescriptor.JAVA_CLASS_ROOT;
            rootString = ((Class<?>) root).getName();
        } else if (root instanceof DataMap) {
            rootType = QueryDescriptor.DATA_MAP_ROOT;
            rootString = ((DataMap) root).getName();
        }

        if (rootType != null) {
            encoder.attribute("root", rootType).attribute("root-name", rootString);
        }

        // print properties
        encodeProperties(encoder);
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
                    if (sql.length() > 0) {
                        encoder.start("sql")
                                .attribute("adapter-class", key)
                                .cdata(sql, true)
                                .end();
                    }
                }
            }
        }

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
