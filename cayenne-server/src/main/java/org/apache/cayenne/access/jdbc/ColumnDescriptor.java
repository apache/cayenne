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

package org.apache.cayenne.access.jdbc;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ProcedureParameter;
import org.apache.cayenne.util.EqualsBuilder;
import org.apache.cayenne.util.HashCodeBuilder;
import org.apache.cayenne.util.ToStringBuilder;

import static org.apache.commons.lang.StringUtils.isBlank;

/**
 * A descriptor of a ResultSet column.
 * 
 * @since 1.1
 */
public class ColumnDescriptor {

    private DbAttribute attribute;

    // identifies column in result set
    private String name;
    private String namePrefix;

    // identifies column in a DataRow
    private String dataRowKey;

    private int jdbcType;
    private String javaClass;

    /**
     * Creates a ColumnDescriptor
     */
    public ColumnDescriptor() {
    }

    /**
     * Creates a column descriptor with user-specified parameters.
     * 
     * @since 3.1
     */
    public ColumnDescriptor(String columnName, int jdbcType) {
        this.name = columnName;
        this.dataRowKey = columnName;
        this.jdbcType = jdbcType;
        this.javaClass = TypesMapping.getJavaBySqlType(jdbcType);
    }

    /**
     * Creates a ColumnDescriptor from Cayenne DbAttribute.
     * 
     * @since 1.2
     */
    public ColumnDescriptor(DbAttribute attribute, String tableAlias) {
        this(attribute.getName(), attribute.getType());

        this.attribute = attribute;
        this.namePrefix = tableAlias;
    }

    /**
     * @since 1.2
     */
    public ColumnDescriptor(ObjAttribute objAttribute, DbAttribute dbAttribute, String columnAlias) {
        this(dbAttribute, columnAlias);
        this.dataRowKey = objAttribute.getDbAttributePath();
        this.javaClass = objAttribute.getType();
    }

    /**
     * Creates a ColumnDescriptor from stored procedure parameter.
     * 
     * @since 1.2
     */
    public ColumnDescriptor(ProcedureParameter parameter) {
        this(parameter.getName(), parameter.getType());
    }

    /**
     * Creates a ColumnDescriptor using ResultSetMetaData.
     * 
     * @since 3.1
     */
    public ColumnDescriptor(String columnName, ResultSetMetaData metaData, int position) throws SQLException {
        this(columnName, metaData.getColumnType(position));
    }

    /**
     * Creates a ColumnDescriptor using ResultSetMetaData.
     * 
     * @since 1.2
     */
    public ColumnDescriptor(ResultSetMetaData metaData, int position) throws SQLException {
        this(getColumnNameFromMeta(metaData, position), metaData, position);
    }

    private static String getColumnNameFromMeta(ResultSetMetaData metaData, int position) throws SQLException {
        String name = metaData.getColumnLabel(position);
        if (isBlank(name)) {
            name = metaData.getColumnName(position);

            if (isBlank(name)) {
                name = "column_" + position;
            }
        }
        return name;
    }

    /**
     * Returns true if another object is a ColumnDescriptor with the same name,
     * name prefix, table and procedure names. Other fields are ignored in the
     * equality test.
     * 
     * @since 1.2
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ColumnDescriptor)) {
            return false;
        }

        ColumnDescriptor rhs = (ColumnDescriptor) o;
        return new EqualsBuilder()
                .append(name, rhs.name)
                .append(namePrefix, rhs.namePrefix)
                .append(dataRowKey, rhs.dataRowKey)
                .isEquals();
    }

    /**
     * @since 1.2
     */
    @Override
    public int hashCode() {
        return new HashCodeBuilder(23, 43)
                .append(name)
                .append(namePrefix)
                .append(dataRowKey).toHashCode();
    }

    /**
     * @since 1.2
     */
    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("namePrefix", namePrefix)
                .append("name", getName())
                .append("dataRowKey", getDataRowKey())
                .append("javaClass", getJavaClass())
                .append("jdbcType", getJdbcType())
                .toString();
    }

    /**
     * Returns "qualifiedColumnName" property.
     * 
     * @since 1.2
     */
    public String getQualifiedColumnName() {
        return namePrefix != null ? namePrefix + '.' + name : name;
    }

    public int getJdbcType() {
        return jdbcType;
    }

    /**
     * Returns column name. Name is an unqualified column name in a query.
     */
    public String getName() {
        return name;
    }

    /**
     * @since 4.0
     */
    public String getNamePrefix() {
        return namePrefix;
    }

    public void setJdbcType(int i) {
        jdbcType = i;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getJavaClass() {
        return javaClass;
    }

    public void setJavaClass(String string) {
        javaClass = string;
    }

    /**
     * @since 3.0
     */
    public String getDataRowKey() {
        return dataRowKey != null ? dataRowKey : getName();
    }

    /**
     * @since 3.0
     */
    public void setDataRowKey(String dataRowKey) {
        this.dataRowKey = dataRowKey;
    }

    public void setNamePrefix(String namePrefix) {
        this.namePrefix = namePrefix;
    }

    public DbAttribute getAttribute() {
        return attribute;
    }
}
