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

import javax.persistence.PrimaryKeyJoinColumn;

import org.apache.cayenne.util.XMLEncoder;
import org.apache.cayenne.util.XMLSerializable;

public class JpaPrimaryKeyJoinColumn implements XMLSerializable {

    protected String name;
    protected String referencedColumnName;
    protected String columnDefinition;

    public JpaPrimaryKeyJoinColumn() {

    }

    public JpaPrimaryKeyJoinColumn(PrimaryKeyJoinColumn annotation) {

        if (!"".equals(annotation.name())) {
            name = annotation.name();
        }

        if (!"".equals(annotation.columnDefinition())) {
            columnDefinition = annotation.columnDefinition();
        }

        if (!"".equals(annotation.referencedColumnName())) {
            referencedColumnName = annotation.referencedColumnName();
        }
    }

    public void encodeAsXML(XMLEncoder encoder) {
    }

    /**
     * Returns columnDefinition property value.
     * <h3>Specification Docs</h3>
     * <p>
     * <b>Description:</b> (Optional) A SQL fragment that is used when generating DDL for
     * the column.
     * </p>
     * <p>
     * <b>Default:</b> generated SQL to create column of the inferred type.
     * </p>
     */
    public String getColumnDefinition() {
        return columnDefinition;
    }

    public void setColumnDefinition(String columnDefinition) {
        this.columnDefinition = columnDefinition;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getReferencedColumnName() {
        return referencedColumnName;
    }

    public void setReferencedColumnName(String referencedColumnName) {
        this.referencedColumnName = referencedColumnName;
    }
}
