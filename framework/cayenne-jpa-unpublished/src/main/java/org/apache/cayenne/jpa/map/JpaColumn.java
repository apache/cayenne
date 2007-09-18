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

import javax.persistence.Column;

import org.apache.cayenne.util.XMLEncoder;
import org.apache.cayenne.util.XMLSerializable;

public class JpaColumn implements XMLSerializable {

    public static final int DEFAULT_LENGTH = 255;

    protected String name;
    protected boolean unique;
    protected boolean nullable;
    protected boolean insertable;
    protected boolean updatable;
    protected String columnDefinition;
    protected String table;
    protected int length;
    protected int precision;
    protected int scale;

    public JpaColumn() {

    }

    public JpaColumn(Column annotation) {
        if (!"".equals(annotation.name())) {
            name = annotation.name();
        }

        unique = annotation.unique();
        nullable = annotation.nullable();
        insertable = annotation.insertable();
        updatable = annotation.updatable();

        if (!"".equals(annotation.columnDefinition())) {
            columnDefinition = annotation.columnDefinition();
        }

        if (!"".equals(annotation.table())) {
            table = annotation.table();
        }

        length = annotation.length();
        precision = annotation.precision();
        scale = annotation.scale();
    }

    public void encodeAsXML(XMLEncoder encoder) {
        encoder.print("<column");
        if (name != null) {
            encoder.print(" name=\"" + name + "\"");
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

        if (length > 0 && length != DEFAULT_LENGTH) {
            encoder.print(" length=\"" + length + "\"");
        }

        if (precision > 0) {
            encoder.print(" precision=\"" + precision + "\"");
        }

        if (scale > 0) {
            encoder.print(" scale=\"" + scale + "\"");
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

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
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

    public int getPrecision() {
        return precision;
    }

    public void setPrecision(int precision) {
        this.precision = precision;
    }

    public int getScale() {
        return scale;
    }

    public void setScale(int scale) {
        this.scale = scale;
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

    @Override
    public String toString() {
        String className = getClass().getName();
        return className.substring(className.lastIndexOf('.') + 1) + ":" + name;
    }
}
