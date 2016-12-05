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
import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.configuration.ConfigurationNodeVisitor;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.util.XMLEncoder;
import org.apache.cayenne.util.XMLSerializable;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Generic descriptor of a Cayenne query.
 *
 * @since 4.0
 */
public class QueryDescriptor implements Serializable, ConfigurationNode, XMLSerializable {

    public static final String SELECT_QUERY = "SelectQuery";
    public static final String SQL_TEMPLATE = "SQLTemplate";
    public static final String EJBQL_QUERY = "EJBQLQuery";
    public static final String PROCEDURE_QUERY = "ProcedureQuery";

    /**
     * Creates new SelectQuery query descriptor.
     */
    public static SelectQueryDescriptor selectQueryDescriptor() {
        return new SelectQueryDescriptor();
    }

    /**
     * Creates new SQLTemplate query descriptor.
     */
    public static SQLTemplateDescriptor sqlTemplateDescriptor() {
        return new SQLTemplateDescriptor();
    }

    /**
     * Creates new ProcedureQuery query descriptor.
     */
    public static ProcedureQueryDescriptor procedureQueryDescriptor() {
        return new ProcedureQueryDescriptor();
    }

    /**
     * Creates new EJBQLQuery query descriptor.
     */
    public static EJBQLQueryDescriptor ejbqlQueryDescriptor() {
        return new EJBQLQueryDescriptor();
    }

    /**
     * Creates query descriptor of a given type.
     */
    public static QueryDescriptor descriptor(String type) {
        switch (type) {
            case SELECT_QUERY:
                return selectQueryDescriptor();
            case SQL_TEMPLATE:
                return sqlTemplateDescriptor();
            case EJBQL_QUERY:
                return ejbqlQueryDescriptor();
            case PROCEDURE_QUERY:
                return procedureQueryDescriptor();
            default:
                QueryDescriptor descriptor = new QueryDescriptor(type);
                return descriptor;
        }
    }

    protected String name;
    protected String type;
    protected DataMap dataMap;
    protected Object root;

    protected Map<String, String> properties = new HashMap<>();

    protected QueryDescriptor(String type) {
        this.type = type;
    }

    /**
     * Returns name of the query.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets name of the query.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns type of the query.
     */
    public String getType() {
        return type;
    }

    /**
     * Sets type of the query.
     */
    public void setType(String type) {
        this.type = type;
    }

    public DataMap getDataMap() {
        return dataMap;
    }

    public void setDataMap(DataMap dataMap) {
        this.dataMap = dataMap;
    }

    /**
     * Returns the root of this query.
     */
    public Object getRoot() {
        return root;
    }

    /**
     * Sets the root of this query.
     */
    public void setRoot(Object root) {
        this.root = root;
    }

    /**
     * Returns map of query properties set up for this query.
     */
    public Map<String, String> getProperties() {
        return properties;
    }

    /**
     * Returns query property by its name.
     */
    public String getProperty(String name) {
        return properties.get(name);
    }

    /**
     * Sets map of query properties for this query.
     */
    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    /**
     * Sets single query property.
     */
    public void setProperty(String name, String value) {
        this.properties.put(name, value);
    }

    /**
     * Assembles Cayenne query instance of appropriate type from this descriptor.
     */
    public Query buildQuery() {
        throw new CayenneRuntimeException("Unable to build query object of this type.");
    }

    @Override
    public <T> T acceptVisitor(ConfigurationNodeVisitor<T> visitor) {
        return visitor.visitQuery(this);
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
        }

        if (rootType != null) {
            encoder.print("\" root=\"");
            encoder.print(rootType);
            encoder.print("\" root-name=\"");
            encoder.print(rootString);
        }

        encoder.println("\">");

        encoder.indent(1);

        encodeProperties(encoder);

        encoder.indent(-1);
        encoder.println("</query>");
    }

    void encodeProperties(XMLEncoder encoder) {
        for (Map.Entry<String, String> property : properties.entrySet()) {
            String value = property.getValue();
            if(value == null || value.isEmpty()) {
                continue;
            }
            encoder.printProperty(property.getKey(), value);
        }
    }
}
