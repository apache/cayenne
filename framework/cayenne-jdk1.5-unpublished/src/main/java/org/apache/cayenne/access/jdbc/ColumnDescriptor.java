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

import java.io.Serializable;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import org.apache.cayenne.dba.QuotingStrategy;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ProcedureParameter;
import org.apache.cayenne.util.EqualsBuilder;
import org.apache.cayenne.util.HashCodeBuilder;
import org.apache.cayenne.util.ToStringBuilder;

/**
 * A descriptor of a ResultSet column.
 * 
 * @since 1.1
 */
public class ColumnDescriptor implements Serializable {

    protected String tableName;
    protected String procedureName;

    // identifies column in result set
    protected String name;
    protected String namePrefix;

    // identifies column in a DataRow
    protected String dataRowKey;

    protected int jdbcType;
    protected String javaClass;

    /**
     * Creates a ColumnDescriptor
     */
    public ColumnDescriptor() {
    }

    /**
     * Creates a column descriptor with user-specified parameters.
     * 
     * @since 1.2
     */
    public ColumnDescriptor(String columnName, int jdbcType, String javaClass) {
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
        this.name = attribute.getName();
        this.namePrefix = tableAlias;
        this.dataRowKey = name;
        this.jdbcType = attribute.getType();
        this.javaClass = getDefaultJavaClass(attribute.getMaxLength(), attribute
                .getScale());

        if (attribute.getEntity() != null) {
            this.tableName = attribute.getEntity().getName();
        }
    }

    /**
     * @since 1.2
     */
    public ColumnDescriptor(ObjAttribute objAttribute, DbAttribute dbAttribute,
            String columnAlias) {
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
        this.name = parameter.getName();
        this.dataRowKey = name;
        this.jdbcType = parameter.getType();
        this.javaClass = getDefaultJavaClass(parameter.getMaxLength(), parameter
                .getPrecision());

        if (parameter.getProcedure() != null) {
            this.procedureName = parameter.getProcedure().getName();
        }
    }

    /**
     * Creates a ColumnDescriptor using ResultSetMetaData.
     * 
     * @since 1.2
     */
    public ColumnDescriptor(ResultSetMetaData metaData, int position) throws SQLException {
        String name = metaData.getColumnLabel(position);
        if (name == null || name.length() == 0) {
            name = metaData.getColumnName(position);

            if (name == null || name.length() == 0) {
                name = "column_" + position;
            }
        }

        this.name = name;
        this.dataRowKey = name;
        this.jdbcType = metaData.getColumnType(position);
        this.javaClass = metaData.getColumnClassName(position);
    }

    /**
     * Returns true if another object is a ColumnDescriptor with the same name, name
     * prefix, table and procedure names. Other fields are ignored in the equality test.
     * 
     * @since 1.2
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ColumnDescriptor)) {
            return false;
        }

        ColumnDescriptor rhs = (ColumnDescriptor) o;
        return new EqualsBuilder().append(name, rhs.name).append(
                namePrefix,
                rhs.namePrefix).append(procedureName, rhs.procedureName).append(
                dataRowKey,
                rhs.dataRowKey).append(tableName, rhs.tableName).isEquals();
    }

    /**
     * @since 1.2
     */
    @Override
    public int hashCode() {
        return new HashCodeBuilder(23, 43).append(name).append(namePrefix).append(
                procedureName).append(tableName).append(dataRowKey).toHashCode();
    }

    /**
     * @since 1.2
     */
    @Override
    public String toString() {
        ToStringBuilder builder = new ToStringBuilder(this);
        builder.append("namePrefix", namePrefix);
        builder.append("name", getName());
        builder.append("dataRowKey", getDataRowKey());
        builder.append("tableName", getTableName());
        builder.append("procedureName", getProcedureName());
        builder.append("javaClass", getJavaClass());
        builder.append("jdbcType", getJdbcType());
        return builder.toString();
    }

    /**
     * Returns a default Java class for an internal JDBC type.
     * 
     * @since 1.2
     */
    public String getDefaultJavaClass(int size, int scale) {
        return TypesMapping.getJavaBySqlType(getJdbcType(), size, scale);
    }

    /**
     * Returns "qualifiedColumnName" property.
     * 
     * @since 1.2
     */
    public String getQualifiedColumnName() {
        return (namePrefix != null) ? namePrefix + '.' + name : name;
    }
    
    public String getQualifiedColumnNameWithQuoteSqlIdentifiers(QuotingStrategy strategy) {
        String nameWithQuoteSqlIdentifiers = strategy.quoteString( name );
        return (namePrefix != null) ? strategy.quoteString( namePrefix ) + '.' +
                nameWithQuoteSqlIdentifiers: nameWithQuoteSqlIdentifiers;
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
     * Returns the name of the parent table.
     * 
     * @since 1.2
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * @since 1.2
     */
    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    /**
     * Returns the name of the parent stored procedure.
     * 
     * @since 1.2
     */
    public String getProcedureName() {
        return procedureName;
    }

    /**
     * @since 1.2
     */
    public void setProcedureName(String procedureName) {
        this.procedureName = procedureName;
    }

    /**
     * Returns "label" used in a DataRow for column value.
     * 
     * @since 1.2
     * @deprecated since 3.0 use {@link #getDataRowKey()}
     */
    public String getLabel() {
        return getDataRowKey();
    }

    /**
     * @since 1.2
     * @deprecated since 3.0 use {@link #setDataRowKey(String)}.
     */
    public void setLabel(String label) {
        setDataRowKey(label);
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
}
