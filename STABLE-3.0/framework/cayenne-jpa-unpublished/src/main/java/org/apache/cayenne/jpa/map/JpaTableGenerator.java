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

package org.apache.cayenne.jpa.map;

import java.util.ArrayList;
import java.util.Collection;

import javax.persistence.TableGenerator;

import org.apache.cayenne.util.XMLEncoder;
import org.apache.cayenne.util.XMLSerializable;

/**
 * A primary key generator based on a database table.
 * 
 */
public class JpaTableGenerator implements XMLSerializable {

    protected String name;
    protected String table;
    protected String catalog;
    protected String schema;
    protected String pkColumnName;
    protected String valueColumnName;
    protected String pkColumnValue;
    protected int initialValue = 0;
    protected int allocationSize = 50;

    protected Collection<JpaUniqueConstraint> uniqueConstraints;

    public JpaTableGenerator() {

    }

    public JpaTableGenerator(TableGenerator annotation) {
        name = annotation.name();
        table = annotation.table();
        catalog = annotation.catalog();
        schema = annotation.schema();
        pkColumnName = annotation.pkColumnName();
        valueColumnName = annotation.valueColumnName();
        pkColumnValue = annotation.pkColumnValue();
        initialValue = annotation.initialValue();
        allocationSize = annotation.allocationSize();

        getUniqueConstraints();
        for (int i = 0; i < annotation.uniqueConstraints().length; i++) {
            uniqueConstraints.add(new JpaUniqueConstraint(
                    annotation.uniqueConstraints()[i]));
        }
    }
    
    public void encodeAsXML(XMLEncoder encoder) {
    }

    public int getAllocationSize() {
        return allocationSize;
    }

    public void setAllocationSize(int allocationSize) {
        this.allocationSize = allocationSize;
    }

    public String getCatalog() {
        return catalog;
    }

    public void setCatalog(String catalog) {
        this.catalog = catalog;
    }

    public int getInitialValue() {
        return initialValue;
    }

    public void setInitialValue(int initialValue) {
        this.initialValue = initialValue;
    }

    /**
     * Returns table generator name.
     * <h3>Specification Documenatation</h3>
     * <p>
     * <b>Description:</b> A unique generator name that can be referenced by one or more
     * classes to be the generator for id values.
     * </p>
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPkColumnName() {
        return pkColumnName;
    }

    public void setPkColumnName(String pkColumnName) {
        this.pkColumnName = pkColumnName;
    }

    public String getPkColumnValue() {
        return pkColumnValue;
    }

    public void setPkColumnValue(String pkColumnValue) {
        this.pkColumnValue = pkColumnValue;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    /**
     * Returns table generator table name.
     * <h3>Specification Documentation</h3>
     * <p>
     * <b>Description:</b> Name of table that stores the generated id value.
     * </p>
     * <p>
     * <b>Default:</b> Name is chosen by persistence provider.
     * </p>
     */
    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public String getValueColumnName() {
        return valueColumnName;
    }

    public void setValueColumnName(String valueColumnName) {
        this.valueColumnName = valueColumnName;
    }

    public Collection<JpaUniqueConstraint> getUniqueConstraints() {
        if (uniqueConstraints == null) {
            uniqueConstraints = new ArrayList<JpaUniqueConstraint>(2);
        }

        return uniqueConstraints;
    }
}
