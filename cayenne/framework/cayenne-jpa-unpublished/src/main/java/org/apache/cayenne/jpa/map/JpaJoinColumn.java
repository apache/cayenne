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

import javax.persistence.JoinColumn;

import org.apache.cayenne.util.XMLEncoder;
import org.apache.cayenne.util.XMLSerializable;

/**
 * Join column specifies a mapped column for joining an entity association, aka flattened
 * attribute.
 * 
 */
public class JpaJoinColumn implements XMLSerializable {

    protected String name;
    protected String referencedColumnName;
    protected boolean unique;
    protected boolean nullable;
    protected boolean insertable;
    protected boolean updatable;
    protected String columnDefinition;
    protected String table;

    public JpaJoinColumn() {

    }

    public JpaJoinColumn(JoinColumn annotation) {
        if (!"".equals(annotation.name())) {
            name = annotation.name();
        }

        if (!"".equals(annotation.referencedColumnName())) {
            referencedColumnName = annotation.referencedColumnName();
        }

        if (!"".equals(annotation.columnDefinition())) {
            columnDefinition = annotation.columnDefinition();
        }

        if (!"".equals(annotation.table())) {
            table = annotation.table();
        }

        unique = annotation.unique();
        nullable = annotation.nullable();
        insertable = annotation.insertable();
        updatable = annotation.updatable();
    }

    public void encodeAsXML(XMLEncoder encoder) {
        encoder.print("<join-column");
        if (name != null) {
            encoder.print(" name=\"" + name + "\"");
        }

        if (referencedColumnName != null) {
            encoder.print(" referenced-column-name=\"" + referencedColumnName + "\"");
        }
        
        if (unique) {
            encoder.print(" unique=\"true\"");
        }

        if (!nullable) {
            encoder.print(" nullable=\"false\"");
        }

        if (!insertable) {
            encoder.print(" insertable=\"false\"");
        }

        if (!updatable) {
            encoder.print(" updatable=\"false\"");
        }

        if (columnDefinition != null) {
            encoder.print(" column-definition=\"" + columnDefinition + "\"");
        }

        if (table != null) {
            encoder.print(" table=\"" + table + "\"");
        }

        encoder.println("/>");
    }

    public String getColumnDefinition() {
        return columnDefinition;
    }

    public void setColumnDefinition(String columnDefinition) {
        this.columnDefinition = columnDefinition;
    }

    public boolean isInsertable() {
        return insertable;
    }

    public void setInsertable(boolean insertable) {
        this.insertable = insertable;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isNullable() {
        return nullable;
    }

    public void setNullable(boolean nullable) {
        this.nullable = nullable;
    }

    public String getReferencedColumnName() {
        return referencedColumnName;
    }

    public void setReferencedColumnName(String referencedColumnName) {
        this.referencedColumnName = referencedColumnName;
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public boolean isUnique() {
        return unique;
    }

    public void setUnique(boolean unique) {
        this.unique = unique;
    }

    public boolean isUpdatable() {
        return updatable;
    }

    public void setUpdatable(boolean updateable) {
        this.updatable = updateable;
    }
}
