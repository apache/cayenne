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

    @Override
    public SQLTemplate buildQuery() {
        SQLTemplate template = new SQLTemplate();

        if (root != null) {
            template.setRoot(root);
        }

        template.setName(name);
        template.setDataMap(dataMap);
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
    public void encodeAsXML(XMLEncoder encoder) {
        encoder.print("<query name=\"");
        encoder.print(getName());
        encoder.print("\" type=\"");
        encoder.print(type);

        String rootString = null;
        String rootType = null;

        if (root instanceof String) {
            rootType = MapLoader.OBJ_ENTITY_ROOT;
            rootString = root.toString();
        } else if (root instanceof ObjEntity) {
            rootType = MapLoader.OBJ_ENTITY_ROOT;
            rootString = ((ObjEntity) root).getName();
        } else if (root instanceof DbEntity) {
            rootType = MapLoader.DB_ENTITY_ROOT;
            rootString = ((DbEntity) root).getName();
        } else if (root instanceof Procedure) {
            rootType = MapLoader.PROCEDURE_ROOT;
            rootString = ((Procedure) root).getName();
        } else if (root instanceof Class<?>) {
            rootType = MapLoader.JAVA_CLASS_ROOT;
            rootString = ((Class<?>) root).getName();
        } else if (root instanceof DataMap) {
            rootType = MapLoader.DATA_MAP_ROOT;
            rootString = ((DataMap) root).getName();
        }

        if (rootType != null) {
            encoder.print("\" root=\"");
            encoder.print(rootType);
            encoder.print("\" root-name=\"");
            encoder.print(rootString);
        }

        encoder.println("\">");

        encoder.indent(1);

        // print properties
        encodeProperties(encoder);

        // encode default SQL
        if (sql != null) {
            encoder.print("<sql><![CDATA[");
            encoder.print(sql);
            encoder.println("]]></sql>");
        }

        // encode adapter SQL
        if (adapterSql != null && !adapterSql.isEmpty()) {

            // sorting entries by adapter name
            TreeSet<String> keys = new TreeSet<String>(adapterSql.keySet());
            for (String key : keys) {
                String value = adapterSql.get(key);

                if (key != null && value != null) {
                    String sql = value.trim();
                    if (sql.length() > 0) {
                        encoder.print("<sql adapter-class=\"");
                        encoder.print(key);
                        encoder.print("\"><![CDATA[");
                        encoder.print(sql);
                        encoder.println("]]></sql>");
                    }
                }
            }
        }

        encoder.indent(-1);
        encoder.println("</query>");
    }
}
